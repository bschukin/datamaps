package com.datamaps.maps

import javax.xml.crypto.Data

/**
 * Created by Щукин on 27.10.2017.
 */
class DataMap(val name: String) : HashMap<String, Any>() {

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

    fun list(prop:String) :MutableList<DataMap> {

        var res =  this[prop];
        if(res==null)
        {
            res = mutableListOf<DataMap>()
            this[prop] = res
        }
        return res as MutableList<DataMap>;
    }

}