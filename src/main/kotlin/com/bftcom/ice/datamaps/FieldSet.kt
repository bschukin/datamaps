@file:Suppress("UNCHECKED_CAST")

package com.bftcom.ice.datamaps

import com.bftcom.ice.datamaps.misc.FieldSetRepo
import com.bftcom.ice.datamaps.common.utils.*
import com.bftcom.ice.datamaps.core.delta.DeltaStore
import com.bftcom.ice.datamaps.misc.*
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.jvm.isAccessible


////ЭТО ФАЙЛ для всего что имеет отношение к фиелдсетам (маппингам)

open class FieldSetGroup(var name: String, var caption: String, var group: FieldSetGroup? = null) {

    override fun equals(other: Any?): Boolean {
        return this === other
                || other is FieldSetGroup && name == other.name && caption == other.caption && group == other.group
    }

    override fun hashCode() = name.hashCode()
}

/*Базовый класс маппингов (фиелдсетов). Чтобы не таскать женерики (см. MappingFieldSet)*/
//FieldSet - потому что маппинг описывается пользователем.
//DataMapping -
abstract class FieldSet(private var _entity: String = "", var caption: String? = null,
                        var fieldSetGroup: FieldSetGroup? = null,
                        vararg options: FieldSetOption) {

    val options: MutableList<FieldSetOption> = mutableListOf()
    var table: String? = null

    init {
        if(_entity == "")
            _entity = this::class.simpleName!!
        registerFieldSet()
        this.options.addAll(options)
    }

    val entity:String get() = _entity

    private fun registerFieldSet() {
        FieldSetRepo.registerFieldSet(this)
    }

    //нативный ключ
    open val nativeKey: List<Field<*, *>> by lazy {
        emptyList<Field<*, *>>()
    }

    open fun addOption(option: FieldSetOption) {
        options.add(option)
    }

    open fun containsOption(kclass: kotlin.reflect.KClass<*>): Boolean {
        return options.firstOrNull { kclass.isInstance(it) } != null
    }

    open fun <T : FieldSetOption> getOption(kclass: kotlin.reflect.KClass<T>): T? {
        return options.firstOrNull { kclass.isInstance(it) } as T?
    }

    fun isDynamic() = containsOption(Dynamic::class) || entity == DataMapF.DYNAMIC_ENTITY
    fun isTransient() = containsOption(Transience::class)
}

fun FieldSet.getField(property: String): Field<*, *>? {
    val obj = this::class.objectInstance
    val res = this::class.declaredMembers.find { it.name.equals(property, true) }
    if (res != null)
        return res.call(obj) as? Field<*, *>
    return null
}

fun MappingFieldSet<*>.getReferencedFieldSet(property: String): MFS<*>? {
    return getField(property)?.refFieldSet()
}


fun FieldSet.getAllFields(): List<Field<*, *>> {
    val obj = this::class.objectInstance
    return this::class.members.filterIsInstance(KProperty::class.java)
            .map {
                it.isAccessible = true
                it.call(obj)
            }
            .filter { it is Field<*, *> }.map { it as Field<*, *> }.toList()
}

