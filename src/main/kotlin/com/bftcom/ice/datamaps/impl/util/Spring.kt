package com.bftcom.ice.datamaps.impl.util

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class SpringProfileUtil {
    @Autowired
    lateinit var env: Environment

    fun isProstgress(): Boolean = env.activeProfiles.contains("postgresql")

    fun isOracle(): Boolean = env.activeProfiles.contains("oracle")
}