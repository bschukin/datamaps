package com.datamaps.maps

import com.datamaps.util.caseInsMapOf

/**
 * Created by Щукин on 27.10.2017.
 */
class DataMap(val name: String) {

    var map = caseInsMapOf<Any>()

    var id: Long? = null
        get() = field
        set(value) {
            if (id != null)
                throw RuntimeException("cannot change id")
            field = value
        }

    constructor (name: String, id: Long) : this(name) {
        this.id = id
    }

    operator fun get(field: String): Any? {
        return map[field]
    }

    operator fun set(field: String, value: Any) {
        map[field] = value
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
        return "DataMap(name='$name' id='$id') ${map}"
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