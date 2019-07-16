package com.bftcom.ice.server.datamaps

import com.bftcom.ice.common.maps.DataProjectionF
import com.bftcom.ice.common.maps.UnionProjection
import com.bftcom.ice.common.maps.projection
import org.springframework.stereotype.Service
import javax.annotation.Resource

const val __level = "__level"

@Service
class QueryRecursiveBuilder {


    @Resource
    private lateinit var queryBuilder: QueryBuilder

    @Resource
    private lateinit var queryUnionBuilder: QueryUnionBuilder


    fun createRecursiveFindQuery(proj: DataProjectionF<*>, parentField: String, childs: Boolean, includeLevel0: Boolean):
            SqlQueryContext {

        val u1 = proj
        if (u1[parentField] == null)
            u1.field(parentField)
        u1[parentField]!!.option().asMapWithOnlyId()

        val u2 = proj.copy()
        if (u2[parentField] == null)
            u2.field(parentField)
        u2.filter = null
        u2.oql = null
        u2[parentField]!!.option().asMapWithOnlyId()

        return createRecursiveQueryByDataProjections(u1 UNION u2, parentField, childs, includeLevel0)
    }

    fun createRecursiveQueryByDataProjections(up: UnionProjection, parentField: String, childs: Boolean, includeLevel0: Boolean): SqlQueryContext {
        val params = mutableMapOf<String, Any?>()

        //строим основную структуру юниона
        val pair =
                if (childs)
                    buildChildQueryStructure(up, params, parentField)
                else
                    buildParentQueryStructure(up, params, parentField, includeLevel0)

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
        queryUnionBuilder.appendWhere(up, qr, params, sql)

        //строим СОРТИРОВКИ
        queryUnionBuilder.appendOrders(up, qr, sql)

        //ЛИМИТ
        queryUnionBuilder.appendLimitOffset(qr, params, sql)


        return SqlQueryContext(sql.toString(), params, qr)
    }


    private fun buildChildQueryStructure(up: UnionProjection,
                                         params: MutableMap<String, Any?>,
                                         parentField: String):
            Triple<String, SqlQueryContext, Int> {

        var paramCounterStart1 = 0

        val p0 = up.projections[0]

        p0.formula(__level, "1")
        val firstCtx1: SqlQueryContext = queryBuilder.createQueryByDataProjection(p0, paramCounterStart1)
        params.putAll(firstCtx1.params)
        paramCounterStart1 = firstCtx1.params.size

        val p1 = up.projections[1]
        p1.formula(__level, "$__level+1")
        val firstCtx2: SqlQueryContext = queryBuilder.createQueryByDataProjection(up.projections[1], paramCounterStart1)
        params.putAll(firstCtx2.params)

        ///FROM DEPARTMENT DEPARTMENT1  JOIN r r1 ON DEPARTMENT1.parent_id = r1.id1
        val recursivePart = getRecursiveJoin(firstCtx2, parentField)

        var sql = listOf(firstCtx1.sql, firstCtx2.sql + recursivePart)
                .joinToString(" \n\n\tUNION\n\n")

        sql = "WITH RECURSIVE __R__ AS ($sql)  \n\n\tSELECT * FROM __R__"
        return Triple(sql, firstCtx1, paramCounterStart1)
    }

    private fun buildParentQueryStructure(up: UnionProjection,
                                          params: MutableMap<String, Any?>,
                                          parentField: String,
                                          includeLevel0: Boolean):
            Triple<String, SqlQueryContext, Int> {

        var paramCounterStart1 = 0

        val p0 = up.projections[0]

        p0.formula(__level, "0")
        val firstCtx1: SqlQueryContext = queryBuilder.createQueryByDataProjection(p0, paramCounterStart1)
        params.putAll(firstCtx1.params)
        paramCounterStart1 = firstCtx1.params.size

        val p1 = up.projections[1]
        p1.formula(__level, "$__level-1")
        val firstCtx2: SqlQueryContext = queryBuilder.createQueryByDataProjection(up.projections[1], paramCounterStart1)
        params.putAll(firstCtx2.params)

        /// JOIN __R__ r1 ON department1.id = r1.parent_id1
        val recursivePart = getRecursiveParentJoin(firstCtx2, parentField)

        var sql = listOf(firstCtx1.sql, firstCtx2.sql + recursivePart)
                .joinToString(" \n\n\tUNION\n\n")

        sql = "WITH RECURSIVE __R__ AS ($sql)  \n\n\tSELECT * FROM __R__ " +
                if(includeLevel0) "" else " where $__level < 0 "
        return Triple(sql, firstCtx1, paramCounterStart1)
    }


    private fun getRecursiveParentJoin(firstCtx2: SqlQueryContext, parentField: String): String {
        val tableAlias = firstCtx2.qr.root.alias
        val idColumn = firstCtx2.qr.root.dm.idColumn
        val parentColumn = firstCtx2.qr.root.dm[parentField].sqlcolumn
        val parentColAlias = firstCtx2.qr.getColumnAlias(tableAlias, parentColumn).substringAfter(".")


        return "\nJOIN __R__ r1 ON $tableAlias.$idColumn = r1.$parentColAlias"
    }

    private fun getRecursiveJoin(firstCtx2: SqlQueryContext, parentField: String): String {
        val tableAlias = firstCtx2.qr.root.alias
        val idColumn = firstCtx2.qr.root.dm.idColumn
        val idColAlias = firstCtx2.qr.getColumnAlias(tableAlias, idColumn).substringAfter(".")

        val parentColumn = firstCtx2.qr.root.dm[parentField].sqlcolumn

        return "\nJOIN __R__ r1 ON $tableAlias.$parentColumn = r1.$idColAlias"
    }


}
