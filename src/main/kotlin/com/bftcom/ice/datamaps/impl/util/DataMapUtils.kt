package com.bftcom.ice.datamaps.impl.util

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.misc.FieldType
import com.bftcom.ice.datamaps.common.maps.*
import com.bftcom.ice.datamaps.impl.delta.AnyValue
import com.bftcom.ice.datamaps.impl.delta.Delta
import com.bftcom.ice.datamaps.impl.delta.DeltaType
import com.bftcom.ice.datamaps.impl.mappings.DataMappingsService
import org.springframework.stereotype.Service


@Service
class DataMapsUtilService(
        val dataMappingsService: DataMappingsService,
        val dataService: DataService
) {


    //todo: перенести в DeltaMachibe
    fun updateBackRef(parent: DataMap, slave: DataMap, parentProperty: String, silent: Boolean = true) {

        val backref = getBackRefField(parent, parentProperty)
        slave[backref, silent] = parent
    }

    //todo: перенести в MappingService
    fun getBackRefField(parent: DataMap, parentProperty: String): String {
        val dm = dataMappingsService.getDataMapping(parent.entity)
        val backref = dataMappingsService.getBackRefField(dm, parentProperty)

        return backref
    }

    fun copy(source: DataMap): DataMap {
        val findCopyTargets = mutableSetOf<DataMap>()
        findCopyTargets(source, findCopyTargets)
        return copy(source, findCopyTargets)
    }

    private fun copy(source: DataMap, toCopy: MutableSet<DataMap>, cache: MutableMap<DataMap, DataMap> = mutableMapOf()): DataMap {


        if (cache.containsKey(source))
            return cache[source]!!

        val res = DataMapF(source.fieldSet, source.entity, id = null, isNew = true)
        cache[source] = res

        source.map.keys
                .filter { !it.equals("id", true) }
                .forEach { fieldName ->
                    val value = source[fieldName]
                    when {
                        value is DataMap -> {
                            when (value["isBackRef"]) {
                                true -> res[fieldName] = copy(source[fieldName] as DataMap, toCopy, cache)
                                else -> res[fieldName] =
                                        if (toCopy.contains(value))
                                            copy(value, toCopy, cache)
                                        else
                                            findReferenceByNativeKey(value)
                            }
                        }
                        value is List<*> -> value.forEach {
                            res.list(fieldName).add(copy(it as DataMap, toCopy, cache))
                        }
                        else -> res[fieldName] = source[fieldName]
                    }
                }

        return res
    }

    private fun findReferenceByNativeKey(value: DataMap): DataMap {
        if (value.fieldSet == null)
            return value
        if (value.fieldSet!!.nativeKey.isEmpty())
            return value
        if (value.fieldSet!!.nativeKey.size > 1)
            TODO()
        val key = value.fieldSet!!.nativeKey[0]
        if (!value.map.containsKey(key.fieldName))
            return value

        val res = dataService.find(on(value.entity).field(key).filter(f(key) eq value(value[key])))
        if (res != null)
            return res
        return value
    }

    private fun findCopyTargets(source: DataMap, result: MutableSet<DataMap>, cache: MutableSet<DataMap> = mutableSetOf()) {

        if (cache.contains(source))
            return

        result.add(source)
        cache.add(source)

        source.map.keys
                .filter { !it.equals("id", true) }
                .forEach { fieldName ->
                    val value = source[fieldName]
                    when {
                        (value is DataMap && value.isDynamic()) ->
                            findCopyTargets(value, result, cache)

                        value is List<*> -> value.forEach {
                            findCopyTargets(it as DataMap, result, cache)
                        }
                    }
                }

    }

}


@Service
class DataMapsProjectionUtilService(
        val dataMappingsService: DataMappingsService,
        val dataService: DataService
) {
    fun getFullImageProjection(entity: String,
                               parentProjection: DataProjection = on(entity)): DataProjection {

        parentProjection.scalars()
        parentProjection.withRefs()
        parentProjection.withBlobs()
        parentProjection.withFormulas()

        val dm = dataMappingsService.getDataMapping(entity)
        dm.listsGroup.fields
                .forEach { f ->

                    parentProjection.field(f)

                    val dataField = dm[f]

                    val slice = parentProjection.fields[f]
                    getFullImageProjection(dataField.referenceTo(), slice!!)
                }

        return parentProjection
    }
}

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