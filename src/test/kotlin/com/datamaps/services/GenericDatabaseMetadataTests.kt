package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertEqIgnoreCase
import org.testng.Assert
import org.testng.Assert.assertNotNull
import org.testng.annotations.Test
import java.sql.JDBCType
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
        var table = genericDbMetadataService.getTableInfo("JiraWorker")
        println(table)
        Assert.assertEquals(table.name, "JiraWorker".toUpperCase())
        Assert.assertEquals(table.columns.size, 4)

        Assert.assertEquals(table["email"].jdbcType, JDBCType.VARCHAR)
        Assert.assertEquals(table["eMAIL"].size, 50)

        table = genericDbMetadataService.getTableInfo("JiraDepartment")
        println(table)
        Assert.assertEquals(table.name, "JiraDepartment".toUpperCase())
        Assert.assertEquals(table.columns.size, 3)

        Assert.assertEquals(table["name"].jdbcType, JDBCType.VARCHAR)
        Assert.assertEquals(table["name"].comment, "Name of deparment")
    }

    @Test
    public fun testGetImportedKeys()
    {
        var table = genericDbMetadataService.getTableInfo("JiraWorker")

        //JiraWorker имеет одну ссылку M-1: на гендер
        var genderID  = table["genderId"]
        Assert.assertNotNull(genderID)
        Assert.assertNotNull(genderID.importedKey)
        Assert.assertEquals(genderID.importedKey!!.pkTable, "JiraGender".toUpperCase())
        Assert.assertEquals(genderID.importedKey!!.pkColumn, "id".toUpperCase())

        Assert.assertNotNull( genericDbMetadataService.getTableInfo(genderID.importedKey!!
                .pkTable)[genderID.importedKey!!.pkColumn] )


        //JiraDepartment имеет одну ссылку M-1: на JiraDepartment (это же дерево)
        table = genericDbMetadataService.getTableInfo("JiraDepartment")

        var parentId  = table["parentId"]
        Assert.assertNotNull(parentId)
        Assert.assertNotNull(parentId.importedKey)
        Assert.assertEquals(parentId.importedKey!!.pkTable, "JiraDepartment".toUpperCase())
        Assert.assertEquals(parentId.importedKey!!.pkColumn, "id".toUpperCase())

        Assert.assertNotNull( genericDbMetadataService.getTableInfo(parentId.importedKey!!
                .pkTable)[parentId.importedKey!!.pkColumn] )


        //JiraWorker_JiraDepartment имеет 2 ссылки M-1: на JiraWorker и JiraDepartment
        var keys = genericDbMetadataService.getImportedKeys("JiraWorker_JiraDepartment")
        Assert.assertEquals(keys.size, 2)
        Assert.assertTrue(keys.stream().filter({ fk -> fk.pkTable.equals("JiraWorker", true)})
                .findFirst().isPresent)

        Assert.assertTrue(keys.stream().filter({ fk -> fk.pkTable.equals("JiraWorker", true)})
                .findFirst().get().pkColumn.equals("id", true))
        assertNotNull(keys.stream().filter({ fk -> fk.pkTable.equals("JiraDepartment", true)}).findFirst())
        Assert.assertTrue(keys.stream().filter({ fk -> fk.pkTable.equals("JiraDepartment", true)})
                .findFirst().get().pkColumn.equals("id".toUpperCase()))
    }


    @Test
    //тест на получение потенциальных коллекций один ко многим по экспортированным ключам
     fun testGetExportedKeys()
    {
        //JiraGender не имет коллекций
        var table = genericDbMetadataService.getTableInfo("JiraGender")

        Assert.assertTrue(table.oneToManyCollections.size==0)
        Assert.assertTrue(table.exportedKeys.size>1)

        //JiraWorker имеет коллекцию
        table = genericDbMetadataService.getTableInfo("JiraWorker")

        println(table.oneToManyCollections.size)
        Assert.assertTrue(table.oneToManyCollections.size==1)
        Assert.assertTrue(table.exportedKeys.size>1)

        var fk = table.oneToManyCollections[0]
        assertEqIgnoreCase(fk.pkTable, "JiraWorker")
        assertEqIgnoreCase(fk.pkColumn, "ID")
        assertEqIgnoreCase(fk.fkTable, "JiraWorker_JiraDepartment")
        assertEqIgnoreCase(fk.fkColumn, "jiraWorkerId")


        //JiraProject имеет коллекцию
        table = genericDbMetadataService.getTableInfo("JiraProject")

        println(table.oneToManyCollections.size)
        Assert.assertTrue(table.oneToManyCollections.size==1)

        fk = table.oneToManyCollections[0]
        assertEqIgnoreCase(fk.pkTable, "JiraProject")
        assertEqIgnoreCase(fk.pkColumn, "ID")
        assertEqIgnoreCase(fk.fkTable, "JiraTask")
        assertEqIgnoreCase(fk.fkColumn, "jiraProjectId")

    }

}