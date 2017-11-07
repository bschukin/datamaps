package com.datamaps.services

import com.datamaps.BaseSpringTests
import org.testng.Assert
import org.testng.annotations.Test
import java.sql.JDBCType
import java.sql.SQLException
import javax.annotation.Resource


/**
 * Created by Щукин on 03.11.2017.
 */
class GenericDatabaseMetadataTests : BaseSpringTests() {

    @Resource
    lateinit var genericDbMetadataService:GenericDbMetadataService;

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
        println(table)
        Assert.assertEquals(table.name, "JiraWorker".toUpperCase())
        Assert.assertEquals(table.columns.size, 4)
        //todo ассерты на форины и каскады
    }

    @Test(invocationCount = 1)
    @Throws(SQLException::class)
    fun testCoonection() {

        var start = System.currentTimeMillis()

        val c = jdbcTemplate.dataSource.connection
        val md = c.metaData
        var start1 = System.currentTimeMillis()
        var start2 = System.currentTimeMillis()

        var crs = md.getImportedKeys(null, "PUBLIC", "JiraWorker".toUpperCase())
        while (crs.next()) {
            print("-==")
            print(crs.getString("PKTABLE_NAME") + " ")
            print(crs.getString("PKCOLUMN_NAME") + " ")
            print(crs.getString("FKTABLE_NAME")+ " ")
            print(crs.getString("FKCOLUMN_NAME").toString() + " ")
            print(crs.getString("FK_NAME").toString() + " ")
            print(crs.getString("PK_NAME") + " ")
            print(crs.getString("UPDATE_RULE") + " ")
            println(crs.getString("DELETE_RULE") + " ")
        }

        println("============================ " + (start1 - start))
        println("============================ " + (start2 - start1))
        println("============================ " + (start2 - start))

    }

}