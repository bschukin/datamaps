package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.utils.BLOB
import com.bftcom.ice.datamaps.utils.NIY
import com.bftcom.ice.datamaps.utils.throwImpossible
import com.bftcom.ice.datamaps.common.maps.*
import com.bftcom.ice.datamaps.DataMapF.Companion.BACKREFID
import com.bftcom.ice.datamaps.impl.dialects.DbDialect
import com.bftcom.ice.datamaps.utils.Date
import com.bftcom.ice.datamaps.impl.mappings.DataField
import com.bftcom.ice.datamaps.impl.mappings.DataMapping
import com.bftcom.ice.datamaps.impl.mappings.DataMappingsService
import com.bftcom.ice.datamaps.impl.util.DataMapsUtilService
import com.bftcom.ice.datamaps.impl.util.JsonFieldDataMapsBuilder
import com.bftcom.ice.datamaps.impl.util.getJavaTypeByJDBCType
import org.apache.commons.text.StrSubstitutor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.*
import javax.annotation.Resource
import kotlin.reflect.KClass
import kotlin.streams.toList

/**
 * Created by Щукин on 03.11.2017.
 */

@Service
open class QueryBuilder {

    @Autowired
    private lateinit var dataMappingsService: DataMappingsService

    @Autowired
    private lateinit var queryFilterBuilder: QueryFilterBuilder

    @Autowired
    private lateinit var queryTextFilterBuilder: QueryTextFilterBuilder

    @Autowired
    internal lateinit var dbDialect: DbDialect

    @Autowired
    private lateinit var dataMapsUtilService: DataMapsUtilService

    @Autowired
    private lateinit var conversionService: ConversionService

    @Resource
    internal open lateinit var jsonFieldDataMapsBuilder: JsonFieldDataMapsBuilder

    fun createQueryByEntityNameAndId(name: String, id: Any?): SqlQueryContext {

        val dp = Projection(name, id).scalars().withRefs()
        return createQueryByDataProjection(dp)
    }

    fun createUpgradeQueryByMapsAndSlices(maps: List<DataMap>, slice: DataProjection): SqlQueryContext {
        var proj = slice
        var mapper: PostMapper? = null
        if (slice.fields.size == 1 && slice.onDate == null && fieldIsListInCaseOfUpgrade(slice)) {
            //проблема лишнего джойна на родительскую сущность.
            //на самом деле от него можно избавиться (если в слайсе только одна ссылка)
            //slice {collcetion1,
            // collection2} - в этом случае не избавиться от джойна на родительскую суть
            //если же slice {collcetion1} то джойн на родителя не нужен
            val pair = createSelectForJoinedEntity(maps, proj, slice.fields.keys.first())
            proj = pair.first
            mapper = pair.second
        } else {
            slice.entity = maps[0].entity

            slice.onlyId()
                    .filter(f("id") IN maps.map { dm -> dm.id })
        }

        val sqlquerycontext = createQueryByDataProjection(proj)

        if (mapper != null)
            sqlquerycontext.qr.postMappers.add(mapper)

        return sqlquerycontext
    }

    private fun fieldIsListInCaseOfUpgrade(slice: DataProjection): Boolean {

        if (slice.fieldSet == null || slice.fieldSet == UndefinedFieldSet)
            return true
        val field = slice.fieldSet?.getField(slice.fields.keys.first()) ?: return true
        return field.isList()
    }


