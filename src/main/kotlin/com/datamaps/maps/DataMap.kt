package com.datamaps.maps

import com.datamaps.general.checkNIS
import com.datamaps.util.caseInsMapOf
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type


/**
 * Created by Щукин on 27.10.2017.
 */
class DataMap {

    @SerializedName("entity")
    var entity: String

    var id: Long? = null
        get() = field
        set(value) {
            if (id != null)
                throw RuntimeException("cannot change id")
            field = value
        }

    @SerializedName("_")
    var map = caseInsMapOf<Any>()

    private val backRefs = caseInsMapOf<String>()

    constructor (name: String, id: Long) {
        this.entity = name
        this.id = id
    }

    constructor (name: String, id: Long, props: Map<String, Any>) {
        this.entity = name
        this.id = id
        props.forEach { t, u -> map[t] = u }
    }

    operator fun get(field: String): Any? {
        return map[field]
    }

    operator fun set(field: String, value: Any) {
        map[field] = value
    }

    operator fun invoke(f: String): DataMap {
        return map[f] as DataMap
    }

    fun nullf(field: String) {
        map[field] = null
    }


    fun list(prop: String): MutableList<DataMap> {

        var res = this[prop]
        if (res == null) {
            res = mutableListOf<DataMap>()
            this[prop] = res
        }
        return res as MutableList<DataMap>;
    }

    fun addBackRef(name: String) {
        backRefs[name] = name
    }

    fun isBackRef(name: String): Boolean {
        return backRefs.containsKey(name)
    }

    fun nested(property: String):Any? {
        return getNestedPropertiy(this, property)
    }

    fun nestedl(property: String):List<DataMap> {
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

        return true
    }

    override fun hashCode(): Int {
        var result = entity.hashCode()
        result = 31 * result + (id?.hashCode() ?: 0)
        return result
    }


}

class DMSerializer : JsonSerializer<DataMap> {

    override fun serialize(obj: DataMap, foo: Type, context: JsonSerializationContext): JsonElement {

        val jsonObject = JsonObject()

        jsonObject.addProperty("entity", obj.entity)
        jsonObject.addProperty("id", obj.id)
        obj.map.forEach { t, u ->

            when (u) {
                is DataMap -> {
                    if (obj.isBackRef(t))
                        jsonObject.add(t, context.serialize(
                                DataMap(u.entity, u.id!!, mapOf("isBackRef" to true)))
                        )
                    else jsonObject.add(t, context.serialize(u))
                }
                is Collection<*> -> {
                    val arr = JsonArray()
                    u.forEach { e -> arr.add(context.serialize(e)) }
                    jsonObject.add(t, arr)
                }
                else -> jsonObject.addProperty(t, u?.toString() ?: "[null]")
            }

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

fun List<DataMap>.findById(id: Long?): DataMap? {
    return this.find { dm -> dm.id == id }
}

fun MutableList<DataMap>.addIfNotIn(dataMap: DataMap) {
    if (findById(dataMap.id) == null)
        this.add(dataMap)
}


fun getNestedPropertiy(dm: DataMap, nested: String):Any? {
    var curr = dm
    var list = nested.split('.')

    for (item: Int in IntRange(0, list.size - 2)) {
        val prop = getIndexedProperty(list[item])
        var obj = curr[prop.first]
        if(obj is List<*> && prop.second>=0)
            obj = obj[prop.second]

        if(obj is DataMap)
            curr = obj
    }
    return curr[list[list.size-1]]
}

private fun getIndexedProperty(prop:String):Pair<String, Int>
{

    var index = prop.indexOf('[')
    val name = if(index>0) prop.substring(0, index) else prop
    val ind = if(index>0) Integer.parseInt(prop.substring(index+1, prop.length-1)) else -1

    return Pair(name, ind)
}
