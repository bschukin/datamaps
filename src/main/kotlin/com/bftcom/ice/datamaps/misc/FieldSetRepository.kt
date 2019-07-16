package com.bftcom.ice.datamaps.misc

import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.FieldSet
import com.bftcom.ice.datamaps.MFS
import com.bftcom.ice.datamaps.MappingFieldSet
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.jvm.isAccessible



object FieldSetRepo {
    private val cache: CiMap<FieldSet?> = ciMapOf()

    internal fun clearCache() = cache.clear()

    fun fieldSets(): List<String> {
        return cache.keys
                .union(FieldSetProviders.knownFieldSets())
                .distinct()
    }

    fun fieldSetOrNull(entity: String): FieldSet? {
        if (cache.containsKey(entity))
            return cache[entity]

        val fs = FieldSetProviders.findFieldSetDefinition(entity)
        fs?.let {
            registerFieldSet(it)
        }

        return fs
    }

    fun fieldSet(entity: String): FieldSet {
        return fieldSetOrNull(entity)!!
    }

    fun registerFieldSet(t: FieldSet) {
        cache[t.entity] = t
    }

    fun getStaticFields(fieldSet: FieldSet): Map<String, Field<*, *>> {
        return FieldSetProviders.staticFieldsMap(fieldSet)
    }

    fun findDynamicFieldByPath(entity: String, nestedProperty: List<String>): Field<*, *>? {
        return FieldSetProviders.findDynamicFieldByPath(entity, nestedProperty)
    }

    fun getFieldOrNull(fieldSet: FieldSet, field: String): Field<*, *>? {
        return getStaticFields(fieldSet)[field]
    }
}

interface IServerFieldSetProvider {
    fun entities(): List<String>

    fun findFieldSetDefinition(name: String): MappingFieldSet<*>?

    fun canHandleStaticFields(fieldSet: FieldSet): Boolean

    fun staticFields(fieldSet: FieldSet): Map<String, Field<*, *>>

    fun canHandleDynamicFields(fieldSet: String): Boolean = findFieldSetDefinition(fieldSet) != null

    fun findDynamicField(entity: String, path: List<String>): Field<*, *>?
}

object FieldSetProviders {

    private var providers = mutableListOf<IServerFieldSetProvider>()

    fun registerFieldSetProvider(fieldSetProvider: IServerFieldSetProvider) {
        //в тестах несколько раз пытается зарегистрироваться новый провайдер (ServerFieldSetProvider)
        providers.find { it::class == fieldSetProvider::class }
                ?.let { unregisterFieldSetProvider(it) }

        providers.add(fieldSetProvider)
    }

    fun unregisterFieldSetProvider(fieldSetProvider: IServerFieldSetProvider) {
        providers.remove(fieldSetProvider)
    }

    internal fun findFieldSetDefinition(entity: String): FieldSet? {
        return providers.firstNotNullResult { it.findFieldSetDefinition(entity) }
    }

    internal fun knownFieldSets(): List<String> = providers.flatMap { it.entities() }


    internal fun staticFieldsMap(fieldSet: FieldSet): Map<String, Field<*, *>> {

        val res = providers
                .filter { it.canHandleStaticFields(fieldSet) }
        if (res.isEmpty())
            return emptyMap()

        if (res.size == 1)
            return res[0].staticFields(fieldSet)

        assert(res.size == 2)
        return res[0].staticFields(fieldSet).plus(res[1].staticFields(fieldSet))
    }

    fun findDynamicFieldByPath(entity: String, nestedProperty: List<String>): Field<*, *>? {
        return providers.find { it.canHandleDynamicFields(entity) }?.findDynamicField(entity, nestedProperty)
    }
}