    private fun createSelectForJoinedEntity(maps: List<DataMap>, original: DataProjection, field: String): Pair<DataProjection, PostMapper> {
        val parentMapping = dataMappingsService.getDataMapping(maps[0].entity)


        val mapping = dataMappingsService.getRefDataMapping(parentMapping, field)
        val backField = mapping.getBackReferenceFieldForThisList(parentMapping, field)


        val sl = original[field]!!
        sl.entity = mapping.name
        sl.formula(BACKREFID, "{{${backField.name}}}")
        sl.filter { f(backField.name) IN maps }


        val postMapper: PostMapper = { list, _ ->
            val map = mutableMapOf<Any, DataMap>()
            list.forEach { dataMap ->
                dataMap[BACKREFID, true] = conversionService
                        .convert(dataMap[BACKREFID], parentMapping.idField()!!.kotlinType.java)
                val id = dataMap[BACKREFID]!!
                val dmNew = map.computeIfAbsent(id, { DataMap(parentMapping.name, id) })
                when {
                    parentMapping[field].isM1() -> dmNew[field, true] = dataMap
                    parentMapping[field].is1N() -> dmNew.list(field).addIfNotInSilent2(dataMap)
                }
            }
            map.values.stream().toList()
        }

        return Pair(sl, postMapper)
    }

    fun createQueryByDataProjection(dp: DataProjection, paramCounterStart: Int = 0): SqlQueryContext {

        enhanceProjectionIfShould(dp)

        val qr = buildCommonQueryStructures(paramCounterStart, dp)

        buildGroupBy(qr)

        buildOrders(qr)

        addOffsetLimit(qr, dp)

        return buildSqlQuery(qr)
    }

    fun createDeleteQueryByDataProjection(dp: DataProjection): SqlQueryContext {

        val qr = buildCommonQueryStructures(0, dp)

        return buildDeleteSqlQuery(qr)
    }

    private fun buildCommonQueryStructures(paramCounterStart: Int, dp: DataProjection): QueryBuildContext {
        val qr = QueryBuildContext(this, paramCounterStart)

        buildMainQueryStructure(qr, dp, null)

        buildWhere(qr)

        return qr
    }


    fun createCountQuery(dp: DataProjection): SqlQueryContext {
        val countProjection = Projection(dp.entity, "id").alias("e")

        countProjection.oql = dp.oql
        countProjection.textFilter = dp.textFilter
        countProjection.textFilterString = dp.textFilterString
        countProjection.params = dp.params
        countProjection.filter = dp.filter

        val query = createQueryByDataProjection(countProjection)

        val idColumnAlias = query.qr.getColumnAlias("e", "id")

        val countQuerySql = "SELECT count(DISTINCT $idColumnAlias) FROM (${query.sql}) t"

        return SqlQueryContext(countQuerySql, query.params, query.qr)
    }

