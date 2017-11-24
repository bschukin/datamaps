package com.datamaps.services

import com.datamaps.general.throwNIS
import com.datamaps.mappings.DataMapping
import com.datamaps.mappings.DataProjection
import com.datamaps.mappings.f
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

    //карта ключ: {tableAlias.parentField}-->{уникальный алиас колонки в запросе}
    var columnAliases = linkedCaseInsMapOf<String>()

    //карта - таблица - выданное число алиасов
    var aliasTableCounters = mutableMapOf<String, Int>()

    //карта - таблица - выданное число алиасов
    private val tableAliases = caseInsMapOf<QueryLevel>()

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

    //корень  "дерева" запроса
    lateinit var root: QueryLevel

    //карта  алиасРоиделя.свойство -> алиас таблицы
    //используется для вычисления алиаса таблицы при работе с влложеными путями в фильттрах
    private var parentPathes = mutableMapOf<SPair, String>()

    //карта "алиас колонки" - маппер колонки
    var columnMappers = mutableMapOf<String, MutableList<RowMapper>>()

    var stack = Stack<QueryLevel>()

    //строка where
    var where = ""

    //строка order
    var orderBy = ""

    var params = mutableMapOf<String, Any?>()

    var paramNameCounter = 0

    var offset: Int? = null
    var limit: Int? = null

    fun getSelectString(): String {
        return selectColumns.stream()
                .collect(Collectors.joining(", "))
    }

    fun getJoinString(): String {
        return joins.stream()
                .collect(Collectors.joining(" "))
    }

    fun addSelect(tableAlias: String, column: String?): String {
        val res = getColumnAlias(tableAlias, column)
        selectColumns.add(" ${tableAlias}.${column}  AS  ${res}")
        return res
    }

    fun addTableAlias(alias: String, queryLevel: QueryLevel) {
        tableAliases[alias] = queryLevel
    }

    fun isTableAlias(alias: String): Boolean = tableAliases.containsKey(alias)

    fun getAliasQueryLevel(alias: String): QueryLevel = tableAliases[alias]!!


    fun addParentPathAlias(parentAlias: String?, parentProp: String?, alias: String) {
        val key = SPair(parentAlias ?: "", parentProp ?: "")
        parentPathes.merge(key, alias, { t, u -> throwNIS("${key} already exist") })
    }

    fun getAliasByPathFromParent(parentAlias: String?, parentProp: String?): String? {

        if (parentAlias.isNullOrBlank())
            return rootAlias

        val key = SPair(parentAlias ?: "", parentProp ?: "")
        return parentPathes[key]
    }

    fun addMapper(alias: String, rowMapper: RowMapper) {
        columnMappers.putIfAbsent(alias, mutableListOf())
        columnMappers[alias]!!.add(rowMapper)
    }

    fun addJoin(alias: String) {
        joins.add(alias)
    }

    fun createTableAlias(table: String): String {
        aliasTableCounters.putIfAbsent(table, 0)
        val counter = aliasTableCounters.computeIfPresent(table) { s, integer -> integer + 1 }!!
        return table + counter
    }


    fun getColumnAlias(tableAlias: String, identifier: String?): String {
        val fullName = tableAlias + "." + identifier
        return columnAliases.computeIfAbsent(fullName, { o ->
            run {
                aliasColumnCounters.putIfAbsent(identifier!!, 0)
                val counter = aliasColumnCounters.computeIfPresent(identifier) { s, integer -> integer + 1 }!!
                identifier + counter
            }
        })
    }

    ///добавить параметр в карту, сгенерировать для него имя и вернуть
    fun addParam(value: Any): String {
        val name = "param${paramNameCounter++}"
        params.put(name, value)
        return name
    }

    //нам надо для фильтруемой проперти - выстроить путь к ней (если он не был построен)
    // (если колонка например не участвует в селекте)
    //и получить имя фильтрумой колонки в запросе (то есть имя алиаса таблицы - наименование колонки)
    //или (при returnTableQualifiedName=false - вернуть уникальный алиас колонки в запросе)
    //NB: при создании цепочек свойств мы считаем что свойства пишутся от корневой сущности (или от указанного пользователем алиаса)
    //например: JiraStaffUnit --> Worker-->Gender-->gender будут писаться в фильтре как worker.gender.gender (рута нет)
    fun getFieldNameInQuery(exp: f, returnTableQualifiedName: Boolean = true): String {
        var alias = rootAlias
        var currLevel = root
        val list = exp.name.split('.')

        for (i in 0 until list.size - 1) {
            if (i == 0 && isTableAlias(list[i])) {
                alias = list[i]
                currLevel = getAliasQueryLevel(list[i])

            } else {
                alias = getAliasByPathFromParent(alias, list[i])!!
                currLevel = currLevel.childProps[list[i]]!!
            }

        }

        if (returnTableQualifiedName)
            return "${alias}.${currLevel.dm.get(list.last()).sqlcolumn!!}"
        else
            return getColumnIdentiferForFillter(alias, currLevel.dm.get(list.last()).sqlcolumn!!)
    }

    fun getColumnIdentiferForFillter(tableAlias: String, identifier: String): String {
        val fullName = tableAlias + "." + identifier
        return columnAliases[fullName] ?: fullName
    }

}

class QueryLevel(var dm: DataMapping, var dp: DataProjection, var alias: String,
                 val parentLinkField: String?, val parent: QueryLevel?) {

    var childProps = caseInsMapOf<QueryLevel>()

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

        if (alias == q.qr.rootAlias)
            resultMap.putIfAbsent(id, datamap)

        return datamap
    }

    fun curr(name: String): DataMap? = curr[name]

    fun clear() = curr.clear()


    fun result(): List<DataMap> = resultMap.values.toList()

}

//public interface RowMapper //: BiFunction<MappingContext, ResultSet, Boolean>
//{}

typealias RowMapper = (MappingContext, ResultSet) -> Unit

class SqlQueryContext(val sql: String, val dataProjection: DataProjection,
                      val params: Map<String, Any?>, var qr: QueryBuildContext) {
}

class SPair {

    val s1: String
    val s2: String

    constructor(s1: String, s2: String) {
        this.s1 = s1.toLowerCase()
        this.s2 = s2.toLowerCase()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SPair

        if (s1 != other.s1) return false
        if (s2 != other.s2) return false

        return true
    }

    override fun hashCode(): Int {
        var result = s1.hashCode()
        result = 31 * result + s2.hashCode()
        return result
    }


}
