package com.bftcom.ice.datamaps

import com.bftcom.ice.datamaps.common.maps.FieldSetRepo
import com.bftcom.ice.datamaps.common.maps.getAllFields
import com.bftcom.ice.datamaps.common.maps.getField
import com.bftcom.ice.datamaps.impl.delta.JsonPath
import com.bftcom.ice.datamaps.misc.CaseInsensitiveKeyMap
import com.bftcom.ice.datamaps.misc.FieldType
import com.bftcom.ice.datamaps.misc.caseInsMapOf
import com.bftcom.ice.datamaps.misc.makeSure

/**
 * Created by Щукин on 27.10.2017.
 */
open class DataMapF<T : FieldSet> {

    companion object {
        const val NAME = "name"
        const val CAPTION = "caption"
        const val DISPLAY_NAME = "displayName"
        const val ID = "id"
        const val __ID = "__id"
        const val entityDiscriminator = "entityDiscriminator"
        const val BACKREFID = "backRefId__"
        const val ENTITY = "entity"
        const val DYNAMIC_ENTITY = "dynamic"
        const val DYNAMIC_HOLDER = "__holder"
        const val JSON_PATH = "__jsonPath"
        const val NEW_MAP_GUID = "newMapGuid"

        private val empty: DataMap = DataMapF(UndefinedFieldSet)
        private val emptyList: MutableList<DataMap> = mutableListOf()

        val captionFields = arrayOf("name", "caption", "title", "displayName")

        fun empty(): DataMap = empty
        fun emptyList(): MutableList<DataMap> = emptyList

        fun existing(entity: String, id: Any?): DataMap {
            val f = FieldSetRepo.fieldSetOrNull(entity) as MappingFieldSet<*>?
            return if (f == null || f == UndefinedFieldSet)
                DataMapF<UndefinedFieldSet>(entity, id)
            else f.existing(id) {}
        }

        fun new(entity: String, newMapGuid: String? = null): DataMap {
            val f = FieldSetRepo.fieldSetOrNull(entity) as MappingFieldSet<*>?

            val res =
                    if (f == null || f == UndefinedFieldSet)
                        DataMapF<UndefinedFieldSet>(entity, null, true)
                    else f.create()

            if (newMapGuid != null)
                res.apply { this.newMapGuid = newMapGuid }
            return res
        }
    }

    val entity: String
    var fieldSet: T? = null
    var map = caseInsMapOf<Any>()

    var id: Any? = null
        set(value) {
            if (!isNew() && id != null && id != value && !isDynamic()) {
                throw RuntimeException("cannot change id")
            }
            field = value
        }

    //техническое поле: гуид нового объекта
    //его наличие также говорит что объект новый
    var newMapGuid: String? = null

    constructor (fieldSet: T, id: Any? = null, isNew: Boolean = false)
            : this(fieldSet, fieldSet.entity, id, isNew)

    constructor  (name: String, id: Any? = null,
                  isNew: Boolean = id == null)
            : this(null, name, id, isNew)


    constructor (fieldSet: T, id: Any?, isNew: Boolean = false,
                 props: Map<String, Any?>? = null,
                 copyMap: CaseInsensitiveKeyMap<Any?>? = null)
            : this(fieldSet, fieldSet.entity, id, isNew, props, copyMap)

    constructor (name: String, id: Any?, isNew: Boolean = false,
                 props: Map<String, Any?>? = null,
                 copyMap: CaseInsensitiveKeyMap<Any?>? = null)
            : this(null, name, id, isNew, props, copyMap)

    constructor  (fieldSet: T?, name: String, id: Any? = null,
                  isNew: Boolean = id == null) {
        this.fieldSet = fieldSet
        this.entity = name
        this.id = id
        if (isNew) {
            this.newMapGuid = DeltaStore.newGuid()
            DeltaStore.create(this)
            id?.let {
                DeltaStore.delta(this, ID, null, id)
            }
        }
    }

    private constructor (fieldSet: T?, name: String, id: Any?, isNew: Boolean = false,
                         props: Map<String, Any?>? = null,
                         copyMap: CaseInsensitiveKeyMap<Any?>? = null)
            : this(fieldSet, name, id, isNew) {
        copyMap?.let {
            this.map = copyMap
        }
        props?.forEach { t ->
            doSet(t.key, t.value)
        }
    }

    constructor(fieldSet: T) {
        entity = fieldSet.entity
        this.fieldSet = fieldSet
    }