    open fun buildMainQueryStructure(qr: QueryBuildContext, parentProjection: DataProjection, field: String? = null): QueryLevel {

        val isRoot = field == null

        //если мы на руте - берем рутовый маппинг
        val dm = if (isRoot)
            dataMappingsService.getDataMapping(parentProjection.entity!!)
        else
            dataMappingsService.getRefDataMapping(qr.stack.peek().dm, field!!)

        //если мы на руте - берем рутовую проекцию, иначе берем проекцию с поля
        val currProjection = if (isRoot) parentProjection else
            parentProjection.fields.getOrDefault(field!!, Projection(dm.name, field = field))

        currProjection.setFieldSetIfShould(FieldSetRepo.fieldSetOrNull(dm.name))


        //генерим алиас
        val alias = currProjection.queryAlias ?: qr.createTableAlias(dm.table)

        //запомним рутовый алиас
        if (isRoot)
            qr.rootAlias = alias

        //запомним в таблицу алиасов - родительский путь к нам
        if (!isRoot)
            qr.addParentPathAlias(qr.stack.peek().alias, field, alias)

        val ql = QueryLevel(dm, currProjection, alias, field, if (isRoot) null else qr.stack.peek(), dbDialect)

        if (isRoot)
            qr.root = ql
        //зарегистрируем алиас
        qr.addTableAlias(alias, ql)


        //строим дерево
        if (!isRoot)
            qr.stack.peek().childProps[field!!] = ql

        //ддля рута - формируем клауз FROM
        if (isRoot)
            qr.from = dm.table + " " + alias

        //для ссылки - формируем JOIN
        val join = if (isRoot) null else
            qr.addJoin(buildJoin(qr.stack.peek(), ql), currProjection.joinFilter)

        //запоминаем в контексте
        qr.stack.push(ql)


        //получаем список всех полей которые мы будем селектить
        //все поля =  поля всех указанных групп (groups) U поля указанные в списке fields
        val allFields = getAllFieldsOnLevel(currProjection, dm, qr.isGroupByQuery())

        //бежим по всем полям и решаем что с кажным из них делать
        allFields.forEach { f ->
            run {
                val entityField = dm[f]
                when {
                    dm.idColumn.equals(entityField.sqlcolumn, true) -> buildIDfield(qr, dm, alias, entityField)

                    entityField.isScalarOqlFormula() -> buildFormula(qr, ql, entityField.name, entityField.oqlFormula!!.oql)

                    entityField.isReferenceOqlFormula() -> {
                        val lateralTable = entityField.oqlFormula!!.lateralTable!!
                        buildLateral(qr, ql,
                                Lateral(lateralTable,
                                        ("(${entityField.oqlFormula.oql}) $lateralTable on true"),
                                        entityField.name,
                                        entityField.oqlFormula.isList)
                        )
                    }

                    entityField.isJson() -> buildJsonField(qr, currProjection[entityField.name], alias, entityField)

                    entityField.isSimple() -> buildSimpleField(qr, alias, entityField)

                    entityField.isM1() -> {
                        analyzeAndBuildManyToOneField(qr, currProjection, entityField, dm, alias)
                    }
                    entityField.is1N() -> {
                        analyzeAndBuildOneToManyField(currProjection, entityField, qr, dm)
                    }

                    else -> throw NIY()
                }
            }
        }

        //а теперь бежим по формулам
        currProjection.formulas.forEach { (formulaName, formulaBody) ->
            buildFormula(qr, ql, formulaName, formulaBody)
        }

        //а теперь бежим по латералям
        currProjection.laterals.forEach { l ->
            buildLateral(qr, ql, l)
        }

        //а теперь разберемся с темпоральностью
        findTargetDate(qr.stack)?.let {
            addTemporalRangeInFilter(qr, ql, it, join)
        }


        return qr.stack.pop()
    }

    private fun analyzeAndBuildOneToManyField(currProjection: DataProjection, entityField: DataField, qr: QueryBuildContext, dm: DataMapping): Any {
        val proj = currProjection.fields[entityField.name]
        return when {
            proj != null && proj.options?.retrieveType == RetrieveType.Recursive ->
                createPostSelectChildsTreeQueryAction(qr, proj, FieldSetRepo.fieldSet(dm.name))

            proj?.options == null || proj.options?.collectionJoinType == JoinType.JOIN ->
                buildMainQueryStructure(qr, currProjection, entityField.name)

            proj.options?.collectionJoinType == JoinType.SELECT ->
                createPostSelectUpgradeAction(qr, on("").apply { fields[entityField.name] = proj})

            else -> throwImpossible()
        }
    }

    private fun analyzeAndBuildManyToOneField(qr: QueryBuildContext,
                                              currProjection: DataProjection, entityField: DataField, dm: DataMapping, alias: String) {
        val proj = currProjection[entityField.name]
        when {

            proj?.options?.retrieveType == null
                    || proj.options?.retrieveType == RetrieveType.Usual -> {
                buildManyToOneField(qr, currProjection, entityField)
            }

            proj.options?.retrieveType == RetrieveType.JustId -> {
                val dm2 = dataMappingsService.getRefDataMapping(dm, entityField.name)
                buildIDfieldOnRef(qr, dm2, alias, entityField,
                        getJavaTypeByJDBCType(entityField.jdbcType))
            }

            proj.options?.retrieveType == RetrieveType.Recursive -> {
                createPostSelectParentsTreeQueryAction(qr, proj, FieldSetRepo.fieldSet(dm.name))
            }

            else -> TODO()
        }
    }

