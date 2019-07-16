package com.bftcom.ice.datamaps.tools.dbsync

import com.bftcom.ice.datamaps.DataService
import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.MappingFieldSet
import com.bftcom.ice.datamaps.misc.Date
import com.bftcom.ice.datamaps.impl.mappings.DataMappingsService
import com.bftcom.ice.datamaps.impl.mappings.TableNameResolver
import com.bftcom.ice.datamaps.impl.dialects.DbDialect
import com.bftcom.ice.datamaps.tools.Sources
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.annotation.PostConstruct


object DdlScript : MappingFieldSet<DdlScript>("DdlScript") {
    val fileName = Field.string("fileName")
    val executed = Field.date("executed")

    override val nativeKey: List<Field<*, *>> by lazy {
        listOf(fileName)
    }
}

/**
 * Интерфейс сервиса, ответственного за механизмы обновления БД (в тч при старте).
 * Для каждой БД должен реализовываться свой сервис,с  учетом имеющихся в базе
 * механизмов DDL (наприме отката транзакций DDL)
 */
interface ScriptExecutor {
    fun findAndExecuteSqlScripts()
    fun executeScripts(sql: String)
    fun markScriptExecuted(file: String)
}

abstract class AbstractScriptExecutor : ScriptExecutor {


    @PostConstruct
    open fun init() {
        findAndExecuteSqlScripts()
    }
}

@Service("scriptExecutor")
@Profile("oracle", "hsqldb")
open class NotExecutingScriptExecutor : AbstractScriptExecutor() {
    override fun findAndExecuteSqlScripts() {
    }

    override fun executeScripts(sql: String) {
    }

    override fun markScriptExecuted(file: String) {
    }

}


@Service("scriptExecutor")
@DependsOn("nameMappingsStrategy")
@Profile("postgresql")
open class PostgresScriptExecutor : AbstractScriptExecutor() {

    companion object {
        val logger = LoggerFactory.getLogger(ScriptExecutor::class.java)
        val version95 = DbDialect.DbVersion(9, 5)
    }

    @Autowired
    lateinit var dataService: DataService

    @Autowired
    lateinit var sources: Sources

    @Autowired
    protected lateinit var dataMappingsService: DataMappingsService

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    protected lateinit var tableNameResolver: TableNameResolver

    @Autowired
    lateinit var dbDialect: DbDialect

    @Transactional
    override fun executeScripts(sql: String) {
        if (sql.isBlank())
            return
        val resource = ByteArrayResource(sql.toByteArray())

        //нельзя использовать ScriptUtils - они выполняются вне транзакции
        //ScriptUtils.executeSqlScript(jdbcTemplate.dataSource.connection, resource)
        //исполняем самоотоятельно

        val statements = LinkedList<String>()
        ScriptUtils.splitSqlScript(EncodedResource(resource), sql, ScriptUtils.DEFAULT_STATEMENT_SEPARATOR, ScriptUtils.DEFAULT_COMMENT_PREFIX,
                ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER, statements)

        statements.forEach {
            val transformedStatement = transformStatementIfShould(it)
            logger.info("\n"+transformedStatement.replace("\r", "\r\n"))
            jdbcTemplate.execute(transformedStatement)
        }
    }

    @Transactional
    override fun findAndExecuteSqlScripts() {
        val files = findAllScriptsInClassPath()
        if (files.isEmpty()) {
            logger.info("no files found in the sync directories")
            return
        }
        val notExecuted = findAllNotExecutedScripts(files.map { it.filename })

        if (notExecuted.isEmpty()) {
            logger.info("!!!! no new script files found in the sync directories")
            return
        }

        val startBanner = """
                UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU
                DB-SYNC STARTED.......

                """
        logger.warn(startBanner)

        val res = executeAllNotExecutedScripts(files.filter { notExecuted.contains(it.filename) })

        val resString = if (res) "\r\n\tDB-SYNC SUCCEED" else "\r\n\tDB-SYNC FAILED (("
        val endBanner = """

               $resString
               UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU
                """
        logger.warn(endBanner)
    }

    internal open fun executeAllNotExecutedScripts(files: List<Resource>): Boolean {
        files.forEach { file ->
            try {
                exeDdlScript(file)
            } catch (e: Exception) {
                logger.error("Error while executing db update script [$file](( ", e)
                throw e
            }
        }
        return true
    }

    open fun exeDdlScript(file: Resource) {
        logger.info("\tdb-sync: execute file [${file.filename}]")

        val sql = file.inputStream.bufferedReader().use { it.readText() }
        val resolvedSql = resolveSqlWithNameStrategy(sql)
        try {
            executeScripts(resolvedSql)
        } catch (e: Exception) {
            logger.error("\tdb-sync: error while execute file [${file.filename}]")
            throw e
        }

        markScriptExecuted(file.filename)
    }

