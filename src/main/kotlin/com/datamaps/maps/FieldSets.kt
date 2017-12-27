package com.datamaps.maps

import java.util.*
import kotlin.concurrent.getOrSet

////ЭТО ФАЙЛ для всего что имеет отношение к фиелдсетам (маппингам)

/*Базовый класс маппингов (фиелдсетов). Чтобы не таскать женерики (см. MappingFieldSet)*/
//FieldSet - потому что маппинг описывается пользователем.
//DataMapping -
open class FieldSet {

}

open class MappingFieldSet<T: FieldSet>: FieldSet() {

    /**
     * Создать новый инстанс мапы для даннного фиелдсета и заполнить поля
     */
    fun create(): DataMap {
        val c =  DataMap(this)
        return c
    }

    /**
     * Создать новый инстанс мапы для даннного фиелдсета и заполнить поля
     */
    fun create(body: T.(DataMap) -> Unit): DataMap {
        val c =  DataMap(this)
        body(this as T, c)
        return c
    }

    /**
     * Обновить данный инстанс мапы для даннного фиелдсета и заполнить поля
     */
    fun update(dataMap: DataMap, body: T.(DataMap) -> Unit): DataMap {
        body(this as T, dataMap)
        return dataMap
    }

    /***
     * Создать проекцию на
     */
    fun on() = on(this)

    /**
     * Создать проекцию с фильтром
     */
    fun filter(e: (m: Unit) -> exp) = on(this).filter(e)


    /**создать процекцию с where выражнием
     *
     */
    fun where(w: String) = on(this).where(w)



}
typealias MFS<T> = MappingFieldSet<T>


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

