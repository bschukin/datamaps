package com.bftcom.ice.server.tools

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.TimeUnit


@Service
class GradleAgent {
    @Autowired
    lateinit var sources: Sources

    fun buildCommonsForJvm() {
        buildModule("common")
        buildModule("server")
    }

    fun buildModule(module: String) {

        println("gradle building $module...")
        val path = sources.projectSourcesRoot.absolutePath + File.separator + "gradlew" + if (Sources.isWindows()) ".bat" else ""

        val cmd = path + " :$module:build"
        val p = Runtime.getRuntime().exec(cmd)
        p.waitFor(10, TimeUnit.SECONDS)
    }

}