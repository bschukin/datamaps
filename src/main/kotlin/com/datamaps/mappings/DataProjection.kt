package com.datamaps.mappings

import com.datamaps.util.linkedCaseInsMapOf

/**
 * Created by Щукин on 07.11.2017.
 */

/**
 *
 * JiraWorker
 * {
 *    {{"default"}},
 *        "gender" {
 *              fields = "gender"
 *        }
 * }
 *
 */

class DataProjection {



    var field: String? = null
    var entity: String? = null
    var groups = mutableListOf<String>()
    var fields = linkedCaseInsMapOf<DataProjection>() //рекурсивные проекции

    var last: DataProjection? = null
    var prev: DataProjection? = null

    constructor(entity: String?) {
        this.entity = entity
    }

    constructor(entity: String?, field: String?) {
        this.field = field
        this.entity = entity
    }

    operator fun get(field: String): DataProjection? {
        return fields.get(field)
    }

    fun group(gr:String):DataProjection
    {
        groups.add(gr)
        return this
    }

    fun full():DataProjection
    {
        return group(FULL)
    }

    fun default():DataProjection
    {
        return group(FULL)
    }

    fun refs():DataProjection
    {
        return group(REFS)
    }

    fun entity(name: String ):DataProjection
    {
        entity = name
        return this
    }

    fun field(f:String):DataProjection
    {
        fields[f] = DataProjection(null, f)
        last  = fields[f]
        return this
    }
    fun inner():DataProjection {
        this.last!!.prev = this
        return this.last!!
    }
    fun end():DataProjection {
        return prev!!
    }
}