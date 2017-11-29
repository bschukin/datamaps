package com.datamaps.mappings

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors
import javax.annotation.PostConstruct

/**
 * Created by Щукин on 03.11.2017.
 */
@Service
class DmFileStorage {

    @Value("\${dm.pathes}")
    var pathes = mutableListOf<String>()

    var mappingFiles = hashMapOf<String, File>()

    @PostConstruct
    fun init() {
        preparePathes()
        findMappings()
    }

    private fun findMappings() {

        pathes.forEach({var1->
            run {
                Files.walkFileTree(File(var1).toPath(), object : SimpleFileVisitor<Path>() {

                    override fun visitFile(path: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                        if(path!=null) {
                            val f = path.toFile()
                            if (f.extension == "dm")
                                mappingFiles.put(path.fileName.toString(), f)
                        }
                        return FileVisitResult.CONTINUE
                    }
                })
            }
    })
    }

    private fun preparePathes() {
        // /datamaps/target/classes/
        var path = DmFileStorage::class.java.protectionDomain.codeSource.location.file
        val index = path.lastIndexOf("target/classes")
        if (index > 0) {
            path = path.substring(0, index) + "src"
        }
        if (File(path).exists())
            pathes.add(path)

        pathes = pathes.stream().filter({ f -> File(f).exists() }).collect(Collectors.toList<String>())
    }
}