package com.datamaps.services

import com.datamaps.general.NIY
import com.datamaps.mappings.DataMappingsService
import org.springframework.stereotype.Service
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 */

@Service
class QueryBuilder {
    @Resource
    lateinit var dataMappingsService: DataMappingsService;

    public fun createQueryByEntityNameAndId(name: String, id: Long): SqlQuery {
        var dm = dataMappingsService.getDataMapping(name)
        //val query = Query()
        throw NIY()
    }

}