    val paramDateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
    private fun addTemporalRangeInFilter(qr: QueryBuildContext, ql: QueryLevel,
                                         date: Date, join: Join?) {

        //проверяем являет ли "ветка" объектного дерева темпоральной
        val tk = ql.dp.fieldSet?.getOption(Temporal::class) ?: return

        //строим полные пути до startDate / endDate в объектном дереве
        val longField = buildLongFieldName(qr.stack)
        val startField = if (longField == "") tk.startDateField else "${longField}.${tk.startDateField}"
        val endField = if (longField == "") tk.endDateField else "${longField}.${tk.endDateField}"

        val paramName = "date${paramDateFormat.format(date.date)}"

        val f = ({ f(startField) le param(paramName) } /*or { f(stlong) IS NULL }*/) and
                ({ param(paramName) lt f(endField) }/* or { f(endlong) IS NULL }*/)

        //подставляем фильтр по датам в общий фильтр проекци (если мы в корне)
        //или к джойну - во всех остальных случаях
        if (join != null)
            join.addJoinFilter(f)
        else
            qr.root.dp.filter(f)

        //проставляем параметр
        qr.root.dp.param(paramName, date)
    }

    private fun buildLongFieldName(stack: Stack<QueryLevel>): String {
        return stack.map { it.parentLinkField }.filterNotNull()
                .joinToString(".")
    }

    private fun findTargetDate(stack: Stack<QueryLevel>): Date? {
        val ql = stack.findLast { it.dp.onDate != null } ?: return null
        return ql.dp.onDate
    }

    private fun createPostSelectUpgradeAction(qr: QueryBuildContext, currProjection: DataProjection) {

        //надо создать такую функцию, которая после того как отработает основной запрос
        //сделает апгрейд на данную коллекцию

        //на вход нам придет список
        //надо собрать из него мапы-родители (помним, что уровень вложенности может быть любым)
        //и на них сделать апгрейд
        val stackOfProps = qr.stack.map { ql -> ql.parentLinkField }.toMutableList()
        //удаляем первый и последний элементы
        stackOfProps.removeAt(0)
        val mapper: PostMapper = { list, dataService ->

            var parents = list
            stackOfProps.forEach {
                parents = parents.flatMap { el -> el.list(it!!) }
            }

            dataService.upgrade(parents, currProjection)

            list
        }

        qr.postMappers.add(mapper)
    }


    private fun createPostSelectChildsTreeQueryAction(qr: QueryBuildContext, currProjection: DataProjection, fieldSet: FieldSet?) {

        //функция, которая после того как отработает основной запрос
        //догружает детей иерархическим запросом

        //на вход нам придет список
        //надо собрать из него мапы-родители (помним, что уровень вложенности может быть любым)
        //и на них сделать рекурсивные запросы
        val stackOfProps = qr.stack.map { ql -> ql.parentLinkField }.toMutableList()
        //удаляем первый и последний элементы
        stackOfProps.removeAt(0)
        val mapper: PostMapper = { list, dataService ->

            var parents = list

            stackOfProps.forEach {
                parents = parents.flatMap { el -> el.list(it!!) }
            }
            val parents2 = parents as List<DataMapF<UndefinedFieldSet>>
            val tree = fieldSet?.getOption(Tree::class)
            val opts = TreeQueryOptions(
                    parentProperty = tree?.parentField, childProperty = tree?.childsField,
                    depth = currProjection.option().recursiveDepth, mergeIntoSourceList = true, buildHierarchy = true)
            currProjection.entity = fieldSet?.entity
            dataService.loadChilds(parents2, currProjection, opts)

            list
        }

        qr.postMappers.add(mapper)
    }

