package com.datamaps.mappings

import com.datamaps.general.validate
import com.datamaps.util.lcims
import com.datamaps.util.linkedCaseInsMapOf

/**
 * Created by Щукин on 07.11.2017.
 */

/**
 *
 * JiraWorker
 * {
 *    {{"scalars"}},
 *        "gender" {
 *              fields = "gender"
 *        }
 * }
 *
 */

open class DataProjection {

    //для корневой проекции  - сущность по которой надо строить запрос
    //для вложенных поекций - опционально
    var entity: String? = null
    //алиас, использованный для данной сущности в запросе (например для использования в фильтрах)
    var queryAlias: String? = null
    //id объекта - возможно указание только для рутовых ОП
    var id: Long? = null
    //для вложенных проекций - родительское поле
    private var parentField: String? = null
    //группы, которые включеные в проекцию
    var groups = mutableListOf<String>()
    //поля, включенные в проекцию - в вилей проекций
    var fields = linkedCaseInsMapOf<DataProjection>() //рекурсивные проекции

    //вычислимые поля (формулы)
    var formulas = linkedCaseInsMapOf<String>()

    //вычислимые поля, основанные на латеральных джойнах
    var laterals = mutableListOf<Lateral>()

    var params = mutableMapOf<String, Any?>()

    //выражение  where
    private var filter: exp? = null

    //альтернативный вариант where - "OQL"
    private var oql: String? = null

    //выражение  where
    private val orders = mutableListOf<ExpressionField>()

    var limit: Int? = null
    var offset: Int? = null

    constructor()

    constructor(entity: String) {
        this.entity = entity
    }

    constructor(entity: String, id: Long) {
        this.entity = entity
        this.id = id
    }

    constructor(entity: String?, field: String?) {
        this.parentField = field
        this.entity = entity
    }

    operator fun get(field: String): DataProjection? {
        return fields.get(field)
    }

    fun alias(alias: String): DataProjection {
        queryAlias = alias
        return this
    }

    fun group(gr: String): DataProjection {
        groups.add(gr)
        return this
    }


    fun onlyId(): DataProjection {
        return field("id")
    }
    fun full(): DataProjection {
        return group(FULL)
    }

    fun scalars(): DataProjection {
        return group(DEFAULT)
    }


    fun withRefs(): DataProjection {
        return group(REFS)
    }

    fun withCollections(): DataProjection {
        return group(LIST)
    }


    fun field(f: String): DataProjection {
        fields[f] = slice(f)
        return this
    }


    fun with(slice: () -> DataProjection): DataProjection {
        val sl = slice()
        fields[sl.parentField] = sl
        return this
    }


    fun id(id: Long): DataProjection {
        validate(isRoot())
        this.id = id
        return this
    }

    fun formula(name: String, formula: String): DataProjection {
        formulas[name] = formula
        return this
    }

    fun lateral(table:String, sql: String, vararg pairs: Pair<String, String>): DataProjection
    {
        val lmap = linkedCaseInsMapOf<String>()
        pairs.forEach { pair->
            lmap[pair.second] = pair.first
        }
        laterals.add(Lateral(table, sql, linkedCaseInsMapOf(*pairs)))
        return this
    }

    fun isLateral(alias:String):Boolean
    {
        return laterals.any { l->l.table==alias }
    }

    fun isRoot() = parentField == null

    fun where(): String? {
        return this.oql
    }

    fun where(oql:String): DataProjection {
        this.oql = oql
        return this
    }

    fun filter(): exp? {
        return filter
    }

    fun filter(exp: exp): DataProjection {
        filter = if (filter == null)  exp else (filter!! and exp)
        return this
    }

    fun filter(aaa: (m: Unit) -> exp): DataProjection {
        val exp = aaa(Unit)
        filter = if (filter == null)  exp else (filter!! and exp)
        return this
    }

    fun order(vararg fields:f): DataProjection {
        orders.addAll(fields)
        return this
    }

    fun limit(l: Int): DataProjection {
        limit = l
        return this
    }

    fun offset(l: Int): DataProjection {
        offset = l
        return this
    }

    fun orders() = orders

    fun param(k: String, v: Any?): DataProjection {
        this.params[k] = v
        return this
    }
}

data class Lateral(val table: String, val sql:String, val mappings: lcims)


class slice(f:String):DataProjection(null, f)

open class exp {

    infix fun or(exp: exp): exp {
        return OR(this, exp)
    }

    infix fun or(righta: (m: Unit) -> exp): exp {
        return OR(this, righta(Unit))
    }


    infix fun and(exp: exp): exp {
        return AND(this, exp)
    }

    infix fun and(righta: (m: Unit) -> exp): exp {
        return AND(this, righta(Unit))
    }

    infix fun gt(exp1: Any): exp {
        return bop(exp1, Operation.gt)
    }

    infix fun ge(exp1: Any): exp {
        return bop(exp1, Operation.ge)
    }

    infix fun le(exp1: Any): exp {
        return bop(exp1, Operation.le)
    }

    infix fun lt(exp1: Any): exp {
        return bop(exp1, Operation.lt)
    }

    infix fun eq(exp1: Any): exp {
        return bop(exp1, Operation.eq)
    }

    infix fun like(exp1: Any): exp {
        return bop(exp1, Operation.like)
    }

    infix fun IS(nul: NULL): exp {
        return bop(nul, Operation.isnull)
    }

    infix fun ISNOT(nul: NULL): exp {
        return bop(nul, Operation.isnotnull)
    }

    infix fun IN(list: List<*>): exp {
        return bop(list, Operation.inn)
    }


    private fun bop(exp1: Any, op: Operation): exp {
        val exp2 = if (exp1 !is exp) value(exp1) else exp1
        return binaryOP(this, exp2, op)
    }
}

data class binaryOP(var left: exp, var right: exp, var op: Operation) : exp()





data class f(val name: String) : exp() {

    var asc = true

    fun asc(): f {
        asc = true
        return this
    }
    fun desc(): f {
        asc = false
        return this
    }
}

typealias ExpressionField = f

data class value(val v: Any) : exp() {

}


data class OR(val left: exp, val right: exp) : exp() {

}

data class NOT(val right: exp) : exp() {

}

data class AND(val left: exp, val right: exp) : exp()

class NULL : exp() {

}

infix fun (() -> exp).and(function: () -> exp): exp {
    return AND(this(), function())
}

infix fun (() -> exp).and(exp: exp): exp {
    return AND(this(), exp)
}

infix fun (() -> exp).or(function: () -> exp): exp {
    return OR(this(), function())
}

infix fun (() -> exp).or(exp: exp): exp {
    return OR(this(), exp)
}

fun not(function: () -> exp): exp {
    return NOT(function())
}

fun not(exp: exp): exp {
    return NOT(exp)
}


typealias expLamda = (m: Unit) -> exp
typealias projection = DataProjection

enum class Operation(val value: String) {
    eq("="),
    gt(">"),
    ge(">="),
    lt("<"),
    le("<="),
    like("like"),
    isnull("is"),
    isnotnull("is not"),
    inn("in")
}