package com.datamaps.services

import com.datamaps.mappings.DataMappingsService
import com.datamaps.maps.DataMap
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Service
import javax.annotation.Resource


interface CacheClearable
{
    fun clearCache()
}

@Service
class DmUtilService: ApplicationContextAware
{
    lateinit var appCtx : ApplicationContext

    override fun setApplicationContext(p0: ApplicationContext?) {
        appCtx = p0!!
    }

    fun clearCaches()
    {
        appCtx.getBeansOfType(CacheClearable::class.java).forEach { t, u ->
            u.clearCache()
        }
    }


    @Resource
    lateinit var dataMappingsService: DataMappingsService

    fun updateBackRef(parent: DataMap, slave: DataMap, property: String, silent: Boolean = true) {
        val dm = dataMappingsService.getDataMapping(parent.entity)
        val backref = dataMappingsService.getBackRefField(dm, property)

        slave.addBackRef(backref)
        slave[backref, silent] = parent
    }
}