package com.datamaps.services

import com.datamaps.general.NIY
import com.datamaps.general.SNF
import com.datamaps.mappings.DataField
import com.datamaps.mappings.DataMapping
import com.datamaps.mappings.DataMappingsService
import com.datamaps.mappings.DataProjection
import org.springframework.stereotype.Service
import java.util.stream.Collectors.joining
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 */

@Service
class QueryBuilder {
    @Resource
    lateinit var dataMappingsService: DataMappingsService;

    fun createQueryByEntityNameAndId(name: String, id: Long): SqlQuery {
        var dm = dataMappingsService.getDataMapping(name)
        //val query = Query()
        throw NIY()
    }

    class QueryResult {
        var aliasCounters = mutableMapOf<String, Int>()
        var selectColumns = mutableSetOf<String>()
        var joins = mutableSetOf<String>()
        lateinit var from: String

        fun getSelectString(): String {
            return selectColumns.stream()
                    .collect(joining(", "))
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

    fun createQueryByDataProjection(dp: DataProjection): SqlQuery {
        val dm = dataMappingsService.getDataMapping(dp.entity!!)

        val qr = QueryResult()
        val alias = qr.getAlias(dm.table)

        qr.from = dm.table + " as " + alias
        //получаем список всех полей которые мы будем селектить
        //все поля =  поля всех указанных групп (groups) U поля указанные в списке fields
        val allFields = getAllFieldsOnLevel(dp, dm)

        //бежим по всем полям и решаем что с кажным из них делать
        allFields.forEach { f ->
            run {
                val entityField = dm[f]
                if (!entityField.isSimple)
                    throw NIY()
                when {
                    entityField.isSimple -> buildSimpleField(qr, dm, alias, entityField)
                }
            }
        }

        return builqSqlQuery(qr, dp)
    }

    private fun builqSqlQuery(qr: QueryResult, dp: DataProjection): SqlQuery {

        val sql = "SELECT \n\t" +
                qr.getSelectString() + "\n" +
                "FROM " + qr.from

        return SqlQuery(sql, dp, emptyMap())
    }

    private fun buildSimpleField(qr: QueryResult, dm: DataMapping, alias: String, entityField: DataField) {
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
        //поля указанные в списке fields
        dp.fields.forEach { f -> allFields.add(f.key) }

        return allFields
    }

    fun createQueryForEntity(qr: QueryResult, parent: DataProjection, field: String) {

    }

}