package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.misc.throwNotImplementedYet
import org.springframework.stereotype.Service

/**
 * Created by b.schukin on 21.11.2017.
 */
@Service
internal open class QueryTextFilterBuilder {


    fun buildTextFilterWhere(qr: QueryBuildContext) {

        val querylevel = qr.root
        val projection = querylevel.dp
        val table = querylevel.dm.table
        //пока мы строим только запрос по ID

        projection.textFilter?.let {
            if (!qr.where.isBlank())
                qr.where += " AND "

            qr.where +=
                    "zdb('$table', ${qr.rootAlias}.ctid) ==> " +
                    "'${buildTextFilterWhereByExp(it)}'"
        }

        projection.textFilterString?.let {
            if (!qr.where.isBlank())
                qr.where += " AND "

            qr.where +=
                    "zdb('$table', ${qr.rootAlias}.ctid) ==> '$it'"
        }
    }

    private fun buildTextFilterWhereByExp(exp: exp): String {
        return when (exp) {
            is f -> exp.name
            is value -> "\"${exp.v}\""
            is BinaryOP -> "${buildTextFilterWhereByExp(exp.left)} ${exp.op.value} ${buildTextFilterWhereByExp(exp.right)}"
            is OR -> "(${buildTextFilterWhereByExp(exp.left)} OR ${buildTextFilterWhereByExp(exp.right)})"
            is AND -> "(${buildTextFilterWhereByExp(exp.left)} AND ${buildTextFilterWhereByExp(exp.right)})"
            is NOT -> "!${buildTextFilterWhereByExp(exp.right)}"
            else -> throwNotImplementedYet()
        }
    }


}