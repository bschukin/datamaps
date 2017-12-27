package com.datamaps.maps

import java.util.*
import kotlin.concurrent.getOrSet

////ЭТО ФАЙЛ для всего что имеет отношение к фиелдсетам
//заготовка для фиелдсетов. зачем она нужна - не очень понятно.
open class FieldSet {

}

typealias DM = FieldSet

//Фиелдсеты строятся на основе данного филда
data class Field<T, L>(private val _name: String, val t: T, val value: L) {



    companion object {

        fun id(): Field<Long, Long> {
            return long("id")
        }

        fun long(aname: String): Field<Long, Long> {
            return Field(aname, 350L, 350L)
        }

        fun int(aname: String): Field<Int, Int> {
            return Field(aname, 350, 350)
        }

        fun date(aname: String): Field<Date, Date> {
            return Field(aname, Date(), Date())
        }

        fun boolean(aname: String): Field<Boolean, Boolean> {
            return Field(aname, false, false)
        }

        fun string(aname: String): Field<String, String> {
            return Field(aname, "", "")
        }

        fun <T> reference(aname: String, t: T): Field<T, DataMap> {
            return Field(aname, t, DataMap.empty())
        }

        fun <T> list(aname: String, t: T): Field<T, MutableList<DataMap>> {
            return Field(aname, t, DataMap.emptyList())
        }

        internal var context = ThreadLocal<MutableList<String>>()

    }


    private fun name() {
        context.getOrSet { mutableListOf() }.add(_name)
    }

    operator fun get(index: Int): T {
        context.getOrSet { mutableListOf() }.add("$_name[$index]")
        return t
    }

    val n: String
        get() {
            if (context.get() == null)
                return _name
            name()
            val res = context.get().joinToString(".")
            context.remove()
            return res
        }

    operator fun unaryPlus(): String = n

    operator fun unaryMinus(): f = f(this)

    val nl: List<String>
        get() {
            if (context.get() == null)
                return listOf(_name)
            name()
            val res = context.get()
            context.remove()
            return res
        }

    operator fun invoke(): T {
        name()
        return t
    }

    override fun toString(): String {
        val tt = if(t is Any) t else Object()
        return "Field('$_name', type='${tt::class.java.simpleName}')"
    }


}

