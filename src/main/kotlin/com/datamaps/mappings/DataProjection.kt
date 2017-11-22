package com.datamaps.mappings

import com.datamaps.general.validate
import com.datamaps.util.linkedCaseInsMapOf

/**
 * Created by Щукин on 07.11.2017.
 */

/**
 *
 * JiraWorker
 * {
 *    {{"default"}},
 *        "gender" {
 *              fields = "gender"
 *        }
 * }
 *
 */

class DataProjection {

    //для корневой проекции  - сущность по которой надо строить запрос
    //для вложенных поекций - опционально
    var entity: String? = null
    //алиас, использованный для данной сущности в запросе (например для использования в фильтрах)
    var queryAlias: String? = null
    //id объекта - возможно указание только для рутовых ОП
    var id: Long? = null
    //для вложенных проекций - родительское поле
    var field: String? = null
    //группы, которые включеные в проекцию
    var groups = mutableListOf<String>()
    //поля, включенные в проекцию - в вилей проекций
    var fields = linkedCaseInsMapOf<DataProjection>() //рекурсивные проекции

    //выражение  where
    private var filter: exp? = null

    //технические поля для чейнов по созданию проекций
    var last: DataProjection? = null
    var prev: DataProjection? = null

    constructor(entity: String) {
        this.entity = entity
    }

    constructor(entity: String, id: Long) {
        this.entity = entity
        this.id = id;
    }

    constructor(entity: String?, field: String?) {
        this.field = field
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

    fun full(): DataProjection {
        return group(FULL)
    }

    fun default(): DataProjection {
        return group(DEFAULT)
    }

    fun refs(): DataProjection {
        return group(REFS)
    }


    fun field(f: String): DataProjection {
        fields[f] = DataProjection(null, f)
        last = fields[f]
        return this
    }

    fun id(id: Long): DataProjection {
        validate(isRoot())
        this.id = id
        return this
    }

    fun isRoot() = field == null


    fun inner(): DataProjection {
        this.last!!.prev = this
        return this.last!!
    }

    fun end(): DataProjection {
        return prev!!
    }

    fun filter(): exp? {
        return filter
    }

    fun filter(exp: exp): DataProjection {
        filter =  exp
        return this
    }

    fun filter(aaa: (m: Unit) -> exp): DataProjection {
        filter  = aaa(Unit)
        return this
    }

}


public open class exp() {



    infix fun or(exp: exp): exp {
        return OR(this, exp)
    }


    infix fun and(exp: exp): exp {
        return AND(this, exp)
    }

    infix fun gt(exp1: Any): exp {
        return bop(exp1, ">")
    }

    infix fun eq(exp1: Any): exp {
        return bop(exp1, "=")
    }

    fun bop(exp1: Any, op:String): exp {
        val exp2 = if(exp1 !is exp) value(exp1) else exp1
        return binaryOP(this, exp2, op)
    }
}

data class binaryOP(var left: exp, var right: exp, var op:String) : exp() {

}


data class f(val name: String) : exp() {

}

data class value(val v: Any) : exp() {

}


data class OR(val  left: exp, val right: exp) : exp() {

}

data class AND(val left: exp, val right: exp) : exp() {

}