    constructor () : this("", null, false)

    constructor (name: String) : this(name, null, true)


    fun isNew(): Boolean = newMapGuid != null
    fun isDynamic(): Boolean = DYNAMIC_ENTITY == entity || fieldSet?.isDynamic() == true
    fun isTransient(): Boolean = fieldSet?.isTransient() == true
    internal fun persisted() {
        newMapGuid = null
    }

    operator fun get(field: String): Any? {
        if (ID.equals(field, true))
            return id
        if (field.contains('.')) {
            return nested(field)
        }
        return map[field]
    }

    operator fun <L> get(field: Field<*, L>): L {
        if (field.isList())
            return list(field.n) as L
        val res = nested(field.n)
        return res as L
    }

    operator fun <L> get(afield: T.() -> Field<*, L>): L {
        val f = afield(fieldSet!!)
        return this[f]
    }

    operator fun <L> set(afield: T.(DataMap) -> Field<*, L>, value: L?) {
        val field = afield(fieldSet!!, this)
        this[field.n] = value
    }

    operator fun invoke(f: String): DataMap? {
        if (f.contains('.')) {
            return nested(f) as? DataMap
        }
        return map[f] as? DataMap
    }

    inline fun <L> getOrPut(field: Field<*, L?>, defaultValue: () -> L): L {
        val value = this[field]
        return value ?: return defaultValue().also { this[field, false] = it }
    }

    fun with(body: T.(DataMap) -> Unit): DataMapF<T> {
        body(fieldSet as T, this)
        return this
    }

    fun nested(property: String): Any? {
        return getNestedProperty(this, property)
    }

    fun list(field: Field<*, *>): MutableList<DataMap> {
        return list(field.n)
    }

    fun list(prop: String): MutableList<DataMap> {

        var res = this[prop]
        if (res == null) {
            nestedSet(prop, false, null, true)
            res = this[prop]
        }
        return res as MutableList<DataMap>
    }

    /*** функция для выдачи списка элементов простых типов (строки числа даты).
     *  должна использоваться для json - типов
     */
    fun <T> primeList(prop: String): MutableList<T> {
        val res = this[prop]
        return (res as PrimeList<T>)
    }

    open operator fun <L> set(field: Field<*, L>, silent: Boolean = false, value: L?) {
        set(field.n, silent, value)
    }

    open operator fun set(field: String, silent: Boolean = false, value: Any?) {
        if (field.contains('.')) {
            nestedSet(field, silent, value)
            return
        }
        if (ID.equals(field, true))
            id = value

        val old = map[field]
        doSet(field, value)

        if (!silent && value != old)
            DeltaStore.delta(this, field, old, value)
    }

    private fun nestedSet(property: String, silent: Boolean = false, value: Any?, createDataList: Boolean = false) {
        var curr: DataMap = this
        val props = property.split('.')

        for (item: Int in IntRange(0, props.size - 2)) {
            val prop = getIndexedProperty(props[item])
            var obj = curr[prop.first]

            if (obj is List<*> && prop.second >= 0) {
                obj = obj[prop.second]
            }
            if (obj == null) {
                //если мы находиммя в динамике - давайте попробуем создать динамик автоматически
                obj = createInnerDynamicDataMap(curr, prop.first, silent)
            }
            if (obj is DataMap) {
                curr = obj
            }
            //если промежуточный объект оказался null  - выходим c ошибкой
            makeSure(obj != null, "null object in the middle of property {$property}")
        }

        if (createDataList)
            curr[props[props.size - 1], true] =
                    DataList(ArrayList(), curr, props[props.size - 1])
        else
            curr[props[props.size - 1], silent] = value
    }

    private fun doSet(name: String, value: Any?) {
        //для коллекций  - подсовываем свою имплементацию листа
        //для того чтобы мы могли кидать события об изменении списка
        if (value is ArrayList<*> && value !is DataList)
            map[name] = DataList(value as ArrayList<DataMap>, this, name)
        else
            map[name] = value
    }

    fun nullf(field: String) {
        map[field] = null
    }

    fun nestedl(property: String): List<DataMap> {
        return getNestedProperty(this, property) as List<DataMap>
    }

