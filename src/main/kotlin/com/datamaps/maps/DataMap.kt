package com.datamaps.maps

import com.datamaps.general.checkNIS
import com.datamaps.services.DataService
import com.datamaps.services.DeltaStore
import com.datamaps.util.caseInsMapOf
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
import java.util.*


/**
 * Created by Щукин on 27.10.2017.
 */
open class DataMap {
    companion object {
        private val empty: DataMap = DataMap()
        private val emptyList: MutableList<DataMap> = mutableListOf()
        private val emptyMap = caseInsMapOf<Any>()
        fun empty(): DataMap = empty
        fun emptyList(): MutableList<DataMap> = emptyList
        fun emptyMap() = emptyMap
    }

    @SerializedName("entity")
    val entity: String

    var id: Any? = null
        set(value) {
            if (id != null)
                throw RuntimeException("cannot change id")
            field = value
        }

    @SerializedName("_")
    var map = caseInsMapOf<Any>()

    //техническое поле: гуид нового объекта
    //его наличие также говорит что объект новый
    var newMapGuid: String? = null


    //техническое поле: все зарегистрированные обратные ссылки на родителя
    private val backRefs = caseInsMapOf<String>()

    constructor () : this("", null, false) {
    }

    constructor (name: String) : this(name, null, true)

    constructor  (t: Any) : this(getEntityNameFromClass(t), null, true) {
    }

    constructor (name: String, id: Any? = null, isNew: Boolean = false) {
        this.entity = name
        this.id = id
        if (isNew) {
            this.newMapGuid = UUID.randomUUID().toString()
            DeltaStore.create(this)
            id?.let {
                DeltaStore.delta(this, "id", null, id)
            }
        }
    }

    constructor (name: String, id: Any, props: Map<String, Any>, isNew: Boolean = false)
            : this(name, id, isNew) {
        props.forEach { t, u -> map[t] = u }
    }

    fun isNew(): Boolean = newMapGuid != null
    fun persisted() {
        newMapGuid = null
    }

    operator fun <L> get(field: Field<*, L>): L {
        if (field.t2 is List<*>)
            return list(field.n) as L
        return nested(field.n) as L
    }

    operator fun get(field: String): Any? {
        return map[field]
    }

    operator fun <L> set(field: Field<*, L>, silent: Boolean = false, value: L?) {
        set(field.n, silent, value)
    }

    operator fun set(field: String, silent: Boolean = false, value: Any?) {
        val old = map[field]
        silentSet(field, value)

        if (!silent)
            DeltaStore.delta(this, field, old, value)
    }

    private fun silentSet(name: String, value: Any?) {
        //для коллекций  - подсовываем свою имплементацию листа
        //для того чтобы мы могли кидать события об изменении списка
        if (value is ArrayList<*> && !(value is DataList))
            map[name] = DataList(value as ArrayList<DataMap>, this, name)
        else
            map[name] = value
    }

    operator fun invoke(f: String): DataMap {
        return map[f] as DataMap
    }

    fun nullf(field: String) {
        map[field] = null
    }

    fun list(field: Field<*, *>): MutableList<DataMap> {
        return list(field.n)
    }

    fun list(prop: String): MutableList<DataMap> {

        var res = this[prop]
        if (res == null) {
            res = DataList(ArrayList(), this, prop)
            this[prop, true] = res
        }
        return res as MutableList<DataMap>;
    }

    fun addBackRef(name: String) {
        backRefs[name] = name
    }

    fun isBackRef(name: String): Boolean {
        return backRefs.containsKey(name)
    }

    fun nested(property: String): Any? {
        return getNestedPropertiy(this, property)
    }

    fun nestedl(property: String): List<DataMap> {
        return getNestedPropertiy(this, property) as List<DataMap>
    }

