package com.datamaps.mappings

import com.datamaps.BaseSpringTests
import org.testng.Assert
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
        var dt = defaultMappingBuilder.buildDefault("JiraGender")
        assertNotNull(dt)

        Assert.assertEquals(dt.fields.size, 3)
        Assert.assertEquals(dt.fields.values.stream().map { f -> f.name }.toList(), listOf("ID", "GENDER", "ISCLASSIC"))

        Assert.assertEquals(dt.defaultGroup.fields.stream().toList(), listOf("ID", "GENDER", "ISCLASSIC"))
    }


    @Test
    //тест на маппинг таблицы в которой есть простое ссылочное поле
    fun testDefaultMappingForSimpleTable() {
        var dt = defaultMappingBuilder.buildDefault("JiraWorker")
        assertNotNull(dt)

        Assert.assertEquals(dt.fields.size, 4)
        Assert.assertEquals(dt.fields.values.stream().map { f -> f.name }.toList(), listOf("ID", "NAME", "EMAIL", "GENDER"))

        Assert.assertEquals(dt.defaultGroup.fields.stream().toList(), listOf("ID", "NAME", "EMAIL"))
    }

    @Test
            //тест на маппинг таблицы в которой есть простое ссылочное поле
    fun testDefaultMappingForSimpleTable02() {
        val dt = defaultMappingBuilder.buildDefault("JiraStaffUnit")
        assertNotNull(dt)

        println( dt.fields)

        Assert.assertEquals(dt.fields.size, 4)
        Assert.assertEquals(dt.fields.values.stream().map { f -> f.name }.toList(), listOf("ID", "NAME", "WORKER", "GENDER"))

        Assert.assertEquals(dt.defaultGroup.fields.stream().toList(), listOf("ID", "NAME"))
    }

    @Test
            //тест на маппинг таблицы в которой есть простое ссылочное поле
    fun testDefaultMappingForSimpleTable03() {
        var dt = defaultMappingBuilder.buildDefault("JiraDepartment")
        assertNotNull(dt)
        println(dt.fields)
        println(dt.groups)
        Assert.assertEquals(dt.fields.size, 3)
        Assert.assertEquals(dt.fields.values.stream().map { f -> f.name }.toList(), listOf("ID", "NAME", "PARENT"))

        Assert.assertEquals(dt.defaultGroup.fields.stream().toList(), listOf("ID", "NAME"))
    }

}