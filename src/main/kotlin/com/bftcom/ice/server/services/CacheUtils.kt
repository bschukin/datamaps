package com.bftcom.ice.server.services

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Service


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
}