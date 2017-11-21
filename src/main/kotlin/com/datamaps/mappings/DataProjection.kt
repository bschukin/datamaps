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
    //id объекта - возможно указание только для рутовых ОП
    var id:Long? = null
    //для вложенных проекций - родительское поле
    var field: String? = null
    //группы, которые включеные в проекцию
    var groups = mutableListOf<String>()
    //поля, включенные в проекцию - в вилей проекций
    var fields = linkedCaseInsMapOf<DataProjection>() //рекурсивные проекции

    //технические поля для чейнов по созданию проекций
    var last: DataProjection? = null
    var prev: DataProjection? = null

    constructor(entity: String) {
        this.entity = entity
    }

    constructor(entity: String, id:Long) {
        this.entity = entity
        this.id=  id;
    }

    constructor(entity: String?, field: String?) {
        this.field = field
        this.entity = entity
    }

    operator fun get(field: String): DataProjection? {
        return fields.get(field)
    }

    fun group(gr:String):DataProjection
    {
        groups.add(gr)
        return this
    }

    fun full():DataProjection
    {
        return group(FULL)
    }

    fun default():DataProjection
    {
        return group(DEFAULT)
    }

    fun refs():DataProjection
    {
        return group(REFS)
    }


    fun field(f:String):DataProjection
    {
        fields[f] = DataProjection(null, f)
        last  = fields[f]
        return this
    }

    fun id(id:Long):DataProjection
    {
        validate(isRoot())
        this.id = id
        return this
    }

    fun isRoot() = field==null


    fun inner():DataProjection {
        this.last!!.prev = this
        return this.last!!
    }

    fun end():DataProjection {
        return prev!!
    }

    fun filter(exp:exp):DataProjection
    {
        return this
    }

    fun filter( aaa:(m:Unit) -> exp):DataProjection
    {
        val exp = aaa(Unit)
        return this
    }

}


public open class exp() {


    infix fun or(exp: exp): exp {
        println("OR")
        return exp()
    }


    infix fun and(exp: exp): exp {
        println("and")
        return exp()
    }

    infix fun and(boolean: Boolean): exp {
        println("and boolean")
        return exp()
    }

    operator fun  compareTo(b:exp):Int
    {
        println("compareTo")
        return 0
    }

    operator fun  compareTo(b:Int):Int
    {
        println("compareToInt")
        return 0
    }

    infix fun gt(exp1: Any): exp {
        println("gt")
        return exp()
    }
}

public open class f(name:String):exp() {

}

public open class value(v:Any):exp() {

}


public class OR(left: exp, right: exp) : exp() {

}
public class AND(left: exp, right: exp) : exp() {

}