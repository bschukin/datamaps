package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.DataMapF.Companion.entityDiscriminator
import com.bftcom.ice.datamaps.UnionProjection
import com.bftcom.ice.datamaps.core.mappings.GenericDbMetadataService
import com.bftcom.ice.datamaps.projection
import org.apache.commons.text.StrLookup
import org.apache.commons.text.StrSubstitutor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.Resource

@Service
class QueryUnionBuilder {

    @Autowired
    private lateinit var genericDbMetadataService: GenericDbMetadataService

    @Resource
    private lateinit var queryBuilder: QueryBuilder

    @Resource
    private lateinit var unionQueryFilterBuilder: UnionQueryFilterBuilder


    fun createUnionQueryByDataProjections(up: UnionProjection): SqlUnionQueryContext {
        val map = mutableMapOf<String, SqlQueryContext>()
        val params = mutableMapOf<String, Any?>()

        //строим основную структуру юниона
        val pair = buildUnionQueryStructure(up, map, params)
        val sql = StringBuilder().append(pair.first)
        val firstCtx = pair.second
        val paramCounterStart = pair.third

        //клонируем первый контекст
        //чтобы забрать его алиасы и построить на них
        //общие фильтры и сортировки
        val qr = firstCtx.qr.clone()
        qr.paramNameCounter = paramCounterStart + 1
        qr.root.dp = projection().filter(up.filter)
        qr.root.dp._orders = up._orders
        qr.limit = up.limit
        qr.offset = up.offset

        //строим WHERE
        appendWhere(up, qr, params, sql)

        //строим СОРТИРОВКИ
        appendOrders(up, qr, sql)

        //ЛИМИТ
        appendLimitOffset(qr, params, sql)

        return SqlUnionQueryContext(sql.toString(), params, firstCtx, map)
    }

    internal  fun appendLimitOffset(qr: QueryBuildContext, params: MutableMap<String, Any?>, sql: java.lang.StringBuilder) {
        queryBuilder.appendLimitOffset(sql, qr, params)
    }

    internal fun appendWhere(up: UnionProjection, qr: QueryBuildContext, params: MutableMap<String, Any?>,
                             sql: java.lang.StringBuilder) {

        up.filter?.let{

            unionQueryFilterBuilder.buildWhere(qr)
            params.putAll(qr.params)

            sql.append(if (qr.where.isBlank()) " " else " \nWHERE " + qr.where)
        }
        up.where()?.let {
            if (!qr.where.isBlank())
                sql.append( " AND ")
            else
                sql.append(" \nWHERE " + qr.where)
            sql.append(unionQueryFilterBuilder.buildWhereByQOLString(qr, it))
            params.putAll(up.params)
        }
    }

    internal fun appendOrders(up: UnionProjection, qr: QueryBuildContext,
                              sql: java.lang.StringBuilder) {
        if (up._orders.size > 0) {

            unionQueryFilterBuilder.buildOrders(qr)

            sql.append(if (qr.orderBy.isBlank()) " " else " \nORDER BY " + qr.orderBy)
        }
    }

    private fun buildUnionQueryStructure(up: UnionProjection, map: MutableMap<String, SqlQueryContext>, params: MutableMap<String, Any?>):
            Triple<String, SqlQueryContext, Int> {
        var paramCounterStart1 = 0
        var firstCtx1: SqlQueryContext? = null
        var sql = up.projections.map {

            //добавляем дискриминатор
            it.formula(entityDiscriminator, "'${it.entity}'")

            val ctx = queryBuilder.createQueryByDataProjection(it, paramCounterStart1)
            if (firstCtx1 == null) {
                firstCtx1 = ctx
            }

            map.put(it.entity!!, ctx)
            params.putAll(ctx.params)

            paramCounterStart1 += ctx.params.size

            ctx.sql
        }.joinToString(" \n\n\tUNION ALL\n\n")

        sql = "select ${queryBuilder.getLimitOffsetInSelect(firstCtx1!!.qr, up.limit, up.offset)} * from ($sql) "
        if (!genericDbMetadataService.isOracle()) {
            sql+=" as uniontable "
        }
        return Triple(sql, firstCtx1!!, paramCounterStart1)
    }


}

@Service
class UnionQueryFilterBuilder : QueryFilterBuilder() {

    override fun getFieldNameInQuery(qr: QueryBuildContext, field: String): String {
        if (field == entityDiscriminator)
            return entityDiscriminator
        val escapedPath = qr.getFieldNameInQuery(field)
        val path = escapedPath.replace("\"", "")
        val alias = qr.columnAliases[path]!!
        return alias
    }

    internal fun getFieldNameInQuery2(qr: QueryBuildContext, field: String): String {
        return getFieldNameInQuery(qr, field)
    }

    internal fun buildWhereByQOLString(qr: QueryBuildContext, it: String): String {

        val resolver = QueryVariablesResolverU(this, qr)
        val s = StrSubstitutor(resolver, "{{", "}}", '/')
        return s.replace(it)

    }


}

class QueryVariablesResolverU(private val builder: UnionQueryFilterBuilder, private val qr: QueryBuildContext) : StrLookup<String>() {
    override fun lookup(key: String?): String {
        return builder.getFieldNameInQuery2(qr, key!!)
    }
}