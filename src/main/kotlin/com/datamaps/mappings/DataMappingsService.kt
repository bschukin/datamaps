package com.datamaps.mappings

import com.datamaps.general.NIS
import com.datamaps.services.CacheClearable
import org.springframework.stereotype.Service
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 */
@Service
class DataMappingsService : CacheClearable {

    @Resource
    lateinit var defaultMappingBuilder: DefaultMappingBuilder

    @Resource
    lateinit var nameMappingsStrategy: NameMappingsStrategy

    var mappings = mutableMapOf<String, DataMapping>()

    override fun clearCache()
    {
        mappings.clear()
    }

    fun getTableNameByEntity(entity: String): String {
        //пока все очень просто
        val tableName = nameMappingsStrategy.getDbTableName(entity)
        return tableName
    }

    fun getEntityDefaultNameByTableName(table: String): String {
        return nameMappingsStrategy.getJavaEntityName(table)
    }


    fun getDataMapping(name: String): DataMapping {
        var dm = mappings[name]
        if (dm == null) {
            dm = buildMapping(name)
            mappings[name] = dm
        }
        return dm
    }

    /**
     * Получить маппинг по ссылке
     */
    fun getRefDataMapping(dm: DataMapping, field: String): DataMapping {
        val df = dm[field]

        return when {
            df.isM1 -> getDataMapping(df.manyToOne!!.to)
            df.is1N -> getDataMapping(df.oneToMany!!.to)
            else -> throw NIS()
        }

    }


    /**
     * Получить поле обратной ссылки для маппинга и коллекции 1-N
     */
    fun getBackRefField(dm: DataMapping, listProperty: String): String {

        val mapping = getRefDataMapping(dm, listProperty)
        return mapping.getBackReferenceFieldForThisList(dm, listProperty).name
    }


    private fun buildMapping(name: String): DataMapping {

        if (1 == 0) {
            TODO("")
        }

        //создание дефолтного маппинга для сущности
        //на основе jdbc-метаданных БД
        val tableName = nameMappingsStrategy.getDbTableName(name)
        return defaultMappingBuilder.buildDefault(tableName)
    }

}