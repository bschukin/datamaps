package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.misc.caseInsMapOf
import com.bftcom.ice.datamaps.misc.linkedCaseInsMapOf
import com.bftcom.ice.datamaps.misc.throwImpossible
import com.bftcom.ice.datamaps.DataMapF.Companion.entityDiscriminator
import com.bftcom.ice.datamaps.core.dialects.DbDialect
import com.bftcom.ice.datamaps.core.mappings.DataMapping
import com.bftcom.ice.datamaps.core.util.getJDBCTypeByFieldType
import com.bftcom.ice.datamaps.misc.FieldSetRepo
import org.apache.commons.text.StrLookup
import java.sql.ResultSet
import java.util.*
import java.util.stream.Collectors

/**
 * Created by b.schukin on 14.11.2017.
 */
internal class QueryBuildContext(val queryBuilder: QueryBuilder? = null, paramCounterStart: Int = 0) {

    //карта ключ: {алиасТаблицы.имяколонки}-->{уникальный алиас колонки в запросе}
    var columnAliases = linkedCaseInsMapOf<String>()

    //карта - таблица - выданное число алиасов
    private var aliasTableCounters = mutableMapOf<String, Int>()

    //карта - алиасТаблицы - узел дерева, на котором он был сгенирован
    private val tableAliases = caseInsMapOf<QueryLevel>()

    //карта - колонка(без указания таблицы) - выданное число алиасов
    private var aliasColumnCounters = caseInsMapOf<Int>()

    //колонки для селекта в формате {алиасТаблицы.имяколонки AS алиасКолонки}
    var selectColumns = mutableSetOf<String>()

    //поля сущности в порядке колонок селекта
    var selectedEntityFields = mutableSetOf<String>()

    //joinы
    internal var joins = mutableListOf<Join>()

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

    //строка group by
    var groupBy = ""

    var params = mutableMapOf<String, Any?>()

    internal var paramNameCounter = paramCounterStart

    var offset: Int? = null
    var limit: Int? = null

    var postMappers = mutableListOf<PostMapper>()

    fun clone(): QueryBuildContext {
        val qbc = QueryBuildContext(this.queryBuilder)
        qbc.columnAliases = this.columnAliases
        qbc.aliasTableCounters = this.aliasTableCounters
        qbc.tableAliases.putAll(this.tableAliases)
        qbc.selectedEntityFields = this.selectedEntityFields
        qbc.joins = this.joins
        qbc.from = this.from
        qbc.rootAlias = this.rootAlias
        qbc.root = this.root
        qbc.parentPathes = this.parentPathes
        qbc.columnMappers = this.columnMappers
        qbc.groupBy = this.groupBy
        return qbc
    }

    fun isGroupByQuery() = root.dp.groupByFields.size>0

    fun getSelectString(): String {
        return selectColumns.stream()
                .collect(Collectors.joining(", "))
    }

    fun getJoinString(): String {
        return joins.stream().map { it.getResultJoinString() }
                .collect(Collectors.joining(" "))
    }

    fun addSelect(tableAlias: String, column: String?, field: String?): String {
        val res = getColumnAlias(tableAlias, column)
        selectColumns.add(" $tableAlias.${root.getEscapedColumn(column!!)}  AS  $res")
        field?.let {
            selectedEntityFields.add(field)
        }
        return res
    }

    fun addSelectFromFormula(formulaName: String, formula: String) {
        columnAliases[formulaName] = formulaName
        selectColumns.add(" ($formula) AS  $formulaName")
        selectedEntityFields.add(formulaName)
    }

    fun addTableAlias(alias: String, queryLevel: QueryLevel) {
        tableAliases[alias] = queryLevel
    }

    private fun isTableAlias(alias: String): Boolean = tableAliases.containsKey(alias)

    private fun getAliasQueryLevel(alias: String): QueryLevel = tableAliases[alias]!!


    fun addParentPathAlias(parentAlias: String?, parentProp: String?, alias: String) {
        val key = SPair(parentAlias ?: "", parentProp ?: "")
        parentPathes.merge(key, alias, { _, _ -> throwImpossible("$key already exist") })
    }