    fun toReadonly(): DataMapF<T> {
        return DataMapF(fieldSet = this.fieldSet
                ?: UndefinedFieldSet, id = id,
                isNew = isNew(), props = null, copyMap = this.map.toReadonly())
                .apply { this@DataMapF.newMapGuid = this.newMapGuid } as DataMapF<T>
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        other as DataMap

        if (!entity.equals(other.entity, true)) return false

        if (id == null && other.id != null || id != null && other.id == null)
            return false

        if (isNew())
            return this.newMapGuid == other.newMapGuid

        if (id != null && other.id != null) {
            val idn = id as? Number
            val oidn = other.id as? Number
            if (oidn != null && idn != null) {
                //guess ids cannot be doubles
                return idn.toLong() == oidn.toLong()
            }

            return (id == other.id)
        }

        if (newMapGuid == null && other.newMapGuid == null)
            return this === other

        return (newMapGuid == other.newMapGuid)

    }

    override fun hashCode(): Int {
        var result = entity.hashCode()
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (newMapGuid?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "DataMapF(entity='$entity', id=$id, caption = ${firstOf(*captionFields)})"
    }

    private fun createInnerDynamicDataMap(curr: DataMap, prop: String, silent: Boolean): DataMap? {
        val refFieldSet = getDynamicFieldSet(curr, prop)
        var obj1: DataMap? = null
        refFieldSet?.let {
            obj1 = it.create()
            (obj1 as DataMap)[DYNAMIC_HOLDER, true] = curr[DYNAMIC_HOLDER]
            val jp = JsonPath.buildJSonPathString(curr[JSON_PATH] as? String, prop)
            (obj1 as DataMap)[JSON_PATH, true] = jp

            curr[prop, silent || !isNew()] = obj1
        }
        return obj1
    }

}

class SilentDataMapF<T : FieldSet>
(fieldSet: T?, name: String, id: Any?, isNew: Boolean) : DataMapF<T>(fieldSet, name, id, isNew) {


    override fun <L> set(field: Field<*, L>, silent: Boolean, value: L?) {
        super.set(field, true, value)
    }

    override fun set(field: String, silent: Boolean, value: Any?) {
        super.set(field, true, value)
    }
}

object UndefinedFieldSet : MappingFieldSet<UndefinedFieldSet>("", "")
object DynamicFieldSet : MappingFieldSet<DynamicFieldSet>(DataMapF.DYNAMIC_ENTITY, "")

typealias Entity = UndefinedFieldSet
typealias DataMap = DataMapF<*>
typealias DM<T> = DataMapF<T>

fun <T : FieldSet> DataMap.to(): DataMapF<T> = this as DataMapF<T>
fun <T : FieldSet> DataMap.to(t: T): DataMapF<T> {
    return DataMapF(t, this.id, this.isNew(), null, this.map)
}

fun <T : FieldSet> DataMap.of(t: T): DataMapF<T> {
    return DataMapF<T>(this.entity, this.id, this.isNew(), null, this.map)
            .apply { this.fieldSet = t }
}

fun DataMap.firstOf(vararg field: Field<*, *>): Any? {
    field.forEach {
        if (this.map.containsKey(it.n))
            return this[it]
    }
    return null
}

fun DataMap.firstOf(vararg field: String): Any? {
    field.forEach {
        if (this.map.containsKey(it))
            return this[it]
    }
    return null
}

fun DataMap.firstExistingField(vararg field: String): String? {
    field.forEach {
        if (this.map.containsKey(it))
            return it
    }
    return null
}

fun DataMap(entity: String, id: Any? = null,
            isNew: Boolean = false, props: Map<String, Any?>? = null): DataMap {
    return DataMapF<UndefinedFieldSet>(entity, id, isNew, props)
}

fun DataMap(entity: String, id: Any? = null,
            props: Map<String, Any?>? = null): DataMap {
    return DataMapF<UndefinedFieldSet>(entity, id, id == null, props)
}

data class CreateDmSeed<out T : FieldSet>(val dm: T, val map: DataMap) {
    fun with(body: T.(DataMap) -> Unit): DataMap {
        body(dm, map)
        return map
    }
}

data class UpdateDmSeed<out T : FieldSet>(val dm: T, val map: DataMap) {
    fun with(body: T.(DataMap) -> Unit): DataMap {
        body(dm, map)
        return map
    }
}

fun <T : FieldSet> create(dm: T): CreateDmSeed<T> {
    return CreateDmSeed(dm, DataMapF(dm))
}

fun <T : FieldSet> update(dm: T, dataMap: DataMap): UpdateDmSeed<T> {
    return UpdateDmSeed(dm, dataMap)
}

fun DataMap.dataMapToString(): String {
    val sb = StringBuilder()
    return doDataMapToString(this, sb, 1, mutableSetOf()).toString()
}

fun DataMap.print() {
    println(this.dataMapToString())
}

fun doDataMapToString(dm: DataMap, sb: StringBuilder, level: Int, cache: MutableSet<DataMap>): StringBuilder {
    sb.append("${dm.entity}[${dm.id}]{\r\n")
    if (cache.contains(dm))
        return sb.append("}")
    else cache.add(dm)
    val indent = repeatChar(' ', level)
    dm.map.forEach { e ->
        sb.append(indent).append(e.key).append(" = ")
        when {
            dm[e.key] == null -> sb.append("null")
            e.key == DataMapF.DYNAMIC_HOLDER -> {
                val holder = dm(e.key)
                sb.append("{${holder?.entity}:${holder?.id}}")
            }
            dm[e.key] is DataMap -> doDataMapToString(dm[e.key] as DataMap, sb, level + 3, cache)
            dm[e.key] is List<*> -> {
                val indent2 = repeatChar(' ', level + 1)
                sb.append("list{\r\n").append(indent2)
                dm.list(e.key).forEach { doDataMapToString(it, sb, level + 4, cache).append("\r\n").append(indent2) }
                sb.append(indent).append("}")
            }
            else -> sb.append(e.value)
        }
        sb.append("\r\n")

    }
    return sb.append(indent).append("}")
}

private fun repeatChar(char: Char, count: Int): StringBuilder {
    val sb = StringBuilder()
    for (i in 0 until count) {
        sb.append(char)
    }
    return sb
}

fun mergeDataMaps(target: DataMap, provider: DataMap): DataMap {
    return mergeDataMaps(target, provider, mutableMapOf())
}

fun mergeDataMaps(target: List<DataMap>, provider: List<DataMap>): List<DataMap> {
    return mergeDataMaps(target, provider, mutableMapOf(), false)
}

private fun mergeDataMaps(target: DataMap, provider: DataMap, map: MutableMap<DataMap, DataMap>): DataMap {

    if (target === provider || map.containsKey(target))
        return target
    map[target] = target

    makeSure(target.entity.equals(provider.entity, true))
    makeSure(target.id == provider.id)

    //пока просто копируем все из провайдера к нам
    //todo: в будущем надо учитывать измененные поля и не копировать их
    provider.map.forEach { e ->
        when {
            e.value is DataMap && target[e.key] != null -> {
                target[e.key, true] = mergeDataMaps(target(e.key)!!, e.value as DataMap, map)
            }
            e.value is List<*> -> {
                target[e.key, true] = mergeDataMaps(target.list(e.key), provider.list(e.key), map)
            }
            else -> target[e.key, true] = e.value
        }
    }
    return target
}

/**
 * Мержит данные двух списков.
 *
 * NB если target-список пустой - просто вернем provider
 * Для каждого датамапа из target-списка находим соотвествующую мапу из provider-списка
 * и ее мержим (рекурсивно). Если в provider-списке карты нет - просто пропускаем
 *
 * Карты которые есть в провайдере и которых нет в target  - не учитываюся
 *
 */
private fun mergeDataMaps(target: List<DataMap>?, provider: List<DataMap>,
                          map: MutableMap<DataMap, DataMap>, canAddNewEntities: Boolean = true): List<DataMap> {


    if (target == null || target.isEmpty()) return ArrayList(provider)

    target.forEach { t ->
        provider.findById(t.id)?.let {
            mergeDataMaps(t, it, map)
        }
    }

    if (canAddNewEntities)
        provider.forEach { t ->
            if (target.findById(t.id) == null)
                (target as MutableList<DataMap>).add(t)
        }

    return target
}

fun List<DataMap>.findById(id: Any?): DataMap? {
    return this.find { dm -> dm.id == id }
}

fun MutableList<DataMap>.addIfNotInSilent2(dataMap: DataMap) {
    if (findById(dataMap.id) == null)
        (this as DataList).addSilent(dataMap)
}

fun <T : FieldSet> MutableList<DataMapF<T>>.addIfNotInSilent(dataMap: DataMap) {
    if (findById(dataMap.id) == null)
        (this as DataList).addSilent(dataMap)
}

fun DataMap.toHeader(): DataMap {
    val dm = DataMap(this.entity, this.id, false)
    dm.newMapGuid = newMapGuid
    return dm
}

fun <T : FieldSet> DataMapF<T>.toHeaderF(): DataMapF<*> {

    val fs = if (this.fieldSet == null)
        FieldSetRepo.fieldSetOrNull(entity) else fieldSet
    val dm = when (fs) {
        null -> DataMap(entity, this.id, isNew())
        else -> DataMapF(fs, entity, this.id, false)
    }

    dm.newMapGuid = newMapGuid

    return dm
}

//не является частью АПИ
//коллекция элементов примтивного типа (строка число дата буль)
//нужна для передачи сообщений об изменении
class PrimeList<T>(val list: MutableList<T>) : MutableList<T> by list

//не является частью АПИ
//коллекция датамапов
//нужна для передачи сообщений об изменении
internal class DataList(val list: ArrayList<DataMap>,
                        val parent: DataMap, val property: String) : MutableList<DataMap> by list {

    override fun add(element: DataMap): Boolean {
        DeltaStore.listAdd(parent, element, property, list.size)
        return list.add(element)
    }

    override fun addAll(elements: Collection<DataMap>): Boolean {
        var flag = true
        elements.forEach { flag = flag && add(it) }
        return flag
    }

    override fun add(index: Int, element: DataMap) {
        DeltaStore.listAdd(parent, element, property, list.size)
        return list.add(index, element)
    }

    override fun addAll(index: Int, elements: Collection<DataMap>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun remove(element: DataMap): Boolean {
        DeltaStore.listRemove(parent, element, property)
        return list.remove(element)
    }

    override fun removeAt(index: Int): DataMap {
        val dm = list[index]
        remove(dm)
        return dm
    }

    fun addSilent(element: DataMap): Boolean {
        return list.add(element)
    }

}

fun DataMap.getNativeKeyValue(): Pair<String, Any?>? {
    if (this.fieldSet == null || this.fieldSet!!.nativeKey.isEmpty())
        return null
    if (this.fieldSet!!.nativeKey.size > 1)
        TODO()
    val key = this.fieldSet!!.nativeKey[0]
    if (!this.map.containsKey(key.fieldName))
        return null
    return Pair(key.fieldName, this[key])
}

fun getNestedProperty(dm: DataMapF<*>, nested: String): Any? {
    var curr = dm
    val props = nested.split('.')

    for (item: Int in IntRange(0, props.size - 2)) {
        val prop = getIndexedProperty(props[item])
        var obj = curr[prop.first]
        if (obj is List<*> && prop.second >= 0)
            obj = obj[prop.second]

        if (obj is DataMap)
            curr = obj

        //промежуточный объект оказался null - вернем null
        if (obj == null)
            return null
    }
    return curr[props[props.size - 1]]
}

private fun getIndexedProperty(prop: String): Pair<String, Int> {

    val index = prop.indexOf('[')
    val name = if (index > 0) prop.substring(0, index) else prop
    val ind = if (index > 0) (prop.substring(index + 1, prop.length - 1)).toInt() else -1

    return Pair(name, ind)
}


private fun getDynamicFieldSet(obj: DataMap, property: String): MFS<*>? {
    val f = obj.fieldSet?.getField(property)

    val reffiedset = f?.takeIf { it.isReference() }?.refFieldSet()
    if (reffiedset != null && reffiedset.isDynamic()) return reffiedset

    if (obj.isDynamic() || FieldType.Json == f?.fieldType) return DynamicEntity
    return null
}


fun FieldSet.getDisplayField(): Field<*, String> {
    return findDisplayField() ?: throw IllegalStateException("Не найдено отображаемое поля для ${this.entity}")
}

fun FieldSet.findDisplayField(): Field<*, String>? {
    val fields = this.getAllFields()

    var field = getOption(DisplayName::class)?.fieldName?.let { displayFieldName -> fields.first { it.fieldName == displayFieldName } }
            ?: fields.find { it.n == DataMapF.DISPLAY_NAME || it.n == DataMapF.CAPTION }
            ?: fields.find { it.n == DataMapF.NAME }
            ?: fields.find { it.required && it.type() == String::class }

    if (field != null) {
        return field as Field<*, String>
    }
    field = fields.find { it.required && it.isReference() }

    if (field != null) {
        return Field.stringNN(field.n.plus(".").plus(field.refFieldSet().getDisplayField().n))
    }
    return null
}

fun DataMap.getDisplayName(): String {
    return fieldSet?.findDisplayField()?.let { this[it] } ?: (firstOf(*DataMapF.captionFields) as String?).orEmpty()
}