    private fun createPostSelectParentsTreeQueryAction(qr: QueryBuildContext,
                                                       currProjection: DataProjection, fieldSet: FieldSet?) {

        //функция, которая после того как отработает основной запрос
        //догружает детей иерархическим запросом

        //на вход нам придет список
        //надо собрать из него мапы-родители (помним, что уровень вложенности может быть любым)
        //и на них сделать рекурсивные запросы
        val stackOfProps = qr.stack.map { ql -> ql.parentLinkField }.toMutableList()
        //удаляем первый и последний элементы
        stackOfProps.removeAt(0)
        val mapper: PostMapper = { list, dataService ->

            var parents = list

            stackOfProps.forEach {
                parents = parents.flatMap { el -> el.list(it!!) }
            }
            val parents2 = parents as List<DataMapF<UndefinedFieldSet>>
            val tree = fieldSet?.getOption(Tree::class)
            val opts = TreeQueryOptions(
                    parentProperty = tree?.parentField,
                    depth = currProjection.option().recursiveDepth, mergeIntoSourceList = true,
                    buildHierarchy = true, includeLevel0 = true)
            currProjection.entity = fieldSet?.entity
            dataService.loadParents(parents2, currProjection, opts)

            list
        }

        qr.postMappers.add(mapper)
    }


    fun lateCreateAlias(qr: QueryBuildContext, parentLevel: QueryLevel, field: String): QueryLevel {


        //если мы на руте - берем рутовый маппинг
        val dm = dataMappingsService.getRefDataMapping(parentLevel.dm, field)

        //если мы на руте - берем рутовую проекцию, иначе берем проекцию с поля
        val currProjection = parentLevel.dp.fields.getOrDefault(field, Projection(dm.name, field = field))

        //генерим алиас
        val alias = currProjection.queryAlias ?: qr.createTableAlias(dm.table)

        //запомним в таблицу алиасов - родительский путь к нам
        qr.addParentPathAlias(parentLevel.alias, field, alias)

        val ql = QueryLevel(dm, currProjection, alias, field, parentLevel, dbDialect)

        //зарегистрируем алиас
        qr.addTableAlias(alias, ql)

        //строим дерево
        parentLevel.childProps[field] = ql

        qr.addJoin(buildJoin(parentLevel, ql))


        return ql
    }


    private fun addOffsetLimit(qr: QueryBuildContext, dp: DataProjection) {

        if (dp.limit == null) return

        dp.offset.let {
            qr.offset = dp.offset
        }
        dp.limit.let {
            qr.limit = dp.limit
        }
    }

    private fun buildWhere(qr: QueryBuildContext) {

        queryFilterBuilder.buildWhere(qr)
        queryTextFilterBuilder.buildTextFilterWhere(qr)
    }

    private fun buildGroupBy(qr: QueryBuildContext) {
        queryFilterBuilder.buildGroupBy(qr)
    }

    private fun buildOrders(qr: QueryBuildContext) {
        queryFilterBuilder.buildOrders(qr)
    }


    private fun buildManyToOneField(qr: QueryBuildContext, projection: DataProjection, entityField: DataField) {
        //1е - сначала надо понять, не являемся ли мы обратнной ссылкой на уже доставаемый объект
        //в этом случае нам не надо строить всю проекцию для такой обратной ссылки, а просто смаппировать нужный
        //айдишник в поле данной проекции
        if (!isBackRef(qr, entityField))
            buildMainQueryStructure(qr, projection, entityField.name)
    }