    private fun resolveSqlWithNameStrategy(sql: String): String {
        return tableNameResolver.resolveTableNamesForActiveNameStrategy(sql)
    }

    override fun markScriptExecuted(file: String) {
        dataService.insert(DdlScript.create { it[fileName] = file; it[executed] = Date() }, runTriggers = false)
    }

    internal open fun findAllNotExecutedScripts(files: List<String>): List<String> {

        val mapping = dataMappingsService.getDataMapping(DdlScript.entity)
        val table = mapping.table
        val column = mapping[DdlScript.fileName.n].sqlcolumn

        return findAllNotExecutedScriptsInDb(column, table, files)
    }

    protected open fun findAllNotExecutedScriptsInDb(column: String?, table: String, files: List<String>): List<String> {
        val res = dataService.sqlToFlatMaps(DdlScript.entity,
                "select row_number() over() as id, n as fileName from unnest(array[ :arra ]) n where n  not in " +
                        "(select $column from $table)",
                mapOf("arra" to files))

        return res.map { it[DdlScript.fileName]!! }.toList()
    }

    internal fun findAllScriptsInClassPath(): List<Resource> {
        val resourcePatternResolver = PathMatchingResourcePatternResolver(this::class.java.classLoader)

        return sources.dirsToScansScripts.flatMap {
            resourcePatternResolver.getResources("classpath*:$it/*.sql").toList()
        }.union(
                sources.dirsToScansScripts.flatMap {
                    resourcePatternResolver.getResources("classpath*:$it/" +
                            "${dbDialect.getDbInfo().profile?.toLowerCase()}/*.sql").toList()
                }
        ).sortedBy { it.filename }
    }


    private fun transformStatementIfShould(sql: String): String {
        val statement = sql.replace("\\;",";")

        if (dbDialect.getDbInfo().version != version95)
            return statement

        if (statement.toUpperCase().contains("ADD COLUMN") &&
                statement.toUpperCase().contains("IF NOT EXISTS")) {
            return transformAddColumnIfNotExist(statement)
        }
        return statement
    }

    private fun transformAddColumnIfNotExist(statement: String): String {
        val indexIfTableStart = statement.toUpperCase().indexOf("ALTER TABLE") + "ALTER TABLE ".length
        val indexIfTableEnd = statement.toUpperCase().indexOf("ADD COLUMN")
        val table = statement.substring(indexIfTableStart, indexIfTableEnd).trim()

        val indexIfColumnStart = statement.toUpperCase().indexOf("IF NOT EXISTS") + "IF NOT EXISTS".length
        val columnDef = statement.substring(indexIfColumnStart).trim()

        return """
            DO $$
                BEGIN
                  BEGIN
                    ALTER TABLE $table ADD COLUMN $columnDef;
                    EXCEPTION
                    WHEN duplicate_column THEN RAISE NOTICE 'column already exists in table';
                  END;
                END;
                $$
            """
    }


}

@Service("scriptExecutor")
@DependsOn("nameMappingsStrategy")
@Profile("firebird")
open class FirebirdScriptExecutor : PostgresScriptExecutor() {

    @PostConstruct
    override fun init() {
        checkIfDdlTableExists()
        super.init()
    }

    private fun checkIfDdlTableExists() {

        val ddlTable =dataMappingsService.getTableNameByEntity(DdlScript.entity).toUpperCase()

        var flag = false
        jdbcTemplate.query("select 1 from rdb\$relations where rdb\$relation_name = '$ddlTable'") { _, _ ->
            run {
                flag = true
            }
        }

        if (!flag) {
            executeScripts("""
                    CREATE SEQUENCE ${ddlTable}_SEQ;
                    ALTER SEQUENCE ${ddlTable}_SEQ RESTART WITH 1000;

                    CREATE TABLE ${ddlTable} (
                      id          INT NOT NULL PRIMARY KEY,
                      filename     VARCHAR(200),
                      executed  timestamp
                    );

                """)
            exeDdlScript(ClassPathResource("init/firebird-domains.sql"))
        }
    }

    override fun findAllNotExecutedScriptsInDb(column: String?, table: String, files: List<String>): List<String> {
        val res1 = dataService.findAll(DdlScript.on().with(DdlScript.fileName).order(DdlScript.fileName))
        val r = files.filter { res1.find { indb -> indb[DdlScript.fileName] == it } == null }
        return r
    }
}

