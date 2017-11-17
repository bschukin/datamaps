package com.datamaps.mappings

import com.datamaps.BaseSpringTests
import com.datamaps.assertEqIgnoreCase
import org.testng.Assert
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNotNull
import org.testng.annotations.Test
import javax.annotation.Resource
import kotlin.streams.toList

/**
 * Created by Щукин on 07.11.2017.
 */
class DefaultMappingBuilderTests : BaseSpringTests() {

    @Resource
    lateinit var defaultMappingBuilder: DefaultMappingBuilder

    @Test
            //тест на маппинг таблицы в которой нет ссылочных полей
    fun testDefaultMappingForVerySimpleTable() {
        val dt = defaultMappingBuilder.buildDefault("JIRA_GENDER")
        assertNotNull(dt)
        assertEquals(dt.name, "JiraGender")
        assertEquals(dt.table, "JIRA_GENDER")
        Assert.assertEquals(dt.fields.size, 3)
        Assert.assertEquals(dt.fields.values.stream().map { f -> f.name }.toList(), listOf("id", "gender", "isClassic"))

        Assert.assertEquals(dt.defaultGroup.fields.stream().toList(), listOf("id", "gender", "isClassic"))
    }


    @Test
    //тест на маппинг таблицы в которой есть простое ссылочное поле
    fun testDefaultMappingForSimpleTable() {
        val dt = defaultMappingBuilder.buildDefault("Jira_Worker")
        assertNotNull(dt)

        Assert.assertEquals(dt.fields.size, 5)
        assertEqIgnoreCase(dt.fields.values.stream().map { f -> f.name }.toList(),
                listOf("ID", "NAME", "EMAIL", "GENDER", "jiraWorkerJiraDepartments"))

        assertEqIgnoreCase(dt.defaultGroup.fields.stream().toList(), listOf("ID", "NAME", "EMAIL"))
    }

    @Test
            //тест на маппинг таблицы в которой есть простое ссылочное поле
    fun testDefaultMappingForSimpleTable02() {
        val dt = defaultMappingBuilder.buildDefault("JIRA_STAFF_UNIT")
        assertNotNull(dt)

        println( dt.fields)

        Assert.assertEquals(dt.fields.size, 4)
        assertEqIgnoreCase(dt.fields.values.stream().map { f -> f.name }.toList(), listOf("id", "name", "worker", "gender"))

        assertEqIgnoreCase(dt.defaultGroup.fields.stream().toList(), listOf("id", "name"))
    }

    @Test
            //тест на маппинг таблицы в которой есть простое ссылочное поле
    fun testDefaultMappingForSimpleTable03() {
        val dt = defaultMappingBuilder.buildDefault("JIRA_DEPARTMENT")
        assertNotNull(dt)
        println(dt.fields)
        println(dt.groups)
        Assert.assertEquals(dt.fields.size, 3)
        assertEqIgnoreCase(dt.fields.values.stream().map { f -> f.name }.toList(), listOf("id", "name", "parent"))

        assertEqIgnoreCase(dt.defaultGroup.fields.stream().toList(), listOf("id", "name"))
    }

}