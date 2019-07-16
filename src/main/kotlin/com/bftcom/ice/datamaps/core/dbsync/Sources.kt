package com.bftcom.ice.datamaps.core.dbsync

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File


@Service
class Sources {

    companion object {
        private val OS = System.getProperty("os.name").toLowerCase()

        fun isWindows(): Boolean {

            return OS.indexOf("win") >= 0

        }

        fun isMac(): Boolean {

            return OS.indexOf("mac") >= 0

        }

        fun isUnix(): Boolean {

            return OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0

        }
    }


    @Value("\${ice.projectRoot:}")
    private var rootString: String? = null

    @Value("\${ice.dbSync.dirsToScanScripts: db-scripts}")
    var dirsToScansScripts = emptyList<String>()

    val projectSourcesRoot: File by lazy {
        initProjectSourcesRoot()
    }

    val dotIceDir: File by lazy {
        initDotIceDir()
    }

    val dotIceJsDir: File by lazy {
        initDotJsDir()
    }




    private fun initProjectSourcesRoot(): File {
        return if (rootString.isNullOrEmpty())
        //находясь внутри конкретного модуля и папки билд откатывается на уровень  верхнего проекта
            File(Sources::class.java.protectionDomain.codeSource.location.file)
                    .parentFile.parentFile.parentFile.parentFile.parentFile.parentFile
        else File(rootString)
    }

    private fun initDotIceDir(): File {
        val path = File(projectSourcesRoot, ".ice")
        if (!path.exists())
            path.mkdirs()
        return path
    }
    private fun initDotJsDir(): File {
        val path = File(dotIceDir, "js")
        if (!path.exists())
            path.mkdirs()
        return path
    }
}