package com.bftcom.ice.datamaps.tools.dbsync

import com.bftcom.ice.datamaps.dataMapToString
import com.bftcom.ice.datamaps.utils.Date
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.assertEqIgnoreCase
import com.bftcom.ice.datamaps.impl.dialects.DbDialect
import com.bftcom.ice.datamaps.impl.util.shadow
import com.bftcom.ice.IfSpringProfileActive
import com.bftcom.ice.datamaps.tools.Sources
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.support.EncodedResource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.springframework.jdbc.datasource.init.ScriptUtils.*
import org.springframework.test.annotation.Repeat
import java.util.*

/**
 * Created by Щукин on 03.11.2017.
 */
open class DbSyncServiceTests : BaseSpringTests() {

    @Autowired
    lateinit var dbDialect: DbDialect

    //при втором запуске этого теста, без Rollback на DDL мы поимеем ошибки
    @Test
    @Repeat(2)
    @IfSpringProfileActive("postgresql")
    fun testDdlRollbackTable() {
        val sql = "CREATE TABLE \"DDLRollbackTest\"\n" +
                "(\n" +
                "  id bigint NOT NULL PRIMARY KEY" +
                ");"

        val statements = LinkedList<String>()
        val resource = ByteArrayResource(sql.toByteArray())
        ScriptUtils.splitSqlScript(EncodedResource(resource), sql, DEFAULT_STATEMENT_SEPARATOR, DEFAULT_COMMENT_PREFIX,
                DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                DEFAULT_BLOCK_COMMENT_END_DELIMITER, statements)

        statements.forEach {
            jdbcTemplate.execute(it)
        }


    }

    //при втором запуске этого теста, без DDLRollback мы поимеем ошибки
    @Test
    @Repeat(2)
    @IfSpringProfileActive("postgresql")
    fun testDdlRollbackColumn() {

        jdbcTemplate.execute("ALTER TABLE gender ADD COLUMN " +
                " \"GENDERfullName\" character varying(256)")
    }

    @Test
    @Repeat(2)
    @IfSpringProfileActive("postgresql")
    fun testDdlRollbackConstraint() {

        jdbcTemplate.execute("ALTER TABLE city " +
                "ADD constraint \"some_some_fk\" " +
                "FOREIGN KEY (id) REFERENCES gender(id);\n")
    }


    @Test
    @Repeat(2)
    @IfSpringProfileActive("postgresql" /*,"firebird"*/) //только трейти файербирд
    fun testDdlExecute() {

        val sources = applicationContext.autowireCapableBeanFactory.createBean(Sources::class.java)
        sources.dirsToScansScripts = listOf("test-ddl", "test-ddl2")
        val dbSyncService2 = applicationContext.autowireCapableBeanFactory.createBean(PostgresScriptExecutor::class.java)
        dbSyncService2.sources = sources
        dbSyncService2.dbDialect =dbDialect

        val files = dbSyncService2.findAllScriptsInClassPath().map { it.filename }
        assertTrue(files.contains("2017-02-06-09-10.sql"))
        assertTrue(files.contains("2018-02-06-09-10.sql"))
        assertTrue(files.contains("2018-02-06-10-10.sql"))
        assertTrue(files.contains("2019-02-06-10-10.sql"))
        assertTrue(files.contains("2042-02-06-09-10.sql"))


        val scritp1 = DdlScript.shadow { it[fileName] = "2017-02-06-09-10.sql"; it[executed] = Date() }
        val scritp4 = DdlScript.shadow { it[fileName] = "2019-02-06-10-10.sql"; it[executed] = Date() }

        println(scritp1.dataMapToString())
        println(scritp4.dataMapToString())

        val notExecuted = dbSyncService2.findAllNotExecutedScripts(files)
        assertEqIgnoreCase(notExecuted,
                listOf("2018-02-06-09-10.sql", "2018-02-06-10-10.sql", "2042-02-06-09-10.sql"))


        val filesToExec = dbSyncService2.findAllScriptsInClassPath()
                .filter { notExecuted.contains(it.filename) }.toList()
        dbSyncService2.executeAllNotExecutedScripts(filesToExec)


        val innsertedScripts = dataService.findAll(DdlScript.slice()
                .where("{{fileName}} in (:list)")
                .param("list", listOf("2018-02-06-09-10.sql", "2018-02-06-10-10.sql")))
        assertTrue(innsertedScripts.size == 2)

    }

}
