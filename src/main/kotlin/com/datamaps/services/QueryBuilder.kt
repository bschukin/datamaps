package com.datamaps.services

import com.datamaps.general.NIY
import com.datamaps.general.SNF
import com.datamaps.general.throwNIS
import com.datamaps.mappings.*
import com.datamaps.maps.DataMap
import com.datamaps.maps.addIfNotInSilent
import org.apache.commons.lang.text.StrSubstitutor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Service
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 */

@Service
class QueryBuilder {
    @Resource
    lateinit var dataMappingsService: DataMappingsService

    @Resource
    lateinit var filterBuilder: QueryFilterBuilder

    @Resource
    lateinit var dbDialect: DbDialect

    @Resource
    lateinit var conversionService: ConversionService



    @Autowired
    lateinit var dmUtilService: DmUtilService

    fun createQueryByEntityNameAndId(name: String, id: Long): SqlQueryContext {

        val dp = DataProjection(name, id).scalars().withRefs()
        return createQueryByDataProjection(dp)
    }

    fun createUpgradeQueryByMapsAndSlices(maps: List<DataMap>, slice: projection): SqlQueryContext {

        slice.entity = maps[0].entity

        //todo: сейчас есть лишний джойн на родительскую сущность
        //на самом деле от него можно избавиться (если в слайсе только одна ссылка)
        //slice {collcetion1,
        // collection2} - в этом случае не избавиться от джойна на родительскую суть
        //если же slice {collcetion1} то джойн на родителя не нужен
        slice.onlyId()
                .filter(f("id") IN maps.map { dm -> dm.id })

        return createQueryByDataProjection(slice)
    }

    fun createQueryByDataProjection(dp: DataProjection): SqlQueryContext {

        val qr = QueryBuildContext()


        buildMainQueryStructure(qr, dp, null)

        buildWhere(qr)

        addOffsetLimit(qr, dp)

        buildOrders(qr)

        return builqSqlQuery(qr, dp)
    }


    fun buildMainQueryStructure(qr: QueryBuildContext, parentProjection: DataProjection, field: String? = null): QueryLevel {

        val isRoot = field == null

        //если мы на руте - берем рутовый маппинг
        val dm = if (isRoot)
            dataMappingsService.getDataMapping(parentProjection.entity!!)
        else
            dataMappingsService.getRefDataMapping(qr.stack.peek().dm, field!!)

        //если мы на руте - берем рутовую проекцию, иначе берем проекцию с поля
        val currProjection = if (isRoot) parentProjection else
            parentProjection.fields.getOrDefault(field!!, DataProjection(dm.name, field = field))

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
            qr.stack.peek().childProps[field] = ql

        //ддля рута - формируем клауз FROM
        if (isRoot)
            qr.from = dm.table + " as " + alias
        //для ссылки - формируем JOIN
        else
            qr.addJoin(buildJoin(qr, qr.stack.peek(), ql))

        //запоминаем в контексте
        qr.stack.push(ql)


        //получаем список всех полей которые мы будем селектить
        //все поля =  поля всех указанных групп (groups) U поля указанные в списке fields
        val allFields = getAllFieldsOnLevel(currProjection, dm)

        //бежим по всем полям и решаем что с кажным из них делать
        allFields.forEach { f ->
            run {
                val entityField = dm[f]
                when {
                    entityField.sqlcolumn == dm.idColumn -> buildIDfield(qr, dm, alias, entityField)
                    entityField.isSimple -> buildSimpleField(qr, dm, alias, entityField)
                    entityField.isM1 -> buildManyToOneField(qr, currProjection, entityField)
                    entityField.is1N -> buildMainQueryStructure(qr, currProjection, entityField.name)
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


        return qr.stack.pop()
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

        filterBuilder.buildWhere(qr)
    }

    private fun buildOrders(qr: QueryBuildContext) {

        filterBuilder.buildOrders(qr)
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
                        currentmapa[entityField.name, true] = parentmapa
                        currentmapa.addBackRef(entityField.name)
                    }
                }

            }
            )