    fun getAliasByPathFromParent(ql: QueryLevel, parentAlias: String?, parentProp: String?): String? {

        if (parentAlias.isNullOrBlank())
            return rootAlias

        val key = SPair(parentAlias, parentProp ?: "")
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

    internal fun addJoin(joinSql: String, joinFilter: Expression?=null): Join {
        val j = Join(joinSql)

        joinFilter?.let {
            j.addJoinFilter(it)
        }

        joins.add(j)
        return j
    }

    fun createTableAlias(table: String): String {
        aliasTableCounters.putIfAbsent(table, 0)
        val counter = aliasTableCounters.computeIfPresent(table) { _, integer -> integer + 1 }!!
        return table + counter
    }


    fun getColumnAlias(tableAlias: String, identifier: String?): String {
        val fullName = "$tableAlias.$identifier"
        return columnAliases.computeIfAbsent(fullName, {
            run {
                aliasColumnCounters.putIfAbsent(identifier!!, 0)
                val counter = aliasColumnCounters.computeIfPresent(identifier) { _, integer -> integer + 1 }!!
                identifier + counter
            }
        })
    }

    ///добавить параметр в карту, сгенерировать для него имя и вернуть
    fun addParam(value: Any?): String {
        val name = "param${paramNameCounter++}"
        params[name] = extractIdIfNeed(value)
        return name
    }

    //нам надо для фильтруемой проперти - выстроить путь к ней (если он не был построен)
    // (если колонка например не участвует в селекте)
    //и получить имя фильтрумой колонки в запросе (то есть имя алиаса таблицы - наименование колонки)
    //или (при returnTableQualifiedName=false - вернуть уникальный алиас колонки в запросе)
    //NB: при создании цепочек свойств мы считаем что свойства пишутся от корневой сущности (или от указанного пользователем алиаса)
    //например: Department --> Boss-->City-->city будут писаться в фильтре как worker.gender.gender (рута нет)
    fun getFieldNameInQuery(name: String, startAlias: String = rootAlias, startLevel: QueryLevel = root): String {
        var alias = startAlias
        var currLevel = startLevel
        val list = name.split('.')

        for (i in 0 until list.size - 1) {
            if (i == 0 && isTableAlias(list[i])) {
                alias = list[i]
                currLevel = getAliasQueryLevel(list[i])

            } else {
                if (currLevel.dm[list[i]].isJson())
                    return getJsonFieldExpressionInSql(currLevel, list, i, alias)
                alias = getAliasByPathFromParent(currLevel, alias, list[i])!!
                currLevel = currLevel.childProps[list[i]]!!
            }
        }

        return currLevel.getSqlColumn(alias, list.last())
    }

    //получить выражение поля в виде JSON
    //TODO: унести в диалект
    private fun getJsonFieldExpressionInSql(currLevel: QueryLevel, list: List<String>,
                                            i: Int, alias: String): String {
        val res1 = currLevel.getSqlColumn(alias, list[i])
        val jsonPart = listOf(res1).union(list.subList(i + 1, list.lastIndex).map { "'$it'" })
        val jsonPath = jsonPart.joinToString ( " -> " )
        var res = jsonPath + " ->> '${list.last()}'"

        val jsonField = FieldSetRepo.findDynamicFieldByPath(currLevel.dm.name, list)

        if(jsonField!=null)
            res = queryBuilder!!.dbDialect.castSqlExpressionToType(res, jsonField.fieldType?.getJDBCTypeByFieldType())

        return res
    }

    private fun extractIdIfNeed(value: Any?): Any? {
        if (value is DataMap) {
            if (value.id == null)
                throwImpossible("datamap with null id in query parameter")
            return value.id!!
        }
        if (value is List<*>) {
            return value.map { v -> extractIdIfNeed(v!!) }.toList()
        }
        return value
    }

}

class QueryLevel(var dm: DataMapping, var dp: DataProjection, var alias: String,
                 val parentLinkField: String?, val parent: QueryLevel?, private val dbDialect: DbDialect) {

    var childProps = caseInsMapOf<QueryLevel?>()

    fun getSqlColumn(alias: String, name: String, useAlias: Boolean = true): String {
        if (dp.formulas.containsKey(name))
            return "(${dp.formulas[name]!!})"

        if (dp.isLateral(alias) || isLateralFromFieldSet(dm, alias))
            return "$alias.$name"

        return if (useAlias)
            "$alias.${getEscapedColumn(dm[name].sqlcolumn!!)}"
        else
            getEscapedColumn(dm[name].sqlcolumn!!)
    }

    fun getEscapedColumn(name: String) = dbDialect.getQuotedDbIdentifier(name)

    companion object {
        //второй вариант латераля: когда латераль приходит из филдсета (Field.reference  with oqlFormula)
        private fun isLateralFromFieldSet(dm: DataMapping, alias:String) = dm.findByName(alias)?.isReferenceOqlFormula()==true
    }
}


internal open class MappingContext(val q: SqlQueryContext) {

