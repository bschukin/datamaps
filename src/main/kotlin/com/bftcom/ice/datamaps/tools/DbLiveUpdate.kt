package com.bftcom.ice.datamaps.tools

import com.bftcom.ice.datamaps.misc.throwNotFound
import com.bftcom.ice.datamaps.FieldSet
import com.bftcom.ice.datamaps.common.maps.FieldSetRepo
import com.bftcom.ice.datamaps.tools.dbsync.DbDiffService
import com.bftcom.ice.datamaps.tools.dbsync.DbScriptMaker
import com.bftcom.ice.datamaps.tools.dbsync.ScriptExecutor
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import javax.annotation.Resource


//Отвечает за отслеживание изменений в базе и фиелдсетах
//и синхронизацию этих изменений в ту или другую сторону - (в зависимости от ввыбора разработчика)
@Service
class LiveUpdate {

    companion object {
        private val logger = LoggerFactory.getLogger(LiveUpdate::class.java)
        private val sqlScriptFiledateFormat = SimpleDateFormat("yyyy-MM-dd HH-mm-ss")
    }

    @Autowired
    lateinit var sources: Sources

    @Value("\${ice.dbsync.liveUpdateFile:db-live-update.sql}")
    private var liveUpdateFileName: String = "db-live-update.sql"


    private val pathToPutLiveUpdateScriptFile: File? by lazy {
        initPathToPutLiveUpdateScriptFile()
    }

    private val liveUpdateFile: File by lazy {
        initliveUpdateFile()
    }

    @Resource
    lateinit var gradleAgent: GradleAgent

    @Autowired
    lateinit var dbDiffService: DbDiffService

    @Autowired
    lateinit var dbScriptMaker: DbScriptMaker

    @Autowired
    lateinit var scriptExecutor: ScriptExecutor


    //выполняет последовательность операций:
    //1) перегружает класс FieldSet',
    //2) смотрит разницу между Базой и Фиелдсетом и генерит скрипты на
    //          "догонку" базы до фиелдсета
    //3) записывает сгенеренный скрипт в специальный файл "LiveUpdates.ddl"
    fun updateDatabaseByEntity(entity: String, onlyDiff: Boolean = false, notExecuteScripts: Boolean = false) {
        try {
            //0
            gradleAgent.buildCommonsForJvm()

            //1. ищем фиелдсет
            val fs = findFieldSetByPrefix(entity)
            if (fs == null) {
                logger.warn("entity [$entity] was not found. exit")
                return
            }

            //2 рефрешим фиелдсет (перегружаем класс, чтобы увидеть новые поля)
            logger.info("refreshing entity [${fs::class.java.name}] ...")
            BBAgent.refreshDataMapping(fs::class.java.name)

            logger.info("making diff between the db table and the code ...")

            //3 делаем dbDiff
            val diff = dbDiffService.makeDiffForOneEntity(fs.entity, true)
            println(diff)

            if (!diff.isEmpty() && !onlyDiff) {
                //4 генерим скрипты
                logger.info("generating scripts...")
                val scripts = dbScriptMaker.makeScriptsAndApendToFile(diff, liveUpdateFile, !notExecuteScripts, false)

                logger.info("here are the generated scripts:" + System.lineSeparator() + scripts)

                if(!notExecuteScripts) {
                    //5 выполняем скрипты
                    logger.info("executing scripts...")
                    scriptExecutor.executeScripts(scripts)
                }
            }
            //6 OK
            logger.info("DbLiveUpdate is OK")

        } catch (e: Throwable) {
            logger.error("error", e)
            e.printStackTrace()
        }

    }

    fun  appendToLiveUpdateFile(script:String)
    {
        val header = System.lineSeparator() + "-- appended at [${DbScriptMaker.dateFormat.format(Date())}]" + System.lineSeparator()
        liveUpdateFile.appendText(header + script)
    }

    fun clearLiveUpdateFile(writeOK: Boolean = true) {
        PrintWriter(liveUpdateFile).close()

        if (writeOK)
            logger.info("clear liveUpdateFile is OK")
    }

    //выполняет последовательность операций:
    //1) создает файл скрипта обновления с уникальным именем на основе ткущей даты и времени
    //2) копирует в новый файл скрипта тело файла LiveUpdates.ddl
    //3) помечает в базе разработчика  новый файл скрипта как выполненный
    //4) очищает LiveUpdates.ddl


    fun fixLiveUpdates():String? {

        val name = sqlScriptFiledateFormat.format(Date()) + " live-update.sql"

        if (liveUpdateFile.length() == 0L) {
            logger.warn("The LiveUpdate file is empty. There is nothing to do. Exit")
            return null
        }

        //2 копирует в новый файл скрипта тело файла LiveUpdates.ddl
        val file = File(pathToPutLiveUpdateScriptFile, name)
        FileUtils.copyFile(liveUpdateFile, file)

        //3) помечает в базе разработчика  новый файл скрипта как выполненный
        scriptExecutor.markScriptExecuted(name)
        logger.info("The script [$name] was marked as executed")

        //4) очищает LiveUpdates.ddl
        if (file.length() == liveUpdateFile.length())
            clearLiveUpdateFile()
        else
            logger.warn("it seems the content of $liveUpdateFile was not copied to $file")

        logger.info("fixx LiveUpdates is OK")

        return name
    }


    fun initliveUpdateFile(): File {
        if (liveUpdateFileName.length == 0 || !sources.dotIceDir.exists()) {
            logger.warn("cannot init DbLiveUpdate file. The DbLiveUpdate  will not work")
        }

        val file = File(sources.dotIceDir, liveUpdateFileName)
        if (!file.exists())
            file.createNewFile()
        return file
    }

    fun initPathToPutLiveUpdateScriptFile(): File? {
        val nameDir = if (sources.dirsToScansScripts.isEmpty()) null else sources.dirsToScansScripts[0]

        val f = Files.walk(sources.projectSourcesRoot.toPath()).filter {
            !it.toString().contains("build", true) &&
                    it.fileName.toString().equals(nameDir, true)
        }.findFirst()

        return if (f.isPresent) f.get().toFile() else null

    }

    /**
     * Поиск ентити по начальной строчке наименования. Если найдено несколько сущностей или ни одной -
     * будет выкинута ошибка или вовращен нулл в зависимости от @param throwIfCannotFound
     */
    fun findFieldSetByPrefix(entityPrefix: String, throwIfCannotFound: Boolean = false): FieldSet? {
        val list = FieldSetRepo.fieldSets().filter {
            it.startsWith(entityPrefix, true)
        }

        if (list.isEmpty()) {
            if (throwIfCannotFound)
                throwNotFound("no entity [$entityPrefix] found")
            return null
        }
        if (list.size > 1) {
            val exact = list.firstOrNull() { entityPrefix == it }
            if (exact != null)
                return FieldSetRepo.fieldSet(exact)

            if (throwIfCannotFound)
                throwNotFound("more than 1 entities satisfying [$entityPrefix] found: $list")
            return null
        }
        return FieldSetRepo.fieldSet(list[0])

    }

}