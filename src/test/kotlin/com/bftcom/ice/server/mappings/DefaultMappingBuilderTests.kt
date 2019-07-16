package com.bftcom.ice.server.mappings

import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Game
import com.bftcom.ice.server.assertEqIgnoreCase
import com.bftcom.ice.server.datamaps.mappings.DefaultMappingBuilder
import org.junit.Assert
import org.junit.Test
import java.sql.JDBCType
import javax.annotation.Resource
import kotlin.streams.toList

/**
 * Created by Щукин on 07.11.2017.
 */
open class DefaultMappingBuilderTests : BaseSpringTests() {

    @Resource
    lateinit var defaultMappingBuilder: DefaultMappingBuilder

    @Test
            //тест на маппинг таблицы в которой нет ссылочных полей
    fun testDefaultMappingForVerySimpleTable() {
        val dt = defaultMappingBuilder.buildDefault("GENDER")
        assertNotNull(dt)
        assertEqIgnoreCase(dt.name, "Gender")
        assertEqIgnoreCase(dt.table, "GENDER")
        Assert.assertEquals(dt.fields.size, 3)
        assertEqIgnoreCase(dt.fields.values.stream().map { f -> f.name }.toList(), listOf("id", "name", "isClassic"))

        assertEqIgnoreCase(dt.defaultGroup.fields.stream().toList(), listOf("id", "name", "isClassic"))
        println(dt.fields)
    }


    @Test
    //тест на маппинг таблицы в которой есть простое ссылочное поле
    fun testDefaultMappingForSimpleTable() {
        val dt = defaultMappingBuilder.buildDefault("Person")
        assertNotNull(dt)

        Assert.assertEquals(dt.fields.size, 12)
        assertEqIgnoreCase(dt.fields.values.stream().map { f -> f.name }.toList(),
                listOf("ID", "NAME", "EMAIL", "LASTNAME", "AGE","BIO", "PHOTO", "GENDER","CITY",
                        "FAVORITEGAME", "CHILDS","WORKERDEPARTMENTS"))

        assertEqIgnoreCase(dt.fullGroup.fields.stream().toList(), listOf("ID", "NAME", "EMAIL", "LASTNAME", "AGE",
                "GENDER","CITY", "FAVORITEGAME", "CHILDS","WORKERDEPARTMENTS"))
        assertEqIgnoreCase(dt.defaultGroup.fields.stream().toList(), listOf("ID", "NAME", "EMAIL","LASTNAME","AGE"))
        assertEqIgnoreCase(dt.refsGroup.fields.stream().toList(), listOf("GENDER", "CITY", "FAVORITEGAME"))
        assertEqIgnoreCase(dt.listsGroup.fields.stream().toList(), listOf("CHILDS", "WORKERDEPARTMENTS"))
        assertEqIgnoreCase(dt.blobsGroup.fields.stream().toList(), listOf("BIO", "PHOTO"))
    }

    @Test
            //тест на маппинг таблицы в которой есть простое ссылочное поле
    fun testDefaultMappingForSimpleTable02() {
        val dt = defaultMappingBuilder.buildDefault("Child")
        assertNotNull(dt)

        println( dt.fields)

        Assert.assertEquals(dt.fields.size, 3)
        assertEqIgnoreCase(dt.fields.values.stream().map { f -> f.name }.toList(), listOf("id", "name", "person"))

        assertEqIgnoreCase(dt.defaultGroup.fields.stream().toList(), listOf("id", "name"))
    }

    @Test
            //тест на маппинг таблицы в которой есть простое ссылочное поле
    fun testDefaultMappingForSimpleTable03() {
        val dt = defaultMappingBuilder.buildDefault("DEPARTMENT")
        assertNotNull(dt)
        dt.fields.forEach{f-> println(f.value)}
        println(dt.groups)
        Assert.assertEquals(dt.fields.size, 6)
        assertEqIgnoreCase(dt.fields.values.stream().map { f -> f.name }.toList(), listOf("id", "name", "parent","city","boss", "departments"))

        assertEqIgnoreCase(dt.defaultGroup.fields.stream().toList(), listOf("id", "name"))
    }

    @Test
    //тест на маппинг таблицы в которой есть один ко многоим
    fun testDefaultMappingForSimpleTable04() {
        val dt = defaultMappingBuilder.buildDefault("PROJECT")
        assertNotNull(dt)
        dt.print()
    }


    @Test
    //Из колонки с именем вида [myColumnId] не надо вытирать ппостфикс ID
    //если колонка не является ключом или ссылкой
    //(например externalSystemId)
    fun testPostfixIdShouldnotBeErasedWhenNoReferenceExists() {
        val dt = defaultMappingBuilder.buildDefault("GAME")
        if (isOracle()) {
            //хотя в скрипте metacriticId с типом INTEGER, в БД упорно создается с типом NUMBER
            assertTrue(dt[Game.metacriticId.n].jdbcType== JDBCType.DECIMAL)
        }else{
            assertTrue(dt[Game.metacriticId.n].jdbcType== JDBCType.INTEGER)
        }

    }

}