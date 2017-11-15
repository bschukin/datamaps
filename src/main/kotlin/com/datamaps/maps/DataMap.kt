package com.datamaps.maps

import com.datamaps.util.caseInsMapOf
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type


/**
 * Created by Щукин on 27.10.2017.
 */
class DataMap {

    @SerializedName("entity")
    var name:String

    var id: Long? = null
        get() = field
        set(value) {
            if (id != null)
                throw RuntimeException("cannot change id")
            field = value
        }

    @SerializedName("_")
    var map = caseInsMapOf<Any>()


    constructor (name: String, id: Long)  {
        this.name = name
        this.id = id
    }

    operator fun get(field: String): Any? {
        return map[field]
    }

    operator fun set(field: String, value: Any) {
        map[field] = value
    }

    operator fun invoke(field: String): DataMap {
        return map[field] as DataMap
    }

    fun nullf(field:String)
    {
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


    override fun toString(): String {
        return printDataMap(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as DataMap

        if (!name.equals(other.name, true)) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (id?.hashCode() ?: 0)
        return result
    }


}

class DMSerializer : JsonSerializer<DataMap> {

    override fun serialize(obj: DataMap, foo: Type, context: JsonSerializationContext): JsonElement {

        val jsonObject = JsonObject()

        jsonObject.addProperty("entity", obj.name)
        jsonObject.addProperty("id", obj.id)
        obj.map.forEach { t, u ->

            when{
                u is DataMap -> jsonObject.add(t, context.serialize(u))
                else ->   jsonObject.addProperty(t,  u?.toString()?: "[null]")
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