open class MappingFieldSet<T : FieldSet>(entity: String = "",
                                                                 caption: String? = null,
                                                                 group: FieldSetGroup? = null,
                                                                 vararg options: FieldSetOption)
    : FieldSet(entity, caption, group, *options) {

    constructor(entity: String, vararg options: FieldSetOption)
            : this(entity, null, null, options=*options)

    constructor(vararg options: FieldSetOption)
            : this("", caption = null, group = null,  options=*options)

    constructor(entity: String, caption: String, vararg options: FieldSetOption)
            : this(entity, caption, null, *options)

    protected open fun applyDefaultValues(c: DataMap) {
        val fields = FieldSetRepo.getStaticFields(c.fieldSet!!)

        //todo: прокешировать

        val defaultValueFields = fields.values.filter { it.defaultValue != null }
        //заставляем ID выполниться первым
        defaultValueFields
                .filter { it.fieldName.equals(DataMapF.ID, true) }
                .union(
                        defaultValueFields.filter { !it.fieldName.equals(DataMapF.ID, true) }
                )
                .forEach {
                    c[it.n] = it.defaultValue!!.invoke(c)
                }
    }

    /**
     * Создать новый инстанс мапы для даннного фиелдсета и заполнить поля
     */
    fun create(): DataMapF<T> {
        val c = DataMapF(this as T, isNew = true)
        applyDefaultValues(c)
        return c
    }

    /**
     * Создать новый инстанс мапы для даннного фиелдсета и заполнить поля
     */
    fun create(body: T.(DataMapF<T>) -> Unit): DataMapF<T> {
        val c = DataMapF(this as T, isNew = true)
        applyDefaultValues(c)
        body(this as T, c)
        return c
    }

    /*еще create*/
    operator fun invoke(builder: T.(DataMapF<T>) -> Unit): DataMapF<T> {
        return create(body = builder)
    }

    /**
     * Создать новый инстанс мапы для даннного фиелдсета и заполнить поля
     * TODO: понять почему я сюда поставил existing?
     */
    fun existing(id: Any? = null, body: T.(DataMapF<T>) -> Unit): DataMapF<T> {
        val c = DataMapF(this as T, isNew = false, id = id)
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

    /**
     * Создать новый инстанс мапы для даннного фиелдсета и заполнить поля
     */
    fun createInSilence(isNew: Boolean = true, body: T.(DataMapF<T>) -> Unit): DataMapF<T> {
        DeltaStore.silence()

        try {
            val c = DataMapF(this as T, isNew = isNew)
            applyDefaultValues(c)
            body(this as T, c)
            return c
        } finally {
            DeltaStore.noise()
        }
    }

    /**
     * Обновить данный инстанс мапы для даннного фиелдсета и заполнить поля
     */
    fun updateInSilence(dataMap: DataMap, body: T.(DataMap) -> Unit): DataMap {
        DeltaStore.silence()
        try {
            body(this as T, dataMap)
            return dataMap
        } finally {
            DeltaStore.noise()
        }
    }


    /***
     * Создать проекцию на
     */
    open fun on(): DataProjectionF<T> = DataProjectionF<T>(this.entity).apply { fieldSet = this@MappingFieldSet as T? }


    /**создать процекцию с where выражнием
     *
     */
    //fun where(w: String) = DataProjectionF(this as T).where(w)


    fun onlyId(): DataProjection {
        return ProjectionBuilder.currProjection().onlyId()
    }

    fun withId(id: Any?): DataProjectionF<T> {
        return when (ProjectionBuilder.hasProjectionUnderConstruction()) {
            true -> ProjectionBuilder.currProjection().id(id) as DataProjectionF<T>
            false -> on(this).id(id) as DataProjectionF<T>
        }
    }

    fun onDate(date: Date): DataProjectionF<T> {
        return when (ProjectionBuilder.hasProjectionUnderConstruction()) {
            true -> ProjectionBuilder.currProjection().onDate(date) as DataProjectionF<T>
            false -> on(this).onDate(date) as DataProjectionF<T>
        }
    }

    fun withJoinFilter(f: Expression): DataProjectionF<T> {
        return ProjectionBuilder.currProjection().withJoinFilter(f) as DataProjectionF<T>
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

    fun withRefs(): DataProjectionF<T> {
        return when (ProjectionBuilder.hasProjectionUnderConstruction()) {
            true -> ProjectionBuilder.currProjection().withRefs()
            false -> on(this).withRefs()
        } as DataProjectionF<T>
    }

    fun withCollections(): DataProjectionF<T> {
        return when (ProjectionBuilder.hasProjectionUnderConstruction()) {
            true -> ProjectionBuilder.currProjection().withCollections()
            false -> on(this).withCollections()
        } as DataProjectionF<T>
    }

    fun withBlobs(): DataProjectionF<T> {
        return when (ProjectionBuilder.hasProjectionUnderConstruction()) {
            true -> ProjectionBuilder.currProjection().withBlobs()
            false -> on(this).withBlobs()
        } as DataProjectionF<T>
    }

    fun withFormulas(): DataProjectionF<T> {
        return when (ProjectionBuilder.hasProjectionUnderConstruction()) {
            true -> ProjectionBuilder.currProjection().withFormulas()
            false -> on(this).withFormulas()
        } as DataProjectionF<T>
    }

    fun field(field: String): DataProjection {
        return ProjectionBuilder.currProjection().field(field)
    }

    fun filter(builder: T.() -> exp): DataProjectionF<T> {
        return when (ProjectionBuilder.hasProjectionUnderConstruction()) {
            true -> currProjection().filter(builder(this as T))
            false -> on(this).filter(builder(this as T)) as DataProjectionF<T>
        }
    }

    fun orders(builder: T.() -> List<Field<*, *>>): DataProjectionF<T> {
      return  currProjection()
              .order( * builder(this as T).toTypedArray())
    }

    fun order(builder: T.() -> Field<*, *>): DataProjectionF<T> {
        return  currProjection()
                .order(  builder(this as T))
    }

    fun where(sql:String): DataProjectionF<T> {
        return when (ProjectionBuilder.hasProjectionUnderConstruction()) {
            true -> currProjection().where(sql)
            false -> on(this).where(sql) as DataProjectionF<T>
        }
    }

    fun textFilter(exp: exp?): DataProjectionF<T> {
        return when (ProjectionBuilder.hasProjectionUnderConstruction()) {
            true -> currProjection().filter(exp)
            false -> on(this).filter(exp) as DataProjectionF<T>
        }
    }

    fun option(): ProjectionOptions {
        return ProjectionBuilder.currProjection().option()
    }

    fun slice(parentProjection: DataProjection, builder: T.() -> Unit = {}): DataProjectionF<T> {
        return sliceWithProjection(parentProjection, builder)
    }

    fun slice(builder: T.() -> Unit = {}): DataProjectionF<T> {
        return sliceWithProjection(null, builder)
    }

    fun slice(entity: String, builder: T.() -> Unit = {}): DataProjection {
        try {
            val dp = on(entity)
            ProjectionBuilder.createBuilder(dp)
            builder(this as T)
            ProjectionBuilder.destroy()
            return dp
        } finally {
            ProjectionBuilder.destroy()
        }
    }

    fun sum(f: Field<*, *>, alias:String = "sum_${f.n}") {
        currProjection().formula(alias, "sum({{${f.n}}})")
    }

    fun count(f: Field<*, *>, alias:String = "count_${f.n}") {
        currProjection().formula(alias, "count({{${f.n}}})")
    }

    private fun currProjection() = (ProjectionBuilder.currProjection() as DataProjectionF<T>)

    fun max(f: Field<*, *>, alias:String = "max_${f.n}") {
        currProjection().formula(alias, "max({{${f.n}}})")
    }


    fun min(f: Field<*, *>, alias:String = "min_${f.n}") {
        currProjection().formula(alias, "min({{${f.n}}})")
    }

    fun avg(f: Field<*, *>, alias:String = "avg_${f.n}") {
        currProjection().formula(alias, "avg({{${f.n}}})")
    }

    fun withSlice(field: String, builder: T.() -> Unit = {}): DataProjection {
        try {
            val dp = field(field)[field]!!
            ProjectionBuilder.pushSlice(dp)
            builder(this as T)
            return dp
        } finally {
            ProjectionBuilder.popSlice()
        }
    }

    class ProjectionBuilder {

        private val stack = mutableListOf<DataProjection>()

        companion object {
            private val context = ThreadLocalFactory.buildThreadLocal<ProjectionBuilder>()

            fun createBuilder(projection: DataProjection) {
                makeSure(context.get() == null)
                val builder = ProjectionBuilder()
                context.set(builder)
                builder.stack.add(projection)
            }

            fun destroy() {
                context.remove()
            }

            fun pushSlice(projection: DataProjection) {
                makeSure(context.get() != null)
                context.get()!!.stack.add(projection)
            }

            fun popSlice(): DataProjection {
                makeSure(context.get() != null)
                return context.get()!!.stack.removeAt(context.get()!!.stack.lastIndex)
            }

            fun hasProjectionUnderConstruction(): Boolean {
                return context.get() != null
            }

            fun currProjection(): DataProjection {
                return context.get()!!.stack.get(context.get()!!.stack.lastIndex)
            }
        }
    }

    private fun sliceWithProjection(parentProjection: DataProjection? = null, builder: T.() -> Unit = {}): DataProjectionF<T> {
        val flag = ProjectionBuilder.hasProjectionUnderConstruction()
        try {
            val dp = parentProjection?: on()

            if (flag)
                ProjectionBuilder.pushSlice(dp)
            else
                ProjectionBuilder.createBuilder(dp)

            builder(this as T)

            return dp as DataProjectionF<T>
        } finally {
            if (flag)
                ProjectionBuilder.popSlice()
            else
                ProjectionBuilder.destroy()
        }
    }
}

typealias MFS<T> = MappingFieldSet<T>

interface ValuedEnum<V> {
    val value: V
    val displayName: String get() = value.toString()
}

interface StringEnum : ValuedEnum<String> {
    override val value: String
        get() = (this as Enum<*>).name
}

class SimpleValuedEnum<V>(override val value: V, override val displayName: String = value.toString()) : ValuedEnum<V>

//Фиелдсеты строятся на основе данного филда
open class Field<T, L> private constructor(

        //наименование поля.
        // почему не name?
        //чтобы во вложенгых конструкциях вида SomeObject.someProp1().someProp2().name
        //было понятно что имеется ввиду - для последнего поля - это оно называется name? или мы обращаемся к
        //[наименование длинного поля  [SomeObject.someProp1().someProp2().name]]
        val fieldName: String,

        //типы поля. У нас их два
        //для элементарных типов они совпадают
        //для сложных:
        // если ссылка(M-1): targetTypeValue - тип объекта на котороый ссслыемся, containerTypeValue - DataMap
        // если список(1-N): targetTypeValue - тип объекта на котороый ссслыемся, containerTypeValue - List<DataMap>
        private val targetTypeValue: T,
        private val containerTypeValue: L,
        //опции поля (для UI например
        var caption: String = fieldName,
        var tooltip: String? = null,
        var required: Boolean = false,
        var requiredMessage: String? = null,
        var length: Int? = null,
        var pattern: Regex? = null,
        var patternExplanation: String? = null,
        var minLength: Int? = null,
        var maxLength: Int? = null,
        var defaultValue: ((DataMap) -> L)? = null,
        var values: List<L>? = null,
        var min: Comparable<*>? = null,
        var max: Comparable<*>? = null,
        var scale: Int? = null,
        var parser: (String) -> L = { throw NotImplementedError() },
        var formatter: (L) -> String = { it.toString() },
        var validator: ((L) -> Unit)? = null,

        //опции поля для маппингов //возмможно вынести в отдельные классы аля MappingOptions
        var thatJoinColumn: String? = null, //для ремаппинга списков

        //
        var thisJoinColumn: String? = null, //для ремаппинга референсов многий-к-одному

        //ссылка на сущность (для ссылочных и коллекционных полей)
        var referencedEntity: String? = null,

        //тип поля(для генерации полей)
        var fieldType: FieldType? = null,

        //опции поля для работы диффсервиса
        var prevFieldName: String? = null,

        //синтетические поля - поля с собственым маппингом.
        //исключаются при работе алгоримтов сранвения маппингла в филедсете и в бд
        //+ (таким образом позволяют заммаппить одну и тоже колонку БД на разные поля)
        var synthetic: Boolean = false,

        var oqlFormula: String? = null) {

    ///статические методы - конструкторы полей.
    companion object {

        fun id(): Field<Long, Long> {
            return longNN("id")
        }

        fun stringId(): Field<String, String> {
            return stringNN("id")
        }

        fun guid(name: String = "id", handler: FieldHandler<GUID>? = null): Field<GUID, GUID> {
            return Field(name, GUID.constGUID, GUID.constGUID, parser = { GUID(it) },
                    caption = "Идентификатор", required = true,
                    defaultValue = { DeltaStore.newGUID() },
                    fieldType = FieldType.Guid)
                    .let { field -> handler?.invoke(field); field }
        }


        fun long(aname: String, handler: FieldHandler<Long?>? = null): Field<Long?, Long?> {
            return Field<Long?, Long?>(aname, 0L, 0L,
                    parser = String::toLong, fieldType = FieldType.Long)
                    .let { field -> handler?.invoke(field); field }
        }

        fun longNN(aname: String, handler: FieldHandler<Long>? = null): Field<Long, Long> {
            return Field(aname, 0L, 0L,
                    required = true, parser = String::toLong, fieldType = FieldType.Long)
                    .let { field -> handler?.invoke(field); field }
        }

        fun int(aname: String, handler: FieldHandler<Int?>? = null): Field<Int?, Int?> {
            return Field<Int?, Int?>(aname, 0, 0,
                    parser = String::toInt, fieldType = FieldType.Int)
                    .let { field -> handler?.invoke(field); field }
        }

        fun intNN(aname: String, handler: FieldHandler<Int>? = null): Field<Int, Int> {
            return Field(aname, 0, 0,
                    required = true, parser = String::toInt, fieldType = FieldType.Long)
                    .let { field -> handler?.invoke(field); field }
        }

        fun double(aname: String, handler: FieldHandler<Double?>? = null): Field<Double?, Double?> {
            return Field<Double?, Double?>(aname, 0.0, 0.0,
                    parser = String::toDouble, fieldType = FieldType.Double)
                    .let { field -> handler?.invoke(field); field }
        }

        fun doubleNN(aname: String, handler: FieldHandler<Double>? = null): Field<Double, Double> {
            return Field(aname, 0.0, 0.0,
                    required = true, parser = String::toDouble, fieldType = FieldType.Double)
                    .let { field -> handler?.invoke(field); field }
        }

        fun date(aname: String, handler: FieldHandler<Date?>? = null): Field<Date?, Date?> {
            return Field<Date?, Date?>(aname, Date(), Date(),
                    parser = String::toDate, formatter = { it!!.toReadableDateString() },
                    fieldType = FieldType.Date)
                    .let { field -> handler?.invoke(field); field }
        }

        fun dateNN(aname: String, handler: FieldHandler<Date>? = null): Field<Date, Date> {
            return Field(aname, Date(), Date(),
                    parser = String::toDate, formatter = Date::toReadableDateString,
                    required = true, fieldType = FieldType.Date)
                    .let { field -> handler?.invoke(field); field }
        }

        fun timestamp(aname: String, handler: FieldHandler<Timestamp?>? = null): Field<Timestamp?, Timestamp?> {
            return Field<Timestamp?, Timestamp?>(aname, Timestamp(), Timestamp(),
                    parser = String::toTimeStamp, formatter = { it!!.toReadableTimeStampString() },
                    fieldType = FieldType.Timestamp)
                    .let { field -> handler?.invoke(field); field }
        }

        fun timestampNN(aname: String, handler: FieldHandler<Timestamp>? = null): Field<Timestamp, Timestamp> {
            return Field(aname, Timestamp(), Timestamp(), parser = String::toTimeStamp,
                    formatter = Timestamp::toReadableTimeStampString,
                    required = true, fieldType = FieldType.Timestamp)
                    .let { field -> handler?.invoke(field); field }
        }


        fun boolean(aname: String, handler: FieldHandler<Boolean?>? = null): Field<Boolean?, Boolean?> {
            return Field<Boolean?, Boolean?>(aname, false, false,
                    parser = String::toBoolean, fieldType = FieldType.Bool)
                    .let { field -> handler?.invoke(field); field }
        }

        fun booleanNN(aname: String, handler: FieldHandler<Boolean>? = null): Field<Boolean, Boolean> {
            return Field(aname, false, false,
                    parser = String::toBoolean, required = true, fieldType = FieldType.Bool)
                    .let { field -> handler?.invoke(field); field }
        }

        fun string(aname: String, handler: FieldHandler<String?>? = null): Field<String?, String?> {
            return Field<String?, String?>(aname, ""
                    , "", parser = { it }, fieldType = FieldType.String)
                    .let { field -> handler?.invoke(field); field }
        }

        fun stringNN(aname: String, handler: FieldHandler<String>? = null): Field<String, String> {
            return Field(aname, "", "", parser = { it },
                    required = true, fieldType = FieldType.String)
                    .let { field -> handler?.invoke(field); field }
        }

        fun <T : FieldSet> reference(aname: String, t: T, handler: FieldHandler<DataMapF<T>?>? = null)
                : Field<T, DataMapF<T>?> {
            return Field<T, DataMapF<T>?>(aname, t, DataMapF.empty() as DataMapF<T>, fieldType = FieldType.Long)
                    .let { field -> handler?.invoke(field); field }
        }

        fun <T : FieldSet> referenceNN(aname: String, t: T, handler: FieldHandler<DataMapF<T>>? = null): Field<T, DataMapF<T>> {
            return Field(aname, t, DataMapF.empty() as DataMapF<T>, required = true, fieldType = FieldType.Long)
                    .let { field -> handler?.invoke(field); field }
        }

        fun reference(aname: String, nameOfReferencedEntity: String,
                      handler: FieldHandler<DataMap>? = null): Field<SomeFieldSet, DataMap> {
            return Field(aname, SomeFieldSet, DataMapF.empty(), fieldType = FieldType.Long)
                    .apply { this.referencedEntity = nameOfReferencedEntity }
                    .let { field -> handler?.invoke(field); field }
        }

        fun <T : ValuedEnum<String>> enum(aname: String, values: Array<T>, handler: FieldHandler<T?>? = null): Field<T?, T?> {
            return Field<T?, T?>(aname, values[0], values[0],
                    parser = { value -> values.first { it.value == value } },
                    formatter = { it!!.value },
                    fieldType = FieldType.String,
                    values = values.toList())
                    .let { field -> handler?.invoke(field);field }
        }

        fun <T : ValuedEnum<String>> enumNN(aname: String, values: Array<T>, handler: FieldHandler<T>? = null): Field<T, T> {
            return Field(aname, values[0], values[0],
                    parser = { value -> values.first { it.value == value } },
                    formatter = { it.value },
                    fieldType = FieldType.String,
                    values = values.toList(),
                    required = true)
                    .let { field -> handler?.invoke(field); field }
        }

        fun <T : FieldSet> list(aname: String, t: T, handler: FieldHandler<MutableList<DataMapF<T>>>?
        = null): Field<T, MutableList<DataMapF<T>>> {
            return Field(aname, t, DataMapF.emptyList() as MutableList<DataMapF<T>>,
                    fieldType = FieldType.Other)
                    .let { field -> handler?.invoke(field); field }
        }

        fun list(aname: String, nameOfReferencedEntity: String,
                 handler: FieldHandler<MutableList<DataMap>>? = null): Field<SomeFieldSet, MutableList<DataMap>> {
            return Field(aname, SomeFieldSet, DataMapF.emptyList(), fieldType = FieldType.Other)
                    .apply { this.referencedEntity = nameOfReferencedEntity }
                    .let { field -> handler?.invoke(field); field }

        }

        fun list(aname: String, handler: FieldHandler<MutableList<DataMap>>? = null): Field<SomeFieldSet, MutableList<DataMap>> {
            return Field(aname, SomeFieldSet, DataMapF.emptyList(), fieldType = FieldType.Other).let { field -> handler?.invoke(field); field }
        }

        fun clob(aname: String, handler: FieldHandler<String>? = null): Field<String, String> {
            return Field(aname, "", "",
                    parser = { it }, fieldType = FieldType.Clob)
                    .let { field -> handler?.invoke(field); field }
        }

        fun blob(aname: String, handler: FieldHandler<ByteArray>? = null): Field<ByteArray, ByteArray> {
            return Field(aname, emptyByteArray, emptyByteArray, fieldType = FieldType.ByteArray)
                    .let { field -> handler?.invoke(field); field }
        }

        fun jsonb(aname: String, handler: FieldHandler<String?>? = null): Field<String?, String?> {
            return Field<String?, String?>(aname, "", "",
                    parser = { it }, fieldType = FieldType.Json)
                    .let { field -> handler?.invoke(field); field }
        }

        fun jsonbd(aname: String, handler: FieldHandler<DataMap?>? = null): Field<DataMap?, DataMap?> {
            return Field<DataMap?, DataMap?>(aname, DataMapF.empty(), DataMapF.empty(),
                    fieldType = FieldType.Json)
                    .let { field -> handler?.invoke(field); field }
        }

        fun <T : FieldSet> jsonObj(aname: String, t: T, handler: FieldHandler<DataMapF<T>?>? = null)
                : Field<T, DataMapF<T>?> {
            return Field<T, DataMapF<T>?>(aname, t, DataMapF.empty() as DataMapF<T>, fieldType = FieldType.Json)
                    .let { field -> handler?.invoke(field); field }
        }

        fun <T : FieldSet> jsonList(aname: String, t: T, handler: FieldHandler<MutableList<DataMapF<T>>>? = null)
                : Field<T, MutableList<DataMapF<T>>> {
            return Field(aname, t, DataMapF.emptyList() as MutableList<DataMapF<T>>,
                    fieldType = FieldType.Json)
                    .let { field -> handler?.invoke(field); field }
        }

        fun any(aname: String, handler: FieldHandler<Any?>? = null): Field<Any?, Any?> {
            return Field(aname, null, null)
                    .let { field -> handler?.invoke(field as Field<*, Any?>); field as Field<Any?, Any?> }
        }

        private val emptyByteArray: ByteArray = ByteArray(0)

        internal var context = ThreadLocalFactory.buildThreadLocal<MutableList<String>>()

    }


    /*возвращает тип данного поля.
    для ссылок  - это будет тип ссылаемого объекта-фиелдсета.
    для списков это будет тип хранимого оьъекта-фиелдсета*/
    fun type(): Any {
        val tt = if (targetTypeValue is Any) targetTypeValue else Any()
        return tt::class
    }

    /**referenced FieldSet */
    fun refFieldSet(): MappingFieldSet<*> {
        if (targetTypeValue is DataMap)
            return targetTypeValue.fieldSet as MappingFieldSet<*>
        return targetTypeValue as MappingFieldSet<*>
    }

    fun isReference(): Boolean {
        return containerTypeValue is DataMap
    }

    fun isList(): Boolean {
        return containerTypeValue is Collection<*>
    }

    fun isEnum(): Boolean {
        return targetTypeValue is Enum<*>
    }

    /**
     * Полное имя поля:
     * - если поле использовано как одиночное -> n == fieldName
     * - если поле использовано в составной конструкции -> n  - полный путь составного свойства
     */
    open val n: String
        get() {
            if (context.get() == null)
                return fieldName
            name()
            val res = context.get()!!.joinToString(".")
            context.remove()
            return res
        }

    /**
     * Поле:
     * - если поле одиночное -> f == this
     * - если поле использовано в составной конструкции -> f  == NestedField со списком всех полей  составлюящих "длинное" поле
     */
    val f: Field<*, *>
        get() {
            if (context.get() == null)
                return this
            name()
            val res = context.get()!!.joinToString(".")
            context.remove()
            return NestedField(res, this)
        }

    private fun name() {
        context.getOrSet { mutableListOf() }.add(fieldName)
    }

    operator fun get(index: Int): T {
        context.getOrSet { mutableListOf() }.add("$fieldName[$index]")
        return targetTypeValue
    }

    operator fun unaryPlus(): Field<T, L> {
        if(context.get()!=null)
            nestedField(MappingFieldSet.ProjectionBuilder.currProjection(), this)
        else MappingFieldSet.ProjectionBuilder.currProjection().field(this)
        return this
    }

    private fun nestedField(dp: DataProjection, field: Field<*, *>) {
            var currProjection: DataProjection = dp

            val names = field.f.n.split('.')
            names.forEach { n ->

                if (currProjection.fields[n] == null)
                    currProjection.fields[n] = slice(n)
                currProjection = currProjection.fields[n]!!
            }

    }

    operator fun not(): String = n

    operator fun invoke(builder: T.() -> Unit = {}) {
        MappingFieldSet.ProjectionBuilder.pushSlice(slice(this.n))
        builder(this.targetTypeValue)
        val sl = MappingFieldSet.ProjectionBuilder.popSlice()
        MappingFieldSet.ProjectionBuilder.currProjection().with(sl)
    }

    infix fun by(theSlice: DataProjection) {
        theSlice.parentField = this.n
        MappingFieldSet.ProjectionBuilder.currProjection().with(theSlice)
    }

    operator fun invoke(): T {
        name()
        return targetTypeValue
    }

    infix fun gt(exp1: Any): exp {
        return bop(exp1, Operation.GT)
    }

    infix fun ge(exp1: Any): exp {
        return bop(exp1, Operation.GE)
    }

    infix fun le(exp1: Any): exp {
        return bop(exp1, Operation.LE)
    }

    infix fun lt(exp1: Any): exp {
        return bop(exp1, Operation.LT)
    }

    infix fun eq(exp1: Any): exp {
        return bop(exp1, Operation.EQ)
    }

    infix fun neq(exp1: Any): exp {
        return bop(exp1, Operation.NEQ)
    }

    infix fun like(exp1: Any): exp {
        return bop(exp1, Operation.LIKE)
    }

    infix fun ilike(exp1: Any): exp {
        return bop(exp1, Operation.ILIKE)
    }

    infix fun IS(nul: NULL): exp {
        return bop(nul, Operation.IS_NULL)
    }

    infix fun ISNOT(nul: NULL): exp {
        return bop(nul, Operation.IS_NOT_NULL)
    }

    infix fun IN(list: List<*>): exp {
        return bop(list, Operation.IN)
    }

    infix fun IN(list: Array<*>): exp {
        return bop(list, Operation.IN)
    }

    infix fun between(pairs: Pair<Any, Any>): exp {
        return ((this IS NULL) or (this ge pairs.first)) and ((this IS NULL) or (this le pairs.second))
    }


    private fun bop(exp1: Any, op: Operation): exp {
        val exp2 = exp1 as? exp ?: value(exp1)
        return BinaryOP(f(this.f), exp2, op)
    }


    override fun toString(): String {
        val tt = if (targetTypeValue is Any) targetTypeValue else Any()
        return "Field('$fieldName', type='${tt::class.simpleName}')"
    }

    private class NestedField<T, L>(name: String, field: Field<T, L>)
        : Field<T, L>(name, field.targetTypeValue, field.containerTypeValue) {
        override val n: String
            get() {
                return fieldName
            }
    }

    fun parse(str: String?): L? {
        return if (str != null) parseNN(str) else null
    }

    fun format(value: L?): String? {
        return if (value != null) formatNN(value) else null
    }

    fun parseNN(str: String): L {
        return parser.invoke(str)
    }

    fun formatNN(value: L): String {
        return formatter.invoke(value)
    }

    fun validate(value: L) {
        if (pattern != null && value is String && pattern?.matches(value) == false) {
            val message = patternExplanation ?: "Значение не соответствует шаблону \"${pattern?.pattern}\""
            throw ValidationException(error(message))
        }
        if (maxLength != null && value is String && value.length > maxLength!!) {
            throw ValidationException(error("Длина значения не может превышать максимальное количество знаков: $maxLength"))
        }
        if (minLength != null && value is String && value.length < minLength!!) {
            throw ValidationException(error("Длина значения не может быть меньше минимального количество знаков: $minLength"))
        }
        if (length != null && value is String && value.length > length!!) {
            throw ValidationException(error("Длина значения не может превышать максимальное количество знаков: $length"))
        }
        if (values != null && values!!.isNotEmpty() && !values!!.contains(value)) {
            throw ValidationException(error("Значение не входит в список возможных: ${values!!.joinToString()}"))
        }
        if (min != null && value is Comparable<*> && (value as Comparable<Any>) < (min as Any)) {
            throw ValidationException(error("Значение не может быть меньше минимального: $min"))
        }
        if (max != null && value is Comparable<*> && (value as Comparable<Any>) > (max as Any)) {
            throw ValidationException(error("Значение не может превышать максимального: $max"))
        }
        validator?.invoke(value)
    }

    private fun error(message: String) = ValidationError(this.n, this.caption, message)
}

private typealias FieldHandler<L> = Field<*, L>.() -> Unit

object SomeFieldSet : MFS<SomeFieldSet>("", "")

//TODO: DynamicEntity - дублирует DynamicFieldSet
object DynamicEntity : MFS<DynamicEntity>(DataMapF.DYNAMIC_ENTITY, "") {
    fun copy(dataMap: DataMap?): DataMapF<DynamicEntity> {
        return DataMapF(this, null, true,
                dataMap?.map?.apply {
                    this.remove(DataMapF.DYNAMIC_HOLDER)
                })
    }
}


interface FieldSetOption

object Dynamic : FieldSetOption
object Transience : FieldSetOption //transience чтобы не пыталась проставиться kotlin.jvm.Transient

data class Temporal(val startDateField: String = "startDate",
                    val endDateField: String = "endDate") : FieldSetOption {

    constructor(startDate: Field<Date, Date>, endDate: Field<Date, Date>) :
            this(startDate.fieldName, endDate.fieldName)
}

data class Tree(val parentField: String = "parent", val childsField: String? = "childs") : FieldSetOption {

    constructor(parent: Field<*, *>, childs: Field<*, *>? = null) : this(parent.fieldName, childs?.fieldName)
}

class DisplayName(val fieldName: String) : FieldSetOption {

    constructor(property: KProperty0<Field<*, String>>): this(property.name)
}