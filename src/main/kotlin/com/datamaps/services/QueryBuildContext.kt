package com.datamaps.services

import com.datamaps.mappings.DataMapping
import com.datamaps.mappings.DataProjection
import com.datamaps.maps.DataMap
import com.datamaps.util.caseInsMapOf
import com.datamaps.util.linkedCaseInsMapOf
import java.sql.ResultSet
import java.util.*
import java.util.stream.Collectors

/**
 * Created by b.schukin on 14.11.2017.
 */
class QueryBuildContext {

    //карта ключ: {tableAlias.field}-->{уникальный алиас колонки в запросе}
    var columnAliases = linkedCaseInsMapOf<String>()

    //карта - таблица - выданное число алиасов
    var aliasTableCounters = mutableMapOf<String, Int>()

    //карта - колонка(без указания таблицы) - выданное число алиасов
    var aliasColumnCounters = mutableMapOf<String, Int>()

    //колонки для селекта в формате {алиасТаблицы.имяколонки AS алиасКолонки}
    var selectColumns = mutableSetOf<String>()

    //joinы
    var joins = mutableSetOf<String>()

    //FROM {table}
    lateinit var from: String

    //"рутовый" алиас - используется при маппировании ResultSet на  DataMap,
    //для определения сущностей составляющих возваращаемый результат
    lateinit var rootAlias: String

    //карта "алиас колонки" - маппер колонки
    var columnMappers = mutableMapOf<String, RowMapper>()

    var stack = Stack<QueryLevel>()

    fun getSelectString(): String {
        return selectColumns.stream()
                .collect(Collectors.joining(", "))
    }

    fun getJoinString(): String {
        return joins.stream()
                .collect(Collectors.joining(" "))
    }

    fun addSelect(tableAlias: String, column: String): String {
        val res = getColumnAlias(tableAlias, column)
        selectColumns.add(" ${tableAlias}.${column}  AS  ${res}")
        return res
    }

    fun addMapper(alias: String, rowMapper: RowMapper) {
        columnMappers.put(alias, rowMapper)
    }

    fun addJoin(alias: String) {
        joins.add(alias)
    }

    fun getAlias(table: String): String {
        aliasTableCounters.putIfAbsent(table, 0)
        val counter = aliasTableCounters.computeIfPresent(table) { s, integer -> integer + 1 }!!
        return table + counter;
    }


    fun getColumnAlias(tableAlias: String, identifier: String): String {
        val fullName = tableAlias + "." + identifier
        return columnAliases.computeIfAbsent(fullName, { o ->
            run {
                aliasColumnCounters.putIfAbsent(identifier, 0)
                val counter = aliasColumnCounters.computeIfPresent(identifier) { s, integer -> integer + 1 }!!
                identifier + counter
            }
        })
    }
}

class QueryLevel(var dm: DataMapping, var dp: DataProjection, var alias: String, val parentLinkField: String?, val parent:QueryLevel?) {

}


class MappingContext(val q: SqlQueryContext) {

    //карта карт: "Ентити" -> {карта {id of entity}->{DataMap}  }
    var mapOfMaps = caseInsMapOf<MutableMap<Long, DataMap>>()

    //карта "текущих" сущностей в текущей строке RowResult
    //{alias таблицы}-->{DataMap}
    //используется при маппировании
    var curr = caseInsMapOf<DataMap>()

    var resultMap = linkedMapOf<Long, DataMap>()

    /**
     * getOrCreate
     */
    fun create(alias: String, entityName: String, id: Long): DataMap {
        val map = mapOfMaps.computeIfAbsent(entityName, { mutableMapOf() })
        val datamap = map.computeIfAbsent(id, { DataMap(entityName, id) })
        curr[alias] = datamap

        if(alias==q.qr.rootAlias)
            resultMap.putIfAbsent(id, datamap)

        return datamap
    }

    fun curr(name: String): DataMap? = curr[name]

    fun clear()= curr.clear()


    fun result():List<DataMap> = resultMap.values.toList()

}

//public interface RowMapper //: BiFunction<MappingContext, ResultSet, Boolean>
//{}

typealias RowMapper = (MappingContext, ResultSet) -> Unit

class SqlQueryContext(val sql: String, val dataProjection: DataProjection,
                      val params: Map<String, Any>, var qr:QueryBuildContext) {
}