            return true
        }
        return false
    }


    private fun buildJoin(qr: QueryBuildContext, parent: QueryLevel, me: QueryLevel): String {
        val ref = parent.dm[me.parentLinkField!!]

        return when {
            ref.isM1 -> "\r\nLEFT JOIN ${me.dm.table} as ${me.alias} ON " +
                    "${parent.alias}.${ref.sqlcolumn}=${me.alias}.${ref.manyToOne!!.joinColumn}"

            ref.is1N -> "\r\nLEFT JOIN ${me.dm.table} as ${me.alias} ON " +
                    "${parent.alias}.${parent.dm.idColumn}=${me.alias}.${ref.oneToMany!!.theirJoinColumn}"

            else -> throwNIS()
        }

    }


    private fun builqSqlQuery(qr: QueryBuildContext, dp: DataProjection): SqlQueryContext {

        val sql = "SELECT \n\t" +
                dbDialect.getLimitOffsetQueryInSelect(qr.limit, qr.offset) +
                qr.getSelectString() + "\n" +
                "FROM " + qr.from +
                qr.getJoinString() +
                (if (qr.where.isBlank()) " " else " \nWHERE " + qr.where) +
                (if (qr.orderBy.isBlank()) " " else " \nORDER BY " + qr.orderBy) +
                dbDialect.getLimitOffsetQueryInWhere(qr.limit, qr.offset)


        return SqlQueryContext(sql, qr.params, qr)
    }

    private fun buildIDfield(qr: QueryBuildContext, dm: DataMapping, entityAlias: String, entityField: DataField) {
        val columnAlias = qr.addSelect(entityAlias, entityField.sqlcolumn)
        val ql = qr.stack.peek()
        qr.addMapper(columnAlias, { mc, rs ->
            val id = conversionService.convert(rs.getObject(columnAlias), Long::class.java)
            when (id) {
                null ->
                    mc.curr(ql.parent!!.alias)?.let {
                        mc.curr(ql.parent.alias)!!.nullf(ql.parentLinkField!!)
                    }
                else -> {
                    val datamap = mc.create(entityAlias, dm.name, id)

                    ql.parentLinkField?.let {
                        val parentField = ql.parent!!.dm[ql.parentLinkField]
                        when {
                            parentField.isM1 -> mc.curr(ql.parent.alias)!![ql.parentLinkField, true] = datamap
                            parentField.is1N -> {
                                mc.curr(ql.parent.alias)!!.list(ql.parentLinkField)
                                        .addIfNotInSilent(datamap)
                                dmUtilService.updateBackRef(mc.curr(ql.parent.alias)!!, datamap, ql.parentLinkField, true)

                            }
                            else -> throwNIS()
                        }
                    }

                }
            }
        })
    }

    private fun buildSimpleField(qr: QueryBuildContext, dm: DataMapping, entityAlias: String, entityField: DataField) {
        val columnAlias = qr.addSelect(entityAlias, entityField.sqlcolumn)

        //добавляем простой маппер
        qr.addMapper(columnAlias, { mc, rs ->
            if (mc.curr(entityAlias) != null)
                mc.curr(entityAlias)!![entityField.name, true] = rs.getObject(columnAlias)
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

        //для каждого замапленного латералем маппинга (алиас в латерале ->алиас в маппинге) надо сделать маппинг
        lateral.mappings.forEach { k, v ->
            run {
                //добавляем алиас колонки в селект
                val colAlias = qr.addSelect(lateral.table, k)
                //добавляем простой маппер
                qr.addMapper(colAlias, { mc, rs ->
                    if (mc.curr(ql.alias) != null)
                        mc.curr(ql.alias)!![v, true] = rs.getObject(colAlias)
                })
            }
        }
    }


    //получаем список всех полей которые мы будем селектить
    //все поля =
    //      поля дефлотной группы
    //  U   поля всех указанных групп (groups)
    //  U   поля указанные в списке fields
    private fun getAllFieldsOnLevel(dp: DataProjection, dm: DataMapping): Set<String> {
        val allFields = mutableSetOf<String>()
        allFields.add(dm.idColumn!!.toLowerCase())
        // поля дефлотной группы
        if (dp.fields.size == 0)
            allFields.addAll(dm.defaultGroup.fields.map { f -> f.toLowerCase() })

        //поля всех указанных групп (groups)
        dp.groups.forEach { gr ->
            run {
                val datagroup = dm.groups.computeIfAbsent(gr,
                        { t -> throw SNF("group ${gr} of ${dp.entity} entity not found") })
                allFields.addAll(datagroup.fields.map { f -> f.toLowerCase() })
            }
        }
        //поля, указанные как поля
        dp.fields.forEach { f -> allFields.add(f.key.toLowerCase()) }

        return allFields
    }


    fun applyColumnNamesInFormula(formula: String, qr: QueryBuildContext, ql: QueryLevel): String {
        val resolver = QueryVariablesResolver(qr, ql)
        val s = StrSubstitutor(resolver, "{{", "}}", '/')
        return s.replace(formula)
    }
}