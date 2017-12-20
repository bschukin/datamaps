package com.datamaps.services

import com.datamaps.general.throwNIS
import com.datamaps.mappings.DataMapping
import com.datamaps.maps.DataMap
import com.datamaps.maps.DataProjection
import com.datamaps.util.caseInsMapOf
import com.datamaps.util.linkedCaseInsMapOf
import org.apache.commons.lang.text.StrLookup
import java.sql.ResultSet
import java.util.*
import java.util.stream.Collectors

/**
 * Created by b.schukin on 14.11.2017.
 */
class QueryBuildContext(val queryBuilder: QueryBuilder? = null) {

    //карта ключ: {алиасТаблицы.имяколонки}-->{уникальный алиас колонки в запросе}
    var columnAliases = linkedCaseInsMapOf<String>()

    //карта - таблица - выданное число алиасов
    private var aliasTableCounters = mutableMapOf<String, Int>()

    //карта - алиасТаблицы - узел дерева, на котором он был сгенирован
    private val tableAliases = caseInsMapOf<QueryLevel>()

    //карта - колонка(без указания таблицы) - выданное число алиасов
    private var aliasColumnCounters = mutableMapOf<String, Int>()

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

    private var paramNameCounter = 0

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
        selectColumns.add(" $tableAlias.\"$column\"  AS  $res")
        return res
    }

    fun addSelectFromFormula(formulaName: String, formula: String) {
        columnAliases[formulaName] = formulaName
        selectColumns.add(" ($formula) AS  $formulaName")
    }

    fun addTableAlias(alias: String, queryLevel: QueryLevel) {
        tableAliases[alias] = queryLevel
    }

    private fun isTableAlias(alias: String): Boolean = tableAliases.containsKey(alias)

    private fun getAliasQueryLevel(alias: String): QueryLevel = tableAliases[alias]!!


    fun addParentPathAlias(parentAlias: String?, parentProp: String?, alias: String) {
        val key = SPair(parentAlias ?: "", parentProp ?: "")
        parentPathes.merge(key, alias, { _, _ -> throwNIS("$key already exist") })
    }

    fun getAliasByPathFromParent(ql: QueryLevel,parentAlias: String?, parentProp: String?): String? {

        if (parentAlias.isNullOrBlank())
            return rootAlias

        val key = SPair(parentAlias ?: "", parentProp ?: "")
        var res = parentPathes[key]
        if (res == null) {
            queryBuilder!!.lateCreateAlias(this, ql, parentProp!!)
            res = parentPathes[key]
        }
        return res
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
        val counter = aliasTableCounters.computeIfPresent(table) { _, integer -> integer + 1 }!!
        return table + counter
    }


    fun getColumnAlias(tableAlias: String, identifier: String?): String {
        val fullName = tableAlias + "." + identifier
        return columnAliases.computeIfAbsent(fullName, { _ ->
            run {
                aliasColumnCounters.putIfAbsent(identifier!!, 0)
                val counter = aliasColumnCounters.computeIfPresent(identifier) { _, integer -> integer + 1 }!!
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
    fun getFieldNameInQuery(name: String, startAlias: String = rootAlias, startLevel: QueryLevel = root,
                            returnTableQualifiedName: Boolean = true): String {
        var alias = startAlias
        var currLevel = startLevel
        val list = name.split('.')

        for (i in 0 until list.size - 1) {
            if (i == 0 && isTableAlias(list[i])) {
                alias = list[i]
                currLevel = getAliasQueryLevel(list[i])

            } else {
                alias = getAliasByPathFromParent(currLevel, alias, list[i])!!
                currLevel = currLevel.childProps[list[i]]!!
            }

        }

        return if (returnTableQualifiedName)
            currLevel.getSqlColumn(alias, list.last())
        else
            getColumnIdentiferForFilter(alias, currLevel.getSqlColumn(alias, list.last(), false))
    }

    private fun getColumnIdentiferForFilter(tableAlias: String, identifier: String): String {
        val fullName = tableAlias + "." + identifier
        return columnAliases[fullName] ?: fullName
    }

}

class QueryLevel(var dm: DataMapping, var dp: DataProjection, var alias: String,
                 val parentLinkField: String?, val parent: QueryLevel?, val dbDialect: DbDialect) {

    var childProps = caseInsMapOf<QueryLevel>()

    fun getSqlColumn(alias: String, name: String, useAlias: Boolean = true): String {
        if (dp.formulas.containsKey(name))
            return "(${dp.formulas[name]!!})"

        if (dp.isLateral(alias))
            return "$alias.$name"

        return if (useAlias)
            "$alias.${getEscapedColumn(dm[name].sqlcolumn!!)}"
        else
            getEscapedColumn(dm[name].sqlcolumn!!)
    }

    fun getEscapedColumn(name: String) = dbDialect.getQuotedDbIdentifier(name)

}


class MappingContext(private val q: SqlQueryContext) {

    //карта карт: "Ентити" -> {карта {id of entity}->{DataMap}  }
    private var mapOfMaps = caseInsMapOf<MutableMap<Long, DataMap>>()

    //карта "текущих" сущностей в текущей строке RowResult
    //{alias таблицы}-->{DataMap}
    //используется при маппировании
    var curr = caseInsMapOf<DataMap>()

    private var resultMap = linkedMapOf<Long, DataMap>()

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


typealias RowMapper = (MappingContext, ResultSet) -> Unit

class SqlQueryContext(val sql: String, val params: Map<String, Any?>,
                      var qr: QueryBuildContext)

class SPair(s1: String, s2: String) {

    private val s1: String = s1.toLowerCase()
    private val s2: String = s2.toLowerCase()

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


class QueryVariablesResolver(private val qr: QueryBuildContext, private val ql: QueryLevel) : StrLookup() {
    override fun lookup(key: String?): String {
        return qr.getFieldNameInQuery(key!!, ql.alias, ql, true)
    }
}