    override fun toString(): String {
        return printDataMap(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as DataMap

        if (!entity.equals(other.entity, true)) return false
        if (id != other.id) return false
        if (newMapGuid != other.newMapGuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = entity.hashCode()
        result = 31 * result + (id?.hashCode() ?: 0)
        return result
    }

}

fun <T : Any> datamap(t: T, id: Any? = null, isNew: Boolean = false): DataMap {
    val ent = getEntityNameFromClass(t)
    val res = DataMap(ent, id, isNew)
    return res
}

open class DMSerializer : JsonSerializer<DataMap> {

    private val map  = mutableMapOf<DataMap, DataMap>()

    override fun serialize(obj: DataMap, foo: Type, context: JsonSerializationContext): JsonElement {

        val jsonObject = JsonObject()

        jsonObject.addProperty("entity", obj.entity)
        when (obj.id) {
            null -> jsonObject.addProperty("id", "null")
            is Long -> jsonObject.addProperty("id", obj.id as Long)
            else -> jsonObject.addProperty("id", obj.id as String)
        }

        obj.map.forEach { t, u ->
            addJsonNode(u, obj, t, jsonObject, context)
        }
        return jsonObject
    }

    fun addJsonNode(u: Any?, obj: DataMap, t: String, jsonObject: JsonObject, context: JsonSerializationContext) {
        when (u) {
            is DataMap -> {

                if (obj.isBackRef(t) || map.containsKey(u))
                    jsonObject.add(t, context.serialize(
                            DataMap(u.entity, u.id!!, mapOf("isBackRef" to true)))
                    )
                else {
                    map.put(u,u)
                    jsonObject.add(t, context.serialize(u))
                }
            }
            is Collection<*> -> {
                val arr = JsonArray()
                u.forEach { e -> arr.add(context.serialize(e)) }
                jsonObject.add(t, arr)
            }
            else -> jsonObject.addProperty(t, u?.toString() ?: "[null]")
        }
    }
}

class DMSerializer2(val dataService: DataService) : DMSerializer() {


    override fun serialize(obj: DataMap, foo: Type, context: JsonSerializationContext): JsonElement {

        val jsonObject = JsonObject()

        jsonObject.addProperty("entity", obj.entity)
        when (obj.id) {
            null -> jsonObject.addProperty("id", "null")
            is Long -> jsonObject.addProperty("id", obj.id as Long)
            is Int -> jsonObject.addProperty("id", obj.id as Int)
            else -> jsonObject.addProperty("id", obj.id as String)
        }
        if (obj["isBackRef"] == true) {
            jsonObject.addProperty("isBackRef", "true")
            return jsonObject
        }
        val mapping = dataService.getDataMapping(obj.entity)
        mapping.scalars().filter { !it.key.equals("id", true) }.toSortedMap().forEach { t, u ->
            addJsonNode(obj[u.name], obj, t, jsonObject, context)
        }
        mapping.refs().toSortedMap().forEach { t, u ->
            addJsonNode(obj[u.name], obj, t, jsonObject, context)
        }
        mapping.lists().toSortedMap().forEach { t, u ->
            addJsonNode(obj[u.name], obj, t, jsonObject, context)
        }
        return jsonObject
    }
}

fun printDataMap(dm: DataMap): String {
    val gson = GsonBuilder()
            .registerTypeAdapter(DataMap::class.java, DMSerializer())
            .setPrettyPrinting().create()
    return gson.toJson(dm)
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

    checkNIS(target.entity == provider.entity)
    checkNIS(target.id == provider.id)

    //пока просто копируем все из провайдера к нам
    //todo: в будущем надо учитывать измененные поля и не копировать их
    provider.map.forEach { t, u ->
        when {
            u is DataMap && target[t] != null -> {
                target[t] = mergeDataMaps(target(t), u, map)
            }
            u is List<*> -> {
                target[t] = mergeDataMaps(target.list(t), provider.list(t), map)
            }
            else -> target[t] = u
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

    if (!canAddNewEntities)
        checkNIS(target?.size == provider.size)

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

fun MutableList<DataMap>.addIfNotIn(dataMap: DataMap) {
    if (findById(dataMap.id) == null)
        this.add(dataMap)
}

fun MutableList<DataMap>.addIfNotInSilent(dataMap: DataMap) {
    if (findById(dataMap.id) == null)
        (this as DataList).addSilent(dataMap)
}


fun getNestedPropertiy(dm: DataMap, nested: String): Any? {
    var curr = dm
    val list = nested.split('.')

    for (item: Int in IntRange(0, list.size - 2)) {
        val prop = getIndexedProperty(list[item])
        var obj = curr[prop.first]
        if (obj is List<*> && prop.second >= 0)
            obj = obj[prop.second]

        if (obj is DataMap)
            curr = obj
    }
    return curr[list[list.size - 1]]
}

private fun getIndexedProperty(prop: String): Pair<String, Int> {

    val index = prop.indexOf('[')
    val name = if (index > 0) prop.substring(0, index) else prop
    val ind = if (index > 0) Integer.parseInt(prop.substring(index + 1, prop.length - 1)) else -1

    return Pair(name, ind)
}
