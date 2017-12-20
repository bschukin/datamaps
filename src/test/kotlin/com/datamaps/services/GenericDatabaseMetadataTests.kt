package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.assertEqIgnoreCase
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.testng.Assert
import org.testng.annotations.Test
import java.sql.*
import javax.annotation.Resource




/**
 * Created by Щукин on 03.11.2017.
 */
class GenericDatabaseMetadataTests : BaseSpringTests() {

    @Resource
    lateinit var genericDbMetadataService:DbMetadataService;


    @Test
    public fun testGetTableInfo()
    {
        var table = genericDbMetadataService.getTableInfo("Jira_Worker")
        println(table)
        assertBodyEquals(table.name, "Jira_Worker")
        Assert.assertEquals(table.columns.size, 4)

        Assert.assertEquals(table["email"].jdbcType, JDBCType.VARCHAR)
        Assert.assertEquals(table["eMAIL"].size, 50)

        table = genericDbMetadataService.getTableInfo("Jira_Department")
        println(table)
        assertBodyEquals(table.name, "Jira_Department".toUpperCase())
        Assert.assertEquals(table.columns.size, 3)

        Assert.assertEquals(table["name"].jdbcType, JDBCType.VARCHAR)
        assertBodyEquals(table["name"].comment!!, "Name of deparment")
    }

    @Test
    public fun testGetImportedKeys()
    {
        var table = genericDbMetadataService.getTableInfo("Jira_Worker")

        //JiraWorker имеет одну ссылку M-1: на гендер
        var genderID  = table["gender_Id"]
        Assert.assertNotNull(genderID)
        Assert.assertNotNull(genderID.importedKey)
        assertEqIgnoreCase(genderID.importedKey!!.pkTable, "Jira_Gender")
        assertEqIgnoreCase(genderID.importedKey!!.pkColumn, "id")

        Assert.assertNotNull( genericDbMetadataService.getTableInfo(genderID.importedKey!!
                .pkTable)[genderID.importedKey!!.pkColumn] )


        //JiraDepartment имеет одну ссылку M-1: на JiraDepartment (это же дерево)
        table = genericDbMetadataService.getTableInfo("Jira_Department")

        var parentId  = table["parent_id"]
        Assert.assertNotNull(parentId)
        Assert.assertNotNull(parentId.importedKey)
        assertEqIgnoreCase(parentId.importedKey!!.pkTable, "Jira_Department")
        assertEqIgnoreCase(parentId.importedKey!!.pkColumn, "id")

        Assert.assertNotNull( genericDbMetadataService.getTableInfo(parentId.importedKey!!
                .pkTable)[parentId.importedKey!!.pkColumn] )


        //JiraWorker_JiraDepartment имеет 2 ссылки M-1: на JiraWorker и JiraDepartment
        var keys = genericDbMetadataService.getImportedKeys("Jira_Worker_Jira_Department")
        Assert.assertEquals(keys.size, 2)
        Assert.assertTrue(keys.stream().filter({ fk -> fk.pkTable.equals("Jira_Worker", true)})
                .findFirst().isPresent)

        Assert.assertTrue(keys.stream().filter({ fk -> fk.pkTable.equals("Jira_Worker", true)})
                .findFirst().get().pkColumn.equals("id", true))
        assertNotNull(keys.stream().filter({ fk -> fk.pkTable.equals("Jira_Department", true)}).findFirst())
        Assert.assertTrue(keys.stream().filter({ fk -> fk.pkTable.equals("Jira_Department", true)})
                .findFirst().get().pkColumn.equals("id", true))
    }


    @Test
    //тест на получение потенциальных коллекций один ко многим по экспортированным ключам
     fun testGetExportedKeys()
    {
        //JiraGender не имет коллекций
        var table = genericDbMetadataService.getTableInfo("Jira_Gender")

        Assert.assertTrue(table.oneToManyCollections.size==0)
        Assert.assertTrue(table.exportedKeys.size>1)

        //JiraWorker имеет коллекцию
        table = genericDbMetadataService.getTableInfo("Jira_Worker")

        println(table.oneToManyCollections.size)
        Assert.assertTrue(table.oneToManyCollections.size==1)
        Assert.assertTrue(table.exportedKeys.size>1)

        var fk = table.oneToManyCollections[0]
        assertEqIgnoreCase(fk.pkTable, "Jira_Worker")
        assertEqIgnoreCase(fk.pkColumn, "ID")
        assertEqIgnoreCase(fk.fkTable, "jira_worker_jira_department")
        assertEqIgnoreCase(fk.fkColumn, "JIRA_WORKER_Id")


        //JiraProject имеет коллекцию
        table = genericDbMetadataService.getTableInfo("Jira_Project")

        println(table.oneToManyCollections.size)
        Assert.assertTrue(table.oneToManyCollections.size==2)

        fk = table.oneToManyCollections[0]
        assertEqIgnoreCase(fk.pkTable, "Jira_Project")
        assertEqIgnoreCase(fk.pkColumn, "ID")
        assertEqIgnoreCase(fk.fkTable, "Jira_Task")
        assertEqIgnoreCase(fk.fkColumn, "jira_Project_Id")

    }

    @Test(invocationCount = 0)
    fun testJdbcReturingn()
    {
        val sql = "INSERT INTO JIRA_GENDER (GENDER) VALUES('100') RETURNING ID"
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

}