package com.datamaps.mappings

import org.springframework.stereotype.Service
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 */
@Service
class DataMappingsService {

    @Resource
    lateinit var defaultMappingBuilder: DefaultMappingBuilder;

    var mappings = mutableMapOf<String, DataMapping>()

    fun getDataMapping(name: String): DataMapping? {
        var dm = mappings[name]
        if (dm == null) {
            dm = buildMapping(name)
            mappings[name] = dm
        }
        return dm;
    }



    private fun buildMapping(name: String): DataMapping {

        if(1==0)
        {
            TODO("")
        }

         //создание дефолтного маппинга для сущности
         //на основе jdbc-метаданных БД
        return defaultMappingBuilder.buildDefault(name)
    }

}