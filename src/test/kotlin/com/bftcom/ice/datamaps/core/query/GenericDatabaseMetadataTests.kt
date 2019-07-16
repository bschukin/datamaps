package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.assertBodyEquals
import com.bftcom.ice.server.assertEqIgnoreCase
import com.bftcom.ice.IfSpringProfileActive
import com.bftcom.ice.datamaps.core.mappings.DbMetadataService
import org.junit.Assert
import org.junit.Test
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.support.GeneratedKeyHolder
import java.sql.*
import javax.annotation.Resource


/**
 * Created by Щукин on 03.11.2017.
 */
open class GenericDatabaseMetadataTests : BaseSpringTests() {

    @Resource
    lateinit var genericDbMetadataService: DbMetadataService;


    @Test
    fun testGetTableInfo() {
        var table = genericDbMetadataService.getTableInfo("Person")
        println(table)
        assertBodyEquals(table.name, "Person")

        if (!isOracle())
            assertBodyEquals(table.comment!!, "We are the robots")

        Assert.assertEquals(table.columns.size, 10)

        Assert.assertEquals(table["email"].jdbcType, JDBCType.VARCHAR)
        Assert.assertEquals(table["eMAIL"].size, 50)

        table = genericDbMetadataService.getTableInfo("Department")
        println(table)
        assertBodyEquals(table.name, "Department".toUpperCase())
        Assert.assertEquals(table.columns.size, 5)

        Assert.assertEquals(table["name"].jdbcType, JDBCType.VARCHAR)
        if (!isOracle())
            assertBodyEquals(table["name"].comment!!, "Name of deparment")
    }

    @Test
    fun testGetImportedKeys() {
        var table = genericDbMetadataService.getTableInfo("Person")

        //Person имеет одну ссылку M-1: на гендер
        val genderID = table["gender_Id"]
        Assert.assertNotNull(genderID)
        Assert.assertNotNull(genderID.importedKey)
        assertEqIgnoreCase(genderID.importedKey!!.pkTable, "Gender")
        assertEqIgnoreCase(genderID.importedKey!!.pkColumn, "id")

        Assert.assertNotNull(genericDbMetadataService.getTableInfo(genderID.importedKey!!
                .pkTable)[genderID.importedKey!!.pkColumn])


        //Department имеет одну ссылку M-1: на Department (это же дерево)
        table = genericDbMetadataService.getTableInfo("Department")

        val parentId = table["parent_id"]
        Assert.assertNotNull(parentId)
        Assert.assertNotNull(parentId.importedKey)
        assertEqIgnoreCase(parentId.importedKey!!.pkTable, "Department")
        assertEqIgnoreCase(parentId.importedKey!!.pkColumn, "id")

        Assert.assertNotNull(genericDbMetadataService.getTableInfo(parentId.importedKey!!
                .pkTable)[parentId.importedKey!!.pkColumn])


        //Person_Department имеет 2 ссылки M-1: на Person и Department
        val keys = genericDbMetadataService.getImportedKeys("Worker_Department")
        Assert.assertEquals(keys.size, 2)
        Assert.assertTrue(keys.stream().filter({ fk -> fk.pkTable.equals("Person", true) })
                .findFirst().isPresent)

        Assert.assertTrue(keys.stream().filter({ fk -> fk.pkTable.equals("Person", true) })
                .findFirst().get().pkColumn.equals("id", true))
        assertNotNull(keys.stream().filter({ fk -> fk.pkTable.equals("Department", true) }).findFirst())
        Assert.assertTrue(keys.stream().filter({ fk -> fk.pkTable.equals("Department", true) })
                .findFirst().get().pkColumn.equals("id", true))
    }


    @Test
    //тест на получение потенциальных коллекций один ко многим по экспортированным ключам
    fun testGetExportedKeys() {
        //Gender не имет коллекций
        var table = genericDbMetadataService.getTableInfo("Gender")

        Assert.assertTrue(table.oneToManyCollections.size == 0)
        Assert.assertEquals(table.exportedKeys.size, 1)

        //Person имеет коллекцию
        table = genericDbMetadataService.getTableInfo("Person")

        println(table.oneToManyCollections.size)
        Assert.assertTrue(table.oneToManyCollections.size == 2)
        Assert.assertTrue(table.exportedKeys.size > 1)

        var fk = table.oneToManyCollections[0]
        assertEqIgnoreCase(fk.pkTable, "Person")
        assertEqIgnoreCase(fk.pkColumn, "ID")
        assertEqIgnoreCase(fk.fkTable, "child")
        assertEqIgnoreCase(fk.fkColumn, "person_Id")

        fk = table.oneToManyCollections[1]
        assertEqIgnoreCase(fk.pkTable, "Person")
        assertEqIgnoreCase(fk.pkColumn, "ID")
        assertEqIgnoreCase(fk.fkTable, "worker_department")
        assertEqIgnoreCase(fk.fkColumn, "worker_Id")

        //Project имеет коллекцию
        table = genericDbMetadataService.getTableInfo("Project")

        println(table.oneToManyCollections.size)
        Assert.assertTrue(table.oneToManyCollections.size == 2)

        fk = table.oneToManyCollections.stream().filter { fkk -> fkk.fkTable.equals("Task", true) }
                .findFirst().orElse(null)
        assertEqIgnoreCase(fk.pkTable, "Project")
        assertEqIgnoreCase(fk.pkColumn, "ID")
        assertEqIgnoreCase(fk.fkTable, "Task")
        assertEqIgnoreCase(fk.fkColumn, "Project_Id")

    }

    //@Test
    fun testJdbcReturingn() {
        val sql = "INSERT INTO GENDER (GENDER) VALUES('100') RETURNING ID"
        val holder = GeneratedKeyHolder()

        jdbcTemplate.update(object : PreparedStatementCreator {

            @Throws(SQLException::class)
            override fun createPreparedStatement(connection: Connection): PreparedStatement {
                val ps = connection.prepareStatement(sql,
                        Statement.RETURN_GENERATED_KEYS)
                return ps
            }
        }, holder)

        println(holder.key)
    }

    @Test
    @IfSpringProfileActive("postgresql") //TODO: разобраться почему для оракла не пашет
    fun testGetTableUniqueIndexes() {

        val table = genericDbMetadataService.getTableInfo("Hand")
        assertBodyEquals(table.name, "Hand")

        assertTrue(table.uniqueIndexes.size == 3)

        assertTrue(table.uniqueIndexes.find { it.name == "constraint_xxx" }!!.columns.size == 2)
    }

}