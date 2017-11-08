package com.datamaps.services

import com.datamaps.general.NIY
import com.datamaps.general.SNF
import com.datamaps.mappings.DataField
import com.datamaps.mappings.DataMapping
import com.datamaps.mappings.DataMappingsService
import com.datamaps.mappings.DataProjection
import org.springframework.stereotype.Service
import java.util.*
import java.util.stream.Collectors.joining
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 */

@Service
class QueryBuilder {
    @Resource
    lateinit var dataMappingsService: DataMappingsService;

    fun createQueryForEntity(qr: QueryBuildContext, parent: DataProjection, field: String) {

    }

    fun createQueryByEntityNameAndId(name: String, id: Long): SqlQuery {
        var dm = dataMappingsService.getDataMapping(name)
        //val query = Query()
        throw NIY()
    }

    fun createQueryByDataProjection(dp: DataProjection): SqlQuery {

        val qr = QueryBuildContext()

        buildDataProjection(qr, dp, null)

        return builqSqlQuery(qr, dp)
    }

    class QueryBuildContext {

        var aliasCounters = mutableMapOf<String, Int>()
        var selectColumns = mutableSetOf<String>()
        var joins = mutableSetOf<String>()
        lateinit var from: String

        var stack = Stack<QueryLevel>()

        fun getSelectString(): String {
            return selectColumns.stream()
                    .collect(joining(", "))
        }

        fun getJoinString(): String {
            return joins.stream()
                    .collect(joining("\r\n\t "))
        }

        fun addSelect(alias: String, column: String) {
            selectColumns.add(alias + "." + column)
        }

        fun getAlias(table: String): String {
            aliasCounters.putIfAbsent(table, 0)
            val counter = aliasCounters.computeIfPresent(table) { s, integer -> integer + 1 }!!
            return table + counter;
        }
    }

    class QueryLevel(var dm: DataMapping, var dp: DataProjection, var alias: String, var field: String?) {

    }


    private fun buildDataProjection(qr: QueryBuildContext, dp: DataProjection, field: String?) {

        val isRoot = field == null

        //если мы на руте - берем рутовый маппинг
        val dm = if (isRoot)
            dataMappingsService.getDataMapping(dp.entity!!)
        else
            dataMappingsService.getRefDataMapping(qr.stack.peek().dm, field!!)

        //если мы на руте - берем рутовую проекцию, иначе берем проекцию с поля
        val projection = if (isRoot) dp else dp[field!!]

        //генерим алиас
        val alias = qr.getAlias(dm.table)

        val ql = QueryLevel(dm, projection, alias, field)
        //ддля рута - формируем клауз FROM
        if (isRoot)
            qr.from = dm.table + " as " + alias
        //для ссылки - формируем JOIN
        else
            buildJoin(qr, qr.stack.pop(), ql)

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
                    entityField.isSimple -> buildSimpleField(qr, dm, alias, entityField)
                    entityField.isM1 -> buildDataProjection(qr, dp, entityField.name)
                    else -> throw NIY()
                }
            }
        }
        qr.stack.pop()
    }

    private fun buildJoin(qr: QueryBuildContext, parent: QueryLevel?, me: QueryLevel): String {
        var ref = parent!!.dm[me.field!!]

        return "LEFT JOIN ${me.dm.table} ON " +
                parent.alias + "." + ref.sqlcolumn + "=" + me.alias + "." +ref.manyToOne!!.joinColumn
    }

    private fun buildM1Field(qr: QueryBuildContext, dm: DataMapping, alias: String, entityField: DataField) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun builqSqlQuery(qr: QueryBuildContext, dp: DataProjection): SqlQuery {

        val sql = "SELECT \n\t" +
                qr.getSelectString() + "\n" +
                "FROM " + qr.from +
                qr.getJoinString()

        return SqlQuery(sql, dp, emptyMap())
    }

    private fun buildSimpleField(qr: QueryBuildContext, dm: DataMapping, alias: String, entityField: DataField) {
        qr.addSelect(alias, entityField.sqlcolumn)
    }


    //получаем список всех полей которые мы будем селектить
    //все поля =
    //      поля дефлотной группы
    //  U   поля всех указанных групп (groups)
    //  U   поля указанные в списке fields
    private fun getAllFieldsOnLevel(dp: DataProjection, dm: DataMapping): Set<String> {
        val allFields = mutableSetOf<String>()

        // поля дефлотной группы
        allFields.addAll(dm.defaultGroup.fields)

        //поля всех указанных групп (groups)
        dp.groups.forEach { gr ->
            run {
                val datagroup = dm.groups.computeIfAbsent(gr,
                        { t -> throw SNF("group ${gr} of ${dp.entity} entity not found") })
                allFields.addAll(datagroup.fields)
            }
        }
        //
        dp.fields.forEach { f -> allFields.add(f.key) }

        return allFields
    }


}