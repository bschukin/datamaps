package com.bftcom.ice.datamaps.impl.mappings

import com.bftcom.ice.datamaps.misc.throwImpossible
import com.bftcom.ice.datamaps.DataMap
import com.bftcom.ice.datamaps.common.maps.FieldSetRepo
import com.bftcom.ice.datamaps.impl.util.CacheClearable
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Service
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 */
@Service
@DependsOn("serverFieldSetProvider")
class DataMappingsService : CacheClearable {

    @Resource
    private lateinit var defaultMappingBuilder: DefaultMappingBuilder

    @Resource
    private lateinit var nameMappingsStrategy: NameMappingsStrategy

    @Resource
    private lateinit var dbMetadataService: DbMetadataService

    @Resource
    private lateinit var fieldSetMappingBuilder: FieldSetMappingBuilder


    //карта: entity - маппинг.
    var mappings = mutableMapOf<String, DataMapping>()

    override fun clearCache() {
        mappings.clear()
    }

    fun getTableNameByEntity(entity: String): String {
        val f = FieldSetRepo.fieldSetOrNull(entity)
        if(f?.table!=null)
            return f.table!!
        //пока все очень просто
        return nameMappingsStrategy.getDefaultDbTableName(entity)
    }

    fun getEntityDefaultNameByTableName(table: String): String {
        return nameMappingsStrategy.getJavaEntityName(table)
    }

    fun removeDataMapping(name: String): DataMapping? {
        dbMetadataService.clearTableInfo(name)
        return mappings.remove(name)
    }

    fun getDataMapping(entityName: String): DataMapping {
        return getDataMapping(entityName, true)!!
    }

    fun getDataMapping(entityName: String, throwIfNotFound: Boolean = true): DataMapping? {
        var dm = mappings[entityName]
        if (dm == null) {
            dm = buildMapping(entityName, throwIfNotFound)
            if(dm==null&& !throwIfNotFound)
                return null

            mappings[entityName] = dm!!
        }
        return dm
    }

    fun getDefaultDataMapping(entityName: String, throwIfNotFound: Boolean): DataMapping? {

        val tableName = getTableNameByEntity(entityName)
        return defaultMappingBuilder.buildDefault(tableName, throwIfNotFound) ?: return null
    }

    /**
     * Получить маппинг по ссылке
     */
    fun getRefDataMapping(dm: DataMapping, field: String): DataMapping {
        val df = dm[field]

        return when {
            df.isM1() -> getDataMapping(df.referenceTo())
            df.is1N() -> getDataMapping(df.referenceTo())
            else -> throwImpossible()
        }

    }


    /**
     * Получить поле обратной ссылки для маппинга и коллекции 1-N
     */
    fun getBackRefField(dm: DataMapping, listProperty: String): String {

        val mapping = getRefDataMapping(dm, listProperty)
        return mapping.getBackReferenceFieldForThisList(dm, listProperty).name
    }

    fun getBackRefField(parent: DataMap, parentProperty: String):String {
        val dm = getDataMapping(parent.entity)
        val backref = getBackRefField(dm, parentProperty)

        return backref
    }




    private fun buildMapping(entityName: String, throwIfNotFound: Boolean=true): DataMapping? {

        //создание дефолтного маппинга для сущности
        //на основе jdbc-метаданных БД
        val tableName = getTableNameByEntity(entityName)
        val defaultMapping =  defaultMappingBuilder.buildDefault(tableName, throwIfNotFound)
        if(defaultMapping==null && !throwIfNotFound)
            return null
        //построили дефолтный маппинг -
        //строим кастомный - на основе фиелдсета (кастомного маппинга)

        return fieldSetMappingBuilder.buildMappingBasedOnFieldSet(entityName, defaultMapping!!)
    }

}