    private fun isBackRef(qr: QueryBuildContext, entityField: DataField): Boolean {
        val currLevel = qr.stack.peek()
        if (currLevel.parent == null)
            return false
        val parent = currLevel.parent

        //нашли родительское поле

        val parentField = parent.dm[currLevel.parentLinkField!!]
        if (parentField.referenceTo() == currLevel.dm.name
                && parentField.referencedOneToAnother(entityField)) {
            //1. итак, это поле является обратной ссылкой на родителя (родитель->дочерняя коллекция->элемент->ссылка на родителя)
            //при маппировании необходимо в текущую сущность положить ссыль на родителя
            //смаппируем это на ID текущей сущноси
            val idAlias = qr.getColumnAlias(currLevel.alias, currLevel.dm.idColumn)
            qr.addMapper(idAlias, { mc, _ ->
                val parentmapa = mc.curr[parent.alias]
                val currentmapa = mc.curr[currLevel.alias]
                parentmapa?.let {
                    currentmapa?.let {
                        currentmapa[entityField.name, true] = parentmapa//todo: 10may2018 - experimental .toHeader()
                    }
                }

            }
            )

            return true
        }
        return false
    }


    private fun buildJoin(parent: QueryLevel, me: QueryLevel): String {
        val ref = parent.dm[me.parentLinkField!!]

        return when {
            ref.isReference() ->
                "\r\nLEFT JOIN ${me.dm.table} ${me.alias} ON " +
                        "${parent.alias}.${me.getEscapedColumn(ref.reference().thisSideJoinColumn)} = " +
                        "${me.alias}.${me.getEscapedColumn(ref.reference().thatSideJoinColumn)}"

            else -> throwImpossible()
        }

    }


    private fun buildSqlQuery(qr: QueryBuildContext): SqlQueryContext {

        val sql = StringBuilder("SELECT \n\t")
                .append(getLimitOffsetInSelect(qr))
                .append(qr.getSelectString()).append("\n")
                .append("FROM ${qr.from}")
                .append(qr.getJoinString())
                .append(if (qr.where.isBlank()) " " else " \nWHERE " + qr.where)
                .append(if (qr.groupBy.isBlank()) " " else " \nGROUP BY " + qr.groupBy)
                .append(if (qr.orderBy.isBlank()) " " else " \nORDER BY " + qr.orderBy)

        appendLimitOffset(sql, qr)

        return SqlQueryContext(sql.toString(), qr.params, qr)
    }

    private fun buildDeleteSqlQuery(qr: QueryBuildContext): SqlQueryContext {

        val sql = StringBuilder("DELETE\n\t")
                .append("FROM ${qr.from}")
                .append(qr.getJoinString())
                .append(if (qr.where.isBlank()) " " else " \nWHERE " + qr.where)

        return SqlQueryContext(sql.toString(), qr.params, qr)
    }

    internal fun appendLimitOffset(sql: java.lang.StringBuilder, qr: QueryBuildContext,
                                   params: MutableMap<String, Any?> = qr.params) {
        dbDialect.appendLimitOffset(sql, qr.limit, qr.offset, params)
    }

    internal fun getLimitOffsetInSelect(qr: QueryBuildContext, limit: Int? = qr.limit, offset: Int? = qr.offset): String {
        return dbDialect.getLimitOffsetQueryInSelect(limit, offset)
    }


    private fun buildIDfield(qr: QueryBuildContext, dm: DataMapping, entityAlias: String, entityField: DataField) {
        val columnAlias = qr.addSelect(entityAlias, entityField.sqlcolumn, null)
        val ql = qr.stack.peek()
        qr.addMapper(columnAlias, { mc, rs ->
            val id = conversionService.convert(rs.getObject(columnAlias), entityField.kotlinType.java)
            when (id) {
                null ->
                    mc.curr(ql.parent!!.alias)?.let {
                        mc.curr(ql.parent.alias)!!.nullf(ql.parentLinkField!!)
                    }
                else -> {
                    val datamap = mc.create(entityAlias, dm.name, id, rs)

                    ql.parentLinkField?.let {
                        val parentField = ql.parent!!.dm[ql.parentLinkField]
                        when {
                            parentField.isM1() -> mc.curr(ql.parent.alias)!![ql.parentLinkField, true] = datamap
                            parentField.is1N() -> {
                                mc.curr(ql.parent.alias)!!.list(ql.parentLinkField)
                                        .addIfNotInSilent2(datamap)
                                dataMapsUtilService.updateBackRef(mc.curr(ql.parent.alias)!!, datamap, ql.parentLinkField, true)

                            }
                            else -> throwImpossible()
                        }
                    }

                }
            }
        })
    }

    private fun buildIDfieldOnRef(qr: QueryBuildContext, dm: DataMapping, entityAlias: String,
                                  entityField: DataField, ktype: KClass<*>) {
        val columnAlias = qr.addSelect(entityAlias, entityField.sqlcolumn, null)
        val ql = qr.stack.peek()
        qr.addMapper(columnAlias, { mc, rs ->
            val id = conversionService.convert(rs.getObject(columnAlias), ktype.java)
            when (id) {
                null ->
                    mc.curr(entityAlias)?.let {
                        it.nullf(entityField.name)
                    }
                else -> {
                    val datamap = DataMap.existing(dm.name, id)
                    mc.curr(entityAlias)!![entityField.name, true] = datamap
                }
            }
        })
    }

    private fun buildSimpleField(qr: QueryBuildContext, entityAlias: String, entityField: DataField) {
        val columnAlias = qr.addSelect(entityAlias, entityField.sqlcolumn, entityField.name)

        //добавляем простой маппер
        qr.addMapper(columnAlias, { mc, rs ->
            if (mc.curr(entityAlias) != null)
                mc.curr(entityAlias)!![entityField.name, true] =
                        conversionService.convert(rs.getObject(columnAlias),
                                entityField.kotlinType.java)
        })
    }

    private fun buildJsonField(qr: QueryBuildContext, projection: DataProjection?, entityAlias: String, entityField: DataField) {
        val columnAlias = qr.addSelect(entityAlias, entityField.sqlcolumn, entityField.name)

        val effectiveProjection = if (qr.stack.peek().dp.groups.contains(BLOB)) null else projection
        //добавляем маппер json
        qr.addMapper(columnAlias, { mc, rs ->
            val currMap = mc.curr(entityAlias)
            if (currMap != null) {
                val json = conversionService.convert(rs.getObject(columnAlias), String::class.java)
                mc.curr(entityAlias)!![entityField.name, true] =
                        jsonFieldDataMapsBuilder.buildDataMapFromJson(currMap, entityField.name, json, effectiveProjection)
            }
        })

    }

    private fun buildFormula(qr: QueryBuildContext, ql: QueryLevel, formulaName: String, formula: String) {
        val formulaSelect = applyColumnNamesInFormula(formula, qr, ql)
        ql.dp.formula(formulaName, formulaSelect)
        qr.addSelectFromFormula(formulaName, formulaSelect)
        //добавляем простой маппер
        qr.addMapper(formulaName, { mc, rs ->
            if (mc.curr(ql.alias) != null)
                mc.curr(ql.alias)!![formulaName, true] = rs.getObject(formulaName)
        })
    }

