package com.datamaps.services

import com.datamaps.general.NIY
import com.datamaps.general.SNF
import com.datamaps.general.throwNIS
import com.datamaps.mappings.DataField
import com.datamaps.mappings.DataMapping
import com.datamaps.mappings.DataMappingsService
import com.datamaps.mappings.DataProjection
import com.datamaps.util.DataConverter
import org.springframework.stereotype.Service
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 */

@Service
class QueryBuilder {
    @Resource
    lateinit var dataMappingsService: DataMappingsService;

    @Resource
    lateinit var dataConverter: DataConverter

    fun createQueryForEntity(qr: QueryBuildContext, parent: DataProjection, field: String) {

    }

    fun createQueryByEntityNameAndId(name: String, id: Long): SqlQueryContext {
        var dm = dataMappingsService.getDataMapping(name)
        //val query = Query()
        throw NIY()
    }

    fun createQueryByDataProjection(dp: DataProjection): SqlQueryContext {

        val qr = QueryBuildContext()

        buildDataProjection(qr, dp, null)

        return builqSqlQuery(qr, dp)
    }


    fun buildDataProjection(qr: QueryBuildContext, dp: DataProjection, field: String? = null) {

        val isRoot = field == null

        //если мы на руте - берем рутовый маппинг
        val dm = if (isRoot)
            dataMappingsService.getDataMapping(dp.entity!!)
        else
            dataMappingsService.getRefDataMapping(qr.stack.peek().dm, field!!)

        //если мы на руте - берем рутовую проекцию, иначе берем проекцию с поля
        val projection = if (isRoot) dp else
            dp.fields.getOrDefault(field!!, DataProjection(dm.name, field))

        //генерим алиас
        val alias = qr.getAlias(dm.table)

        //запомним рутовый алиас
        if (isRoot)
            qr.rootAlias = alias

        val ql = QueryLevel(dm, projection, alias, field, if (isRoot) null else qr.stack.peek())
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
        val allFields = getAllFieldsOnLevel(projection, dm)

        //бежим по всем полям и решаем что с кажным из них делать
        allFields.forEach { f ->
            run {
                val entityField = dm[f]
                when {
                    entityField.sqlcolumn == dm.idColumn -> buildIDfield(qr, dm, alias, entityField)
                    entityField.isSimple -> buildSimpleField(qr, dm, alias, entityField)
                    entityField.isM1 -> buildManyToOneField(qr, projection, entityField)
                    entityField.is1N -> buildDataProjection(qr, projection, entityField.name)
                    else -> throw NIY()
                }
            }
        }
        qr.stack.pop()
    }


    private fun buildManyToOneField(qr: QueryBuildContext, projection: DataProjection, entityField: DataField) {
        //1е - сначала надо понять, не являемся ли мы обратнной ссылкой на уже доставаемый объект
        //в этом случае нам не надо строить всю проекцию для такой обратной ссылки, а просто смаппировать нужный
        //айдишник в поле данной проекции
        if (!isBackRef(qr, entityField))
            buildDataProjection(qr, projection, entityField.name)
    }

    private fun isBackRef(qr: QueryBuildContext, entityField: DataField): Boolean {
        val currLevel = qr.stack.peek()
        if (currLevel.parent == null)
            return false
        val parent = currLevel.parent

        //нашли родительское поле

        val parentField = parent.dm[currLevel.parentLinkField!!]
        if (parentField.referenceTo() == currLevel.dm.name
                && parentField.thatSideJoinColumn() == entityField.thisSideJoinColumn()
                && parentField.thisSideJoinColumn() == entityField.thatSideJoinColumn()) {
            //todo: сделать здесь маппинг
            //итак, это поле является обратной ссылкой на родителя (родитель->дочерняя коллекция->элемент->ссылка на родителя)
            //при маппировании необходимо в текущую сущность положить ссыль на родителя
            //смаппируем это на ID текущей сущноси
            val idAlias = qr.getColumnAlias(currLevel.alias, currLevel.dm.idColumn)
            qr.addMapper(idAlias, { mc, rs ->
                val parentmapa = mc.curr[parent.alias]
                val currentmapa = mc.curr[currLevel.alias]
                parentmapa?.let {
                    currentmapa?.let {
                        currentmapa[entityField.name] = parentmapa
                        currentmapa.addBackRef(entityField.name)
                    }
                }

            }
            )

            return true
        }
        return false
    }


    private fun buildJoin(qr: QueryBuildContext, parent: QueryLevel?, me: QueryLevel): String {
        var ref = parent!!.dm[me.parentLinkField!!]

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
                qr.getSelectString() + "\n" +
                "FROM " + qr.from +
                qr.getJoinString()

        return SqlQueryContext(sql, dp, emptyMap(), qr)
    }

    private fun buildIDfield(qr: QueryBuildContext, dm: DataMapping, entityAlias: String, entityField: DataField) {
        val columnAlias = qr.addSelect(entityAlias, entityField.sqlcolumn)
        val ql = qr.stack.peek()
        qr.addMapper(columnAlias, { mc, rs ->
            val id = dataConverter.convert(rs.getObject(columnAlias), Long::class.java)
            when {
                id == null ->
                    mc.curr(ql.parent!!.alias)?.let {
                        mc.curr(ql.parent.alias)!!.nullf(ql.parentLinkField!!)
                    }

                else -> {
                    val datamap = mc.create(entityAlias, dm.name, id)

                    ql.parentLinkField?.let {
                        val parentField = ql.parent!!.dm[ql.parentLinkField]
                        when {
                            parentField.isM1 -> mc.curr(ql.parent.alias)!![ql.parentLinkField] = datamap
                            parentField.is1N -> mc.curr(ql.parent.alias)!!.list(ql.parentLinkField).add(datamap)
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
                mc.curr(entityAlias)!![entityField.name] = rs.getObject(columnAlias)
        })

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


}