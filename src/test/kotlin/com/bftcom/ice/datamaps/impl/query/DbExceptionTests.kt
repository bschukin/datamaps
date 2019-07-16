package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.DataMap
import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.MFS
import com.bftcom.ice.assertBodyEquals
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.IfSpringProfileActive
import com.bftcom.ice.datamaps.misc.DbUniqueConstraintException
import org.junit.Test

/**
 * Created by Щукин on 03.11.2017.
 */
open class DbExceptionTests : BaseSpringTests() {


    object Hand : MFS<Hand>("Hand", caption = "Рука") {
        val id = Field.id()
        val name = Field.string("name")
        val name2 = Field.string("name2") { caption = "Имя2" }
        val name3 = Field.string("name3") { caption = "Имя3" }
    }

    object Finger : MFS<Finger>("Finger", caption = "Палец") {
        val id = Field.id()
        val name = Field.string("name")
    }

    object Hand2 : MFS<Hand2>("Hand2", caption = "Рука") {
        val id = Field.id()
        val name = Field.string("name")

    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testMessageOfUniqueExceptionWithoutFieldSet() {

        //создаем две одинаковых мапы без фиелдсета,
        //смотрим, какое  сообщение мы выкинем
        DataMap("Hand3", 1, isNew = true)
        DataMap("Hand3", 1, isNew = true)

        try {
            dataService.flush()
        } catch (e: DbUniqueConstraintException) {
            println(e.message)
            assertBodyEquals(e.message!!,
                    "Нарушено условие уникальности поля. Таблица: [Hand3]. Поле: [id]. Значение уже существует: [1]")
        }
    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testMessageOfUniqueException() {
        Hand {
            it[id] = 105
            it[name] = "xxx"
        }
        Hand {
            it[id] = 106
            it[name] = "xxx"
        }

        try {
            dataService.flush()
        } catch (e: Exception) {

            assertBodyEquals(e.message!!,
                    "Нарушено условие уникальности поля. Таблица: [Рука]. Поле: [name]. Значение уже существует: [xxx]")
        }
    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testMessageOfUniqueExceptionOnPrimaryKey() {
        Hand2 {
            it[id] = 100
            it[name] = "xxx"
        }
        Hand2 {
            it[id] = 100
            it[name] = "zzzz"
        }

        try {
            dataService.flush()
        } catch (e: Exception) {

            assertBodyEquals(e.message!!,
                    "Нарушено условие уникальности поля. Таблица: [Рука]. Поле: [id]. Значение уже существует: [100]")
        }
    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testUniqueExceptionOnMultiFields() {
        Hand {
            it[id] = 100
            it[name2] = "xxx"
            it[name3] = "xxx"
        }
        Hand {
            it[id] = 101
            it[name2] = "xxx"
            it[name3] = "xxx"
        }

        try {
            dataService.flush()
        } catch (e: Exception) {

            assertBodyEquals(e.message!!,
                    "Нарушено условие уникальности поля. Таблица: [Рука]. Поле: [Имя2, Имя3]. Значение уже существует: [xxx, xxx]")
        }
    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testForeignKeyException() {
        println(Finger)

        try {
            dataService.deleteAll(Hand.on())
        } catch (e: Exception) {
            assertBodyEquals(e.message!!,
                    "Операция обновления в таблице [Рука] нарушает целостность. " +
                            " На запись с ключом [id]=(1) есть ссылки из таблицы [Палец] (поле [hand_id])")
        }
    }

}