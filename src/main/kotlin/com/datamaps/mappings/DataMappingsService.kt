package com.datamaps.mappings

import com.datamaps.general.NIY
import org.springframework.stereotype.Service
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 */
@Service
class DataMappingsService {

    @Resource
    lateinit var defaultMappingBuilder: DefaultMappingBuilder

    @Resource
    lateinit var nameMappingsStrategy: NameMappingsStrategy

    var mappings = mutableMapOf<String, DataMapping>()

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
        var df = dm[field]

        if (!df.isM1)
            throw NIY()

        return getDataMapping(df.manyToOne!!.to)
    }


    private fun buildMapping(name: String): DataMapping {

        if (1 == 0) {
            TODO("")
        }

        //создание дефолтного маппинга для сущности
        //на основе jdbc-метаданных БД
        var tableName = nameMappingsStrategy.getDbTableName(name)
        return defaultMappingBuilder.buildDefault(tableName)
    }

}