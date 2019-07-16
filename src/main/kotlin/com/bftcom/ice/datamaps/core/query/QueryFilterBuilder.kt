package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.core.dialects.DbDialect
import com.bftcom.ice.datamaps.misc.makeSure
import com.bftcom.ice.datamaps.misc.throwNotImplementedYet
import org.apache.commons.text.StrSubstitutor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by b.schukin on 21.11.2017.
 */
@Service
open class QueryFilterBuilder {

    @Autowired
    private lateinit var dbDialect: DbDialect

    fun buildOrders(qr: QueryBuildContext) {
        if (qr.root.dp.orders().isEmpty())
            return

        val projection = qr.root.dp

        qr.orderBy +=
                projection.orders().joinToString(", ") { m ->
                    buildFieldOrder(qr, m)
                }
    }

    fun buildGroupBy(qr: QueryBuildContext) {
        if (qr.root.dp.groupByFields.isEmpty())
            return

        val projection = qr.root.dp

        qr.groupBy += projection.groupByFields.map {
            getFieldNameInQuery(qr, it)
        }.joinToString(", ")
    }

    private fun buildFieldOrder(qr: QueryBuildContext, f: f): String {
        return getFieldNameInQuery(qr, f.name) + (if (f.isasc) " ASC" else " DESC")
    }

    fun buildWhere(qr: QueryBuildContext) {

        val querylevel = qr.root
        val projection = querylevel.dp

        //мы строим запрос по явно указанному ID в проекции
        projection.id?.let {
            makeSure(projection.isRoot())

            qr.where = "${querylevel.alias}.${querylevel.dm.idColumn} = :_id1"
            qr.params["_id1"] = it
        }

        //строим запрос по экспрешшену в фильтре
        projection.filter?.let {
            if (!qr.where.isBlank())
                qr.where += " \n\tAND "

            qr.where += buildWhereByExp(qr, it)
            qr.params.putAll(qr.root.dp.params.mapValues { it.value })
        }

        //строим запрос по OQL
        projection.where()?.let {
            if (!qr.where.isBlank())
                qr.where += " \n\tAND "
            qr.where += buildWhereByQOLString(qr, it)
            qr.params.putAll(qr.root.dp.params.mapValues { it.value })
        }

        //строим запросы для фильтров в JOIN'aх
        qr.joins.filter { it.joinFilter != null }.forEach {
            it.joinFilterString = buildWhereByExp(qr, it.joinFilter!!)
        }

    }

    private fun buildWhereByQOLString(qr: QueryBuildContext, it: String): String {

        val resolver = FilterVariablesResolver(qr, qr.root)
        val s = StrSubstitutor(resolver, "{{", "}}", '/')
        return s.replace(it)
    }

    class FilterVariablesResolver(qr: QueryBuildContext,
                                  ql: QueryLevel) : QueryVariablesResolver(qr, ql) {
        override fun lookup(key: String?): String {
            val s =  super.lookup(key)
            return s
        }
    }

    open fun buildWhereByExp(qr: QueryBuildContext, exp: exp): String {
        return when (exp) {
            is f -> getFieldNameInQuery(qr, exp.name)
            is value -> buildFilterValue(qr, exp)
            is param -> buildParamValue(exp)
            is BinaryOP -> buildBinaryOperation(qr, exp)
            is OR -> "(${buildWhereByExp(qr, exp.left)} OR ${buildWhereByExp(qr, exp.right)})"
            is AND -> "\n\t(${buildWhereByExp(qr, exp.left)} AND ${buildWhereByExp(qr, exp.right)})"
            is NOT -> "NOT (${buildWhereByExp(qr, exp.right)})"
            is NULL -> " NULL "
            is ExpFunction -> buildFunction(qr, exp)
            is OqlExpression -> buildOqlExpression(qr, exp)
            else -> throwNotImplementedYet()
        }
    }

    protected open fun getFieldNameInQuery(qr: QueryBuildContext, field: String): String {
        return qr.getFieldNameInQuery(field)
    }

    private fun buildOqlExpression(qr: QueryBuildContext, exp: OqlExpression): String {
        var oql = exp.oql

        exp.params.forEach { (paramName, paramValue) ->
            val paramPlaceholder = ":$paramName"

            if (!oql.contains(paramPlaceholder)) {
                throw IllegalStateException("Не найден параметр \"$paramName\" в выражении \"${exp.oql}\"")
            }
            oql = oql.replace(paramPlaceholder, buildWhereByExp(qr, paramValue), true)
        }
        return buildWhereByQOLString(qr,"($oql)")
    }

    private fun buildFunction(qr: QueryBuildContext, function: ExpFunction): String {
        return "${function.name}(" +
                function.arguments.joinToString {
                    buildWhereByExp(qr, it)
                } +
                ")"
    }

    private fun buildBinaryOperation(qr: QueryBuildContext, exp: BinaryOP): String {

        return when {
            exp.op == Operation.IN -> "${buildWhereByExp(qr, exp.left)} ${exp.op.value} (${buildWhereByExp(qr, exp.right)})"
            else -> {
                var leftSide = buildWhereByExp(qr, exp.left)
                leftSide = addCastForJsonFieldIfShould(qr, leftSide, exp.left, exp.right)

                "$leftSide ${exp.op.value} ${buildWhereByExp(qr, exp.right)}"
            }
        }

    }

    private fun buildFilterValue(qr: QueryBuildContext, exp: value): String {
        return ":${qr.addParam(exp.v)}"
    }

    private fun buildParamValue(exp: param): String {
        return ":" + exp.v.toString()
    }


    private fun addCastForJsonFieldIfShould(qr: QueryBuildContext, leftSide: String, left: exp, right: exp): String {


        fun leftSideIsJsonField(qr: QueryBuildContext, f: ExpressionField?): Boolean {
            return f?.name?.contains('.')?.let {
                val f1 = f.name.split('.')[0]
                qr.root.dm[f1].isJson()
            } ?: false
        }

        fun rightSideValue(f: Expression?): Any? {
            return when (f) {
                is ExpressionParam -> f.v
                is ExpressionValue -> f.v
                else -> null
            }
        }

        val paramValue = rightSideValue(right)
        if (paramValue == null || paramValue is String)
            return leftSide

        if (leftSideIsJsonField(qr, left as? ExpressionField?)) {
            return dbDialect.castSqlExpressionToType(leftSide, paramValue)
        }
        return leftSide
    }
}