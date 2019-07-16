package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.MFS
import com.bftcom.ice.datamaps.Temporal
import com.bftcom.ice.datamaps.f
import com.bftcom.ice.datamaps.utils.Date
import com.bftcom.ice.server.BaseSpringTests
import org.junit.Test

/**
 * Created by Щукин on 03.11.2017.
 */
open class TemporalFeaturesTests : BaseSpringTests() {


    /**Погода на месяц*/
    object Weather : MFS<Weather>("Weather", Temporal("startDate", "endDate")) {

        val id = Field.guid()
        val name = Field.stringNN("name")
        val startDate = Field.dateNN("startDate")
        val endDate = Field.dateNN("endDate")
        val days = Field.list("dayWeathers", DayWeather)
    }

    /**Погода на день*/
    object DayWeather : MFS<DayWeather>("DayWeather", Temporal("startDate", "endDate")) {
        val id = Field.guid()
        val name = Field.stringNN("name")
        val startDate = Field.dateNN("startDate")
        val endDate = Field.dateNN("endDate")
    }

    @Test
            /****самый простой тест на темпоральную сущность */
    fun testTemporalSelectOnOneEntity() {
        insertRecords()

        //точка в середине временнго отрезка
        val weather = dataService.find_(Weather
                .onDate(Date(2018, 8, 9))) //погода на 9 августа
        assertTrue(weather[{ name }] == "Погода в августе")

        //точка слева от временного отрезка
        val weather1 = dataService.find(Weather
                .onDate(Date(2017, 8, 9))) //нет погоды
        assertTrue(weather1 == null)

        //точка - начало  временного отрезка
        val weather2 = dataService.find_(Weather
                .onDate(Date(2018, 8, 1))) //погода на 9 августа
        assertTrue(weather2[{ name }] == "Погода в августе")

        //точка - конец первой записи
        val weather3 = dataService.find_(Weather
                .onDate(Date(2018, 8, 31))) //погода на 9 августа
        assertTrue(weather3[{ name }] == "Погода в августе")

        //точка - начало второй записи
        val weather4 = dataService.find_(Weather
                .onDate(Date(2018, 9, 1))) //Погода в cентябре
        assertTrue(weather4[{ name }] == "Погода в cентябре")

        //точка - середина второй записи
        val weather5 = dataService.find_(Weather
                .onDate(Date(2018, 9, 15))) //Погода в cентябре
        assertTrue(weather5[{ name }] == "Погода в cентябре")

        //точка - конец второй записи
        val weather6 = dataService.find_(Weather
                .onDate(Date(2018, 9, 30))) //Погода в cентябре
        assertTrue(weather6[{ name }] == "Погода в cентябре")

        //точка - конец последней записи
        val weather7 = dataService.find(Weather
                .onDate(Date(2042, 1, 1))) //Погода в cентябре
        assertTrue(weather7 == null)
    }


    @Test
            /****тест  показывает что мы можем определять (переопределять) onDate на любых ветках проекции */
    fun testTemporalSelectOnChildEntity() {
        insertRecords()

        //I. определим темпоральность в запросе на уровне детей
        val weather = dataService
                .find_(Weather.slice {
                    +name
                    days {
                        onDate(Date(2018, 8, 6))
                    }
                    filter { name eq "Погода в августе" }
                })

        assertTrue(weather[{ name }] == "Погода в августе")
        assertTrue(weather[{ days }].size == 2)

        //II. определим темпоральность в запросе на уровне родителей, а на уровне детей переопределим

        val weather2 = dataService
                .find_(Weather.slice {
                    onDate(Date(2018, 8, 1))//эта дата работает вместо фильтра в предыдщем запросе
                    +name
                    days {
                        onDate(Date(2018, 8, 6))
                    }
                })

        assertTrue(weather2[{ name }] == "Погода в августе")
        assertTrue(weather2[{ days }].size == 2)
    }



    @Test /****тест на сущность и ее коллекуцию */
    fun testTemporalSelectOnTwoEntities() {
        insertRecords()

        //I. тестим, что у сущности при заданной целевой дате
        //корретно выберутся родитель и дети
        val weather = dataService.find_(Weather
                .onDate(Date(2018, 8, 5))
                .withCollections()) //погода на 5 августа

        assertTrue(weather[{ name }] == "Погода в августе")
        assertTrue(weather[{ days }].size == 1)
        assertTrue(weather[{ days }][0][{ name }] == "Погода в пятый и шестой день августа")


        //I. тестим, что у сущности при заданной целевой дате
        //корретно выберутся родителИ и дети
        val weathers = dataService
                .findAll(Weather.slice {

                    +name
                    days {
                        onDate(Date(2018, 8, 19))
                    }
                    filter { f(startDate) lt Date(2018, 9, 5) }

                }.order(Weather.startDate))


        assertTrue(weathers.size==2)
        assertTrue(weathers[0][{ name }]=="Погода в августе")
        assertTrue(weathers[0][{ days }].size==1)

        assertTrue(weathers[1][{ name }]=="Погода в cентябре")
        assertTrue(weathers[1][{ days }].size==1)

    }

    @Test /**тест на то что, если у нас ни один зависимый объект не попадает в целевую дату
     то по крайней мере-мастер объект вытянется из базы*/
    fun testTemporalSelectOnTwoEntities2() {
        insertRecords()

        //I. тестим, что у сущности при заданной целевой дате
        //корретно выберутся родитель и дети
        val weather = dataService.find_(Weather
                .onDate(Date(2018, 9, 5))
                .withCollections()) //погода на 5 августа

    }





    fun insertRecords() {

        val w1 = Weather {
            it[name] = "Погода в августе"
            it[startDate] = Date(2018, 8, 1)
            it[endDate] = Date(2018, 9, 1) //потому что крайняя точка не включается
            it[days].add(DayWeather {
                it[Weather.name] = "Погода в первый день августа"
                it[Weather.startDate] = Date(2018, 8, 1)
                it[Weather.endDate] = Date(2018, 8, 2)
            })
            it[days].add(DayWeather {
                it[Weather.name] = "Погода в пятый и шестой день августа"
                it[Weather.startDate] = Date(2018, 8, 5)
                it[Weather.endDate] = Date(2018, 8, 7)
            })
            it[days].add(DayWeather {
                it[Weather.name] = "Погода с шестого по 20 августа"
                it[Weather.startDate] = Date(2018, 8, 6)
                it[Weather.endDate] = Date(2018, 8, 21)
            })

        }
        Weather {
            it[name] = "Погода в cентябре"
            it[startDate] = Date(2018, 9, 1)
            it[endDate] = Date(2018, 10, 1)
            it[days].add(DayWeather {
                it[Weather.name] = "Погода 19 августа"        //может ли в сентябре быть августовская погода
                it[Weather.startDate] = Date(2018, 8, 19)
                it[Weather.endDate] = Date(2018, 8, 20)
            })
        }
        Weather {
            it[name] = "Погода потом"
            it[startDate] = Date(2018, 10, 1)
            it[endDate] = Date(2042, 1, 1)
        }

        dataService.flush()
    }


}