    private fun buildLateral(qr: QueryBuildContext, ql: QueryLevel, lateral: Lateral) {

        //зарегистрируем алиас таблицы
        qr.addTableAlias(lateral.table, ql)

        var sql = applyColumnNamesInFormula(lateral.sql, qr, ql)
        sql = "\r\nLEFT JOIN LATERAL $sql "
        qr.addJoin(sql)

        if (lateral.mappings.isEmpty())
            tryParseMappingsFromSql(lateral.mappings, sql)


        //для каждого замапленного латералем маппинга (алиас в латерале ->алиас в маппинге) надо сделать маппинг
        var i = -1
        lateral.mappings.forEach { k, v ->
            run {
                i++

                //добавляем алиас колонки в селект
                val colAlias = qr.addSelect(lateral.table, k, v)
                val alias = if (lateral.embeddedProperty != null) lateral.table else ql.alias

                val j = i
                //добавляем простой маппер
                qr.addMapper(colAlias) { mc, rs ->
                    val flag = lateral.embeddedProperty != null && j == 0

                    //для embedded-латералов делаем вложенный объект
                    var idColumnAlreadyUsed = false
                    if (flag) {
                        val curr =  mc.curr(ql.alias)!!
                        val embeddedProperty = lateral.embeddedProperty!!
                        val id =
                                if (v.equals(DataMap.ID, true))
                                    rs.getObject(colAlias)
                                            .also { idColumnAlreadyUsed = true }
                                else DeltaStore.newGuid()

                        if (id != null) {
                            val dm = mc.create(alias, alias, id, rs)

                            if (lateral.isList) {
                                curr.list(embeddedProperty).addIfNotInSilent2(dm)
                            } else {
                                curr[embeddedProperty, true] = dm
                            }
                        } else if (lateral.isList) {
                            // Для вложенных списков при отсутствии записей создаём пустой список
                            curr.list(embeddedProperty)
                        }
                    }

                    if (mc.curr(alias) != null && !idColumnAlreadyUsed)
                        mc.curr(alias)!![v, true] = rs.getObject(colAlias)
                }
            }
        }
    }

    private fun tryParseMappingsFromSql(mappings: MutableMap<String, String>, sql: String) {
        val firstIndex = sql.indexOf("select", ignoreCase = true) + 6
        if (firstIndex > sql.length)
            return
        val lastIndex = sql.indexOf("from", ignoreCase = true)
        val fullSelectClause = sql.substring(firstIndex, lastIndex)

        val selectClauses = fullSelectClause.split(" as ", ignoreCase = true)
        if (selectClauses.size < 2)
            return

        selectClauses.subList(1, selectClauses.lastIndex + 1).forEach {
            val name = if (it.contains(",")) it.substringBefore(",").trim()
            else it.trim()
            mappings[name] = name
        }
        println(mappings)
    }


    //получаем список всех полей которые мы будем селектить
    //все поля =
    //      поля дефлотной группы
    //  U   поля всех указанных групп (groups)
    //  U   поля указанные в списке fields
    //в groupBy режиме ничего лишнего не добавляется
    protected open fun getAllFieldsOnLevel(dp: DataProjection, dm: DataMapping, groupByMode: Boolean): Set<String> {
        if (groupByMode) {
            return dp.groupByFields.map { it.toLowerCase() }.toSet() +
                    dp.fields.map { f -> f.key.toLowerCase() }

        } else {
            val allFields = mutableSetOf<String>()
            allFields.add(dm.idColumn!!.toLowerCase())
            // поля дефлотной группы
            if (dp.fields.isEmpty())
                allFields.addAll(dm.defaultGroup.fields.map { f -> f.toLowerCase() })

            //поля всех указанных групп (groups)
            dp.groups.forEach { gr ->
                run {
                    val datagroup = dm.groups[gr]
                    datagroup?.let {
                        allFields.addAll(it.fields.map { f -> f.toLowerCase() })
                    }
                }
            }
            //поля, указанные как поля
            dp.fields.forEach { f -> allFields.add(f.key.toLowerCase()) }

            return allFields
        }
    }


    private fun applyColumnNamesInFormula(formula: String, qr: QueryBuildContext, ql: QueryLevel): String {
        val resolver = QueryVariablesResolver(qr, ql)
        val s = StrSubstitutor(resolver, "{{", "}}", '/')
        return s.replace(formula)
    }

    //функция для действий над проекцией, зависящих от проекции
    private fun enhanceProjectionIfShould(dp: DataProjection) {

        //для проекций имеющих оффсет!=null необходимо ставить сортировку
        //если она не указана уже в проекции
        if(dp.offset!=null && dp._orders.isEmpty())
            dp.order(ExpressionField(DataMap.ID))

    }
}