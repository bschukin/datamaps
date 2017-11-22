package com.datamaps.services

import com.datamaps.general.throwNIS
import com.datamaps.general.validateNIY
import com.datamaps.mappings.*
import org.springframework.stereotype.Service

/**
 * Created by b.schukin on 21.11.2017.
 */
@Service
class FilterBuilder {


    fun buildWhere(qr: QueryBuildContext) {

        val querylevel = qr.stack.peek()
        val projection = querylevel.dp

        //пока мы строим только запрос по ID
        projection.id?.let {
            validateNIY(projection.isRoot())

            qr.where = "${querylevel.alias}.${querylevel.dm.idColumn} = :_id1"
            qr.params["_id1"] = projection.id
        }

        projection.filter()?.let{
            qr.where  = buildWhereByExp(qr, it)
        }

    }

    private fun buildWhereByExp(qr: QueryBuildContext, exp: exp):String {
        return when(exp)
        {
            is f ->buildFilterProperty(qr, exp)
            is value ->buildFilterValue(qr, exp)
            is binaryOP-> "${buildWhereByExp(qr,exp.left)} ${exp.op} ${buildWhereByExp(qr,exp.right)}"
            is OR ->TODO()
            is AND ->"(${buildWhereByExp(qr,exp.left)} AND ${buildWhereByExp(qr,exp.right)})"
            else -> throwNIS()
        }
    }

    private fun buildFilterValue(qr: QueryBuildContext, exp: value): String {
        return qr.addParam(exp.v)
    }


    //нам надо для филттруемой проперти - выстроить путь к ней (если он не был построен)
    // (если колонка например не участвует в селекте)
    //и получить алиас колонки
    //NB: при создании цепочек свойств мы считаем что свойства пишутся от корневой сущности (или от указанного пользователем алиаса)
    //например: JiraStaffUnit --> Worker-->Gender-->gender будут писаться в фильтре как worker.gender.gender (рута нет)
    private fun buildFilterProperty(qr: QueryBuildContext, exp: f):String {
        var alias = qr.rootAlias
        var currLevel = qr.stack.peek()
        var list = exp.name.split('.')

        for (i in 0 until list.size - 1) {
            alias = qr.getAliasByPathFromParent(alias, list[i])!!
            currLevel = currLevel.childProps[list[i]]
        }
        return qr.getColumnIdentiferForFillter(alias, currLevel.dm.get(list.last()).sqlcolumn!!)
    }

}