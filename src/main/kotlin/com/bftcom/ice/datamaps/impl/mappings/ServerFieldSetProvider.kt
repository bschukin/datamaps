package com.bftcom.ice.datamaps.impl.mappings

import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.FieldSet
import com.bftcom.ice.datamaps.MappingFieldSet
import com.bftcom.ice.datamaps.utils.CaseInsensitiveKeyMap
import com.bftcom.ice.datamaps.utils.OrderedCaseInsensitiveMap
import com.bftcom.ice.datamaps.common.maps.*
import com.bftcom.ice.datamaps.impl.util.CacheClearable
import org.apache.commons.lang3.reflect.FieldUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Lazy
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternUtils
import org.springframework.core.type.classreading.CachingMetadataReaderFactory
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.lang.Nullable
import org.springframework.stereotype.Service
import java.io.IOException
import javax.annotation.PostConstruct


@Service
@Lazy(false)
internal class ServerFieldSetProvider : CacheClearable, IServerFieldSetProvider {


    @Value("\${dm.fieldSetPackages:''}")
    private var packagesToScan: List<String> = emptyList()

    //карта:  имя entity (простое) -  FieldSet
    private val map = CaseInsensitiveKeyMap<MappingFieldSet<*>>()

    //карта:  имя entity (простое) -  поля
    private val fields = CaseInsensitiveKeyMap<Map<String, Field<*, *>>>()

    //карта:  имя entity (простое) -  имя класса FieldSet'a
    //здесь хранятся незагруженные сущности
    private val candidates = CaseInsensitiveKeyMap<String>()

    @PostConstruct
    fun init() {
        FieldSetProviders.registerFieldSetProvider(this)

        packagesToScan.filter { !it.isBlank() }.flatMap { FieldSetScanner().scanFieldSets(it) }
                .forEach {
                    candidates[it.split('.').last()] = it
                }
    }

    override fun clearCache() {
        //FieldSetRepo.clearCache()
        map.clear()
        candidates.clear()
        fields.clear()
        init()
    }


    override fun entities(): List<String> {
        val l = candidates.keys.toMutableList()
        l.addAll(map.keys.toList())
        return l.toList()
    }

    override fun findFieldSetDefinition(name: String): MappingFieldSet<*>? {
        val mfs = map[name]

        if (mfs == null) {
            val c = candidates[name]
            if (c != null) {
                val clazz = Class.forName(c)
                map[name] = clazz.kotlin.objectInstance as MappingFieldSet<*>
                candidates.remove(name)
            }
        }
        return map[name]
    }

    override fun canHandleStaticFields(fieldSet: FieldSet): Boolean {
        return true
    }

    override fun staticFields(fieldSet: FieldSet): Map<String, Field<*, *>> {
        if (!fields.containsKey(fieldSet.entity))
            fields[fieldSet.entity] = getFieldsByReflection(fieldSet)

        return fields[fieldSet.entity]!!
    }


    override fun findDynamicField(entity: String, path: List<String>): Field<*, *>? {
        val fs = findFieldSetDefinition(entity) ?: return null

        if (path.size > 1)
            return Field.string(path.last())

        return fs.getField(path[0])
    }

    companion object {
        private fun getFieldsByReflection(fieldSet: FieldSet): OrderedCaseInsensitiveMap<Field<*, *>> {
            return FieldUtils.getAllFields(fieldSet::class.java)
                    .filter { it.type.isAssignableFrom(Field::class.java) }
                    .map {
                        it.isAccessible = true
                        it.get(fieldSet::class.objectInstance) as? Field<*, *>
                    }.filterNotNull().associate {
                        it.fieldName to it
                    }
                    .toMap(OrderedCaseInsensitiveMap())
        }
    }

}

@Suppress("PrivatePropertyName")
internal open class FieldSetScanner : ClassPathScanningCandidateComponentProvider() {

    private var resourcePatternResolver: ResourcePatternResolver? = null
        get() {
            if (field == null) {
                field = PathMatchingResourcePatternResolver()
            }
            return field
        }
    private val DEFAULT_RESOURCE_PATTERN = "**/*.class"

    private var resourcePattern_ = DEFAULT_RESOURCE_PATTERN


    fun scanFieldSets(basePackages: String): List<String> {

        val res = basePackages.split(",").map { it.trim() }.map {basePackage->

            val candidates = mutableListOf<String>()
            try {
                val packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                        resolveBasePackage(basePackage) + '/'.toString() + this.resourcePattern_
                val resources = resourcePatternResolver!!.getResources(packageSearchPath)
                for (resource in resources) {
                    if (resource.isReadable) {
                        val metadataReader = metadataReaderFactory.getMetadataReader(resource)
                        if (isFieldSet(metadataReader)) {
                            candidates.add(metadataReader.classMetadata.className)
                        }
                    }
                }
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            }
            candidates
        }.flatten()
        return res
    }

    private fun isFieldSet(metadataReader: MetadataReader): Boolean {

        metadataReader.classMetadata.superClassName?.let {
            val clazz = Thread.currentThread().contextClassLoader.loadClass(it)
            if (MappingFieldSet::class.java.isAssignableFrom(clazz)) {
                return true
            }
        }

        return false
    }

    override fun setResourceLoader(@Nullable resourceLoader: ResourceLoader) {
        this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
        this.metadataReaderFactory = CachingMetadataReaderFactory(resourceLoader)
    }

    override fun setResourcePattern(resourcePattern: String) {
        this.resourcePattern_ = resourcePattern
    }

}