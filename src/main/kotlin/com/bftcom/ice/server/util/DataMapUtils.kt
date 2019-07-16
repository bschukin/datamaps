package com.bftcom.ice.server.util

import com.bftcom.ice.common.general.FieldType
import com.bftcom.ice.common.maps.*


/**
 * Утилита сравнивает две датапампы и говорит , отличаются они чем то или нет.
 * Предназначена для быстрого понимания - отличаются две версии
 * одной и той же датамапы чем то или нет
 * **/

fun DataMap.differsFrom(dm: DataMap?, compareHeaders: Boolean, recursievly: Boolean = true,
                        cache: MutableSet<DataMap> = mutableSetOf()): Boolean {
    if (cache.contains(this))
        return false
    cache.add(this)

    //если они не даже по иквалс не сравнимы, то нечего и говорить
    if (compareHeaders) {
        if (this != dm)
            return true
    }
    if (this.map.size != dm?.map?.size)
        return true

    var flag = false
    val f = fieldSet

    this.map.forEach { t, u ->
        when (u) {
            is DataMap -> {
                val compareHeads = compareHeaders && !(f?.getField(t)?.fieldType == FieldType.Json)
                if(u.differsFrom(dm(t), compareHeads, recursievly, cache))
                {
                    flag = true
                    return@forEach
                }
            }
            is List<*> -> {
                if (u.size != dm.list(t).size) {
                    flag = true
                    return@forEach
                }
                if(!u.isEmpty()) {
                    if(u.toList().first() is DataMap) {
                        for (i in 0..u.size - 1) {
                            if ((u[i] as DataMap).differsFrom(dm.list(t)[i], compareHeaders, recursievly, cache)) {
                                flag = true
                                return@forEach
                            }
                        }
                    }
                }

            }
            u -> if (u != dm[t]) {
                flag = true
                return@forEach
            }
        }
    }

    return flag
}

/***
 * Утилита возвращает разницу между двемя датамапами в виде объектов Delta
 */
fun DataMap.diffs(dm: DataMap, recursievly: Boolean = true): List<Delta> {
    if (recursievly)
        throw NotImplementedError("))")

    val f = fieldSet

    val res = mutableListOf<Delta>()
    this.map.forEach { t, u ->
        when {
            u is Collection<*> -> TODO()
            u is DataMap && f?.getField(t)?.fieldType == FieldType.Json
                    && u.differsFrom(dm(t), false, true) -> {

                res.add(Delta(DeltaType.VALUE_CHANGE, dm, t,
                        newValue = AnyValue(u.toJson(JsonWriteOptions(false, true)))))

            }
            else -> if (u != dm[t])
                res.add(Delta(DeltaType.VALUE_CHANGE, dm, t, newValue = AnyValue(u)))

        }
    }
    return res
}