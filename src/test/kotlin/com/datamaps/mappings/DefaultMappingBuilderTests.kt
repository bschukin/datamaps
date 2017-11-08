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

        Assert.assertEquals(dt.fields.size, 2)
        Assert.assertEquals(dt.fields.stream().map { f -> f.field }.toList(), listOf("ID", "GENDER"))

        Assert.assertEquals(dt.defaultGroup.fields.stream().toList(), listOf("ID", "GENDER"))
    }

    @Test
    //тест на маппинг таблицы в которой есть простое ссылочное поле
    fun testDefaultMappingForimpleTable() {
        var dt = defaultMappingBuilder.buildDefault("JiraWorker")
        assertNotNull(dt)

        Assert.assertEquals(dt.fields.size, 3)
        Assert.assertEquals(dt.fields.stream().map { f -> f.field }.toList(), listOf("ID", "NAME", "EMAIL"))

        Assert.assertEquals(dt.defaultGroup.fields.stream().toList(), listOf("ID", "NAME", "EMAIL"))
    }

}