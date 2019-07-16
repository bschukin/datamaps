package com.bftcom.ice.server.datamaps

import com.bftcom.ice.common.maps.Field
import com.bftcom.ice.common.maps.MFS
import com.bftcom.ice.common.maps.StringEnum
import com.bftcom.ice.server.BaseSpringTests
import org.junit.Assert
import org.junit.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
open class EnumTests : BaseSpringTests() {

    @Test
    //тест на чтение из базы записей содержащих енам
    //показывает, что при доступе к полю-инаму через Field и через строку - всегда получим enum
    // (что означает что enum в мапе появится уже при маппинге ResultSet из базы на датамап)
    fun testEnumSimpleRead() {

        val wap = dataService.find_(Film.slice { filter { name eq "War And Peace" } })

        //доступ по полю:
        assertTrue(wap[{ filmType }] == FilmType.Drama)

        //доступ по строчке:
        assertTrue(wap["filmType"] == FilmType.Drama)
    }

    @Test
    //тест показывает что при чтении из базы записи с неизвестным значния енама
    //выпадет ошибка
    fun testEnumUnknownValueInDatabase() {

        try {
            dataService.find_(Film.slice { filter { name eq "The Ring" } })
        } catch (e: Exception) {
            println(e)
            assertTrue(e.message=="enum value for [Unknown--Enum--Value] was not found")
            return
        }

        Assert.fail()
    }

    @Test
    //тест на чтение из базы записей содержащих енам
    //показывает, что при доступе к полю-инаму через Field и через строку - всегда получим enum
    // (что означает что enum в мапе появится уже при маппинге ResultSet из базы на датамап)
    fun testEnumSimpleWrite() {

        //1 читаем
        val wap = dataService.find_(Film.slice { filter { name eq "War And Peace" } })

        assertTrue(wap[{ filmType }] == FilmType.Drama)

        //2 меняем в первый раз
        wap[{ filmType }] = FilmType.Boevik

        dataService.flush()

        //проверяем
        val wap1 = dataService.find_(Film.slice { filter { name eq "War And Peace" } })
        assertTrue(wap1[{ filmType }] == FilmType.Boevik)

        //3. меняем во второй раз
        wap1["filmType"] = FilmType.Drama
        dataService.flush()

        val wap2 = dataService.find_(Film.slice { filter { name eq "War And Peace" } })
        assertTrue(wap2["filmType"] == FilmType.Drama)
    }


    @Test
    //тест на использование полей enumov отличающихся  значением от датабазных значений
    fun testEnumWriteValueOtherTnanName() {

        //1 читаем
        val wap = dataService.find_(Film.slice { filter { name eq "War And Peace" } })

        //2 меняем
        wap[{ filmType2 }] = FilmType2.Boevik2

        dataService.flush()

        //проверяем
        val wap1 = dataService.find_(Film.slice { filter { name eq "War And Peace" } })
        assertTrue(wap1[{ filmType2 }] == FilmType2.Boevik2)


    }


    object Film : MFS<Film>("Film") {
        val name = Field.string("name")
        val filmType = Field.enum("filmType", FilmType.values())
        val filmType2 = Field.enum("filmType2", FilmType2.values())
    }

    enum class FilmType : StringEnum {
        Drama,
        Boevik;
    }

    enum class FilmType2(override val value: String) : StringEnum {
        Drama2("Drama-2"),
        Boevik2("Boevik-2");
    }

}

