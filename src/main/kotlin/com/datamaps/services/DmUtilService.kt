package com.datamaps.services

import com.datamaps.mappings.DataMappingsService
import com.datamaps.maps.DataMap
import org.springframework.stereotype.Service
import javax.annotation.Resource

@Service
class DmUtilService
{

    @Resource
    lateinit var dataMappingsService: DataMappingsService

    fun updateBackRef(parent: DataMap, slave: DataMap, property: String, silent: Boolean = true) {
        val dm = dataMappingsService.getDataMapping(parent.entity)
        val backref = dataMappingsService.getBackRefField(dm, property)

        slave.addBackRef(backref)
        slave[backref, silent] = parent
    }
}