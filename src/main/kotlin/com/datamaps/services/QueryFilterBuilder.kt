package com.datamaps.services

import com.datamaps.general.throwNIS
import com.datamaps.general.validateNIY
import com.datamaps.maps.*
import org.apache.commons.lang.text.StrSubstitutor
import org.springframework.stereotype.Service

/**
 * Created by b.schukin on 21.11.2017.
 */
@Service
class QueryFilterBuilder {


    fun buildOrders(qr: QueryBuildContext) {
        if (qr.root.dp.orders().size == 0)
            return

        val projection = qr.root.dp

        qr.orderBy +=
                projection.orders().joinToString(", ") { m ->
                    buildFieldOrder(qr, m)
                }
    }

    private fun buildFieldOrder(qr: QueryBuildContext, f: f): String {
        return qr.getFieldNameInQuery(f.name) + (if (f.asc) " ASC" else " DESC")
    }

    fun buildWhere(qr: QueryBuildContext) {

        val querylevel = qr.root
        val projection = querylevel.dp

        //пока мы строим только запрос по ID
        projection.id?.let {
            validateNIY(projection.isRoot())

            qr.where = "${querylevel.alias}.${querylevel.dm.idColumn} = :_id1"
            qr.params["_id1"] = projection.id
        }

        projection.filter()?.let {
            if (!qr.where.isBlank())
                qr.where += " AND "

            qr.where += buildWhereByExp(qr, it)
        }

        projection.where()?.let {
            if (!qr.where.isBlank())
                qr.where += " AND "

            qr.where += buildWhereByQOLString(qr, it)
            qr.params.putAll( qr.root.dp.params )
        }

    }

    private fun buildWhereByQOLString(qr: QueryBuildContext, it: String): String {

        val resolver = QueryVariablesResolver(qr, qr.root)
        val s = StrSubstitutor(resolver, "{{", "}}", '/')
        return s.replace(it)

    }

    private fun buildWhereByExp(qr: QueryBuildContext, exp: exp): String {
        return when (exp) {
            is f -> qr.getFieldNameInQuery(exp.name)
            is value -> buildFilterValue(qr, exp)
            is binaryOP -> buildBinaryOperation(qr, exp)
            is OR -> "(${buildWhereByExp(qr, exp.left)} OR ${buildWhereByExp(qr, exp.right)})"
            is AND -> "(${buildWhereByExp(qr, exp.left)} AND ${buildWhereByExp(qr, exp.right)})"
            is NOT -> "NOT (${buildWhereByExp(qr, exp.right)})"
            is NULL -> " NULL "
            else -> throwNIS()
        }
    }

    private fun buildBinaryOperation(qr: QueryBuildContext, exp: binaryOP): String {

        return when {
            exp.op == Operation.inn -> "${buildWhereByExp(qr, exp.left)} ${exp.op.value} (${buildWhereByExp(qr, exp.right)})"
            else -> "${buildWhereByExp(qr, exp.left)} ${exp.op.value} ${buildWhereByExp(qr, exp.right)}"
        }

    }


    private fun buildFilterValue(qr: QueryBuildContext, exp: value): String {
        return ":${qr.addParam(exp.v)}"
    }


}