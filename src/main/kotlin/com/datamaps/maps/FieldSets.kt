package com.datamaps.maps

import com.datamaps.general.checkNIS
import java.util.*
import kotlin.concurrent.getOrSet

////ЭТО ФАЙЛ для всего что имеет отношение к фиелдсетам (маппингам)

/*Базовый класс маппингов (фиелдсетов). Чтобы не таскать женерики (см. MappingFieldSet)*/
//FieldSet - потому что маппинг описывается пользователем.
//DataMapping -
open class FieldSet {

}

open class MappingFieldSet<T : FieldSet> : FieldSet() {

    /**
     * Создать новый инстанс мапы для даннного фиелдсета и заполнить поля
     */
    fun create(): DataMap {
        val c = DataMap(this)
        return c
    }

    /**
     * Создать новый инстанс мапы для даннного фиелдсета и заполнить поля
     */
    fun create(body: T.(DataMap) -> Unit): DataMap {
        val c = DataMap(this)
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


    /**создать процекцию с where выражнием
     *
     */
    fun where(w: String) = on(this).where(w)


    fun onlyId(): DataProjection {
        return ProjectionBuilder.currProjection().onlyId()
    }

    fun withId(id: Any?): DataProjection {
        return ProjectionBuilder.currProjection().id(id)
    }

    fun full(): DataProjection {
        return ProjectionBuilder.currProjection().full()
    }

    fun alias(alias: String): DataProjection {
        return ProjectionBuilder.currProjection().alias(alias)
    }

    fun scalars(): DataProjection {
        return ProjectionBuilder.currProjection().scalars()
    }

    fun withRefs(): DataProjection {
        return ProjectionBuilder.currProjection().withRefs()
    }

    /**
     * Создать проекцию с фильтром
     */
    /*fun filter(e: (m: Unit) -> exp): DataProjection  {
        return when (ProjectionBuilder.hasProjectionUnderConstruction()) {
            true -> ProjectionBuilder.currProjection().filter(e)
            false -> on(this).filter(e)
        }
    }*/

    fun filter(builder: T.() -> exp): DataProjection {
        return when (ProjectionBuilder.hasProjectionUnderConstruction()) {
            true -> ProjectionBuilder.currProjection().filter(builder(this as T))
            false -> on(this).filter(builder(this as T))
        }
    }

    fun dice(builder: T.() -> Unit = {}): DataProjection {
        try {
            val dp = on(this)
            ProjectionBuilder.createBuilder(dp)
            builder(this as T)
            ProjectionBuilder.destroy()
            return dp
        } finally {
            ProjectionBuilder.destroy()
        }
    }

    fun <T1> slice(t: Field<T1, *>, builder: T1.() -> Unit = {}) {
        ProjectionBuilder.pushSlice(Slice(t.n))
        builder(t.t)
        val sl = ProjectionBuilder.popSlice()
        ProjectionBuilder.currProjection().with(sl)
    }

    class ProjectionBuilder {

        private val stack: Stack<DataProjection> = Stack()

        companion object {
            private val context = ThreadLocal<ProjectionBuilder>()

            fun createBuilder(projection: DataProjection) {
                checkNIS(context.get() == null)
                val builder = ProjectionBuilder()
                context.set(builder)
                builder.stack.push(projection)
            }

            fun destroy() {
                context.remove()
            }

            fun pushSlice(projection: DataProjection) {
                checkNIS(context.get() != null)
                context.get().stack.push(projection)
            }

            fun popSlice(): DataProjection {
                checkNIS(context.get() != null)
                return context.get().stack.pop()
            }

            fun hasProjectionUnderConstruction(): Boolean {
                return context.get()!=null
            }

            fun currProjection(): DataProjection {
                return context.get().stack.peek()
            }
        }
    }
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

    operator fun unaryPlus(): Field<T, L> {
        MappingFieldSet.ProjectionBuilder.currProjection().field(this)
        return this
    }

    operator fun unaryMinus(): f = f(this)

    operator fun not(): String = n

    operator fun invoke(builder: T.() -> Unit = {}) {
        MappingFieldSet.ProjectionBuilder.pushSlice(Slice(this.n))
        builder(this.t)
        val sl = MappingFieldSet.ProjectionBuilder.popSlice()
        MappingFieldSet.ProjectionBuilder.currProjection().with(sl)
    }


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
        val tt = if (t is Any) t else Object()
        return "Field('$_name', type='${tt::class.java.simpleName}')"
    }


}