    //карта карт: "Ентити" -> {карта {id of entity}->{DataMap}  }
    protected var mapOfMaps = caseInsMapOf<MutableMap<Any, DataMap>>()

    //карта "текущих" сущностей в текущей строке RowResult
    //{alias таблицы}-->{DataMap}
    //используется при маппировании
    var curr = caseInsMapOf<DataMap>()

    private var resultMap = linkedMapOf<Any, DataMap>()

    /**
     * getOrCreate
     */
    open fun create(alias: String, entityName: String, id: Any, rs: ResultSet): DataMap {

        val map = mapOfMaps.computeIfAbsent(entityName, { mutableMapOf() })
        val datamap = map!!.computeIfAbsent(id, {
            DataMap.existing(entityName, id)
        })
        curr[alias] = datamap

        if (alias == q.qr.rootAlias)
            resultMap.putIfAbsent(id, datamap)

        return datamap
    }

    fun curr(name: String): DataMap? = curr[name]

    fun clear() = curr.clear()

    open fun result(): List<DataMap> = resultMap.values.toList()

}

internal class UnionMappingContext(q: SqlQueryContext) : MappingContext(q) {

    private var resultMap = linkedMapOf<DataMap, DataMap>()

    override fun create(alias: String, entityName: String, id: Any, rs: ResultSet): DataMap {
        val realEntityName = rs.getString(entityDiscriminator)

        val map = mapOfMaps.computeIfAbsent(realEntityName, { mutableMapOf() })
        val datamap = map!!.computeIfAbsent(id, {
            DataMap.existing(realEntityName, id)
        })
        curr[alias] = datamap

        if (alias == q.qr.rootAlias)
            resultMap.putIfAbsent(datamap, datamap)

        return datamap
    }

    override fun result(): List<DataMap> {
        return resultMap.values.toList()
    }
}

internal typealias RowMapper = (MappingContext, ResultSet) -> Unit

internal class SqlQueryContext(val sql: String, val params: Map<String, Any?>,
                      var qr: QueryBuildContext)

internal data class SqlUnionQueryContext(val sql: String, val params: Map<String, Any?>,
                                val firstCtx: SqlQueryContext,
                                val qr: Map<String, SqlQueryContext>)


typealias PostMapper = (List<DataMapF<*>>, DataService) -> List<DataMapF<*>>

private class SPair(s1: String, s2: String) {

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

internal class Join(private val joinString: String) {
    internal var joinFilter: Expression? = null
    internal var joinFilterString: String? = null

    fun getResultJoinString(): String {
        if (joinFilter == null)
            return joinString
        return "$joinString  AND ($joinFilterString)"
    }

    fun addJoinFilter(filter: Expression) {
        if (joinFilter == null)
            joinFilter = filter
        else
            joinFilter!! and filter
    }
}


internal open class QueryVariablesResolver(protected val qr: QueryBuildContext, private val ql: QueryLevel) : StrLookup<String>() {
    override fun lookup(key: String?): String {
        return qr.getFieldNameInQuery(key!!, ql.alias, ql)
    }
}