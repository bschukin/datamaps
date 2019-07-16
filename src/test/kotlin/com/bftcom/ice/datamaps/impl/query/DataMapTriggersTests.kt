package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.impl.delta.DataMapTriggersRegistry
import com.bftcom.ice.server.*
import com.bftcom.ice.datamaps.impl.util.printAsJson
import org.junit.Assert
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.test.annotation.DirtiesContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Created by Щукин on 03.11.2017.
 */
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
open class DataMapTriggersTests : BaseSpringTests() {
    @Autowired
    lateinit var dataMapTriggersRegistry: DataMapTriggersRegistry


    @Service
    class TestTriggers : DataMapTriggers {
        override val targetEntities: List<String>
            get() = listOf(Person.entity)

        override fun beforeInsert(event: TriggerContext) {
            if (event.delta[Person.name] == "Leo Tolsty") {
                event.delta[Person.lastName] = "Yasnaya Poliana"
            }
        }

        override fun beforeUpdate(event: TriggerContext) {
            if (event.delta[Person.lastName] == "lev") {
                event.delta[Person.lastName] = "Neyasnaya Poliana"
            }
        }
    }

    @Service
    class TestTriggers2 : DataMapTriggers {
        @Autowired
        lateinit var dataService: DataService

        override val targetEntities: List<String>
            get() = listOf(Department.entity)

        override fun beforeInsert(event: TriggerContext) {
            if (event.delta[Department.name] == "Good Department")
                event.delta[Department.name] = "Good Department 2042"
        }

        override fun beforeUpdate(event: TriggerContext) {
            if (event.delta[Department.name] == "Good Department 2043") {
                event.delta[Department.boss] = dataService.find_(Person.withId(2))
                event.delta[Department.name] = "Good Department 2042"
            }

            //для теста на доступ к объекту _new
            //пытаемся изменить READ-ONLY - объект
            if (event.delta[Department.name] == "ГородФёрст") {
                event.new()[Department.name] = null
            }

            //для теста на доступ к объекту _old
            //пытаемся изменить READ-ONLY - объект
            if (event.delta[Department.name] == "ГородЦеконд") {
                assertTrue(event.old()!![Department.name] == "ГородЗеро")
                event.old()!![Department.name] = null
            }
        }

    }

    @Service
    class TestTriggers3 : DataMapTriggers {
        @Autowired
        lateinit var dataService: DataService

        override val targetEntities: List<String>
            get() = listOf(Attach.entity)

        override fun beforeInsert(event: TriggerContext) {
            if (event.delta[Attach.name] == "Good Department") {
                event.delta[Attach.data2().name] = "Строгое наименование а не финтифлюшки"
            }
        }

        override fun beforeUpdate(event: TriggerContext) {
            if (event.delta[Department.name] == "Good Department 2043") {
                event.delta[Department.boss] = dataService.find_(Person.withId(2))
                event.delta[Department.name] = "Good Department 2042"
            }
        }

    }

    @Service
    class TestAllEntitiesTriggers : DataMapTriggers {

        var someCounter = 0

        override val targetEntities: List<String>
            get() = DataMapTriggers.allEntities

        override fun beforeInsert(event: TriggerContext) {
            someCounter++
        }

        override fun beforeUpdate(event: TriggerContext) {
            someCounter++
        }

    }


    // триггер на уничтожение города
    @Service
    class ApocalypseTestTriggers : DataMapTriggers {
        @Autowired
        lateinit var dataService: DataService

        override val targetEntities: List<String>
            get() = listOf(City.entity)

        override fun beforeDelete(event: TriggerContext) {
            val city = dataService.find_(City.on().id((event.delta as DM<City>).id))
            // кейс 1 - уничтожается город City17 - погибают все жители разом
            if (city[{ title }] == "City17") {
//                dataService.deleteAll(Person.filter { f(Person.city().id) eq city.id!! }) // todo
                dataService.deleteAll(Person.on().where ( "city_id = ${city.id}" ))
            }
            // кейс 2 - уничтожается город бердск - жители погибают по отдельности
            if (city[{ title }] == "Berdsk") {
                val alex = dataService.find(Person.on().filter((f(Person.name) like "Алексей") and (f(Person.city().id) eq city.id!!)))!!
                val artem = dataService.find(Person.on().filter((f(Person.name) like "Артем") and (f(Person.city().id) eq city.id!!)))!!
                dataService.delete(alex)
                dataService.delete(artem)
                dataService.flush()
            }
        }

        override fun beforeUpdate(event: TriggerContext) {
            val city = dataService.find_(City.on().id((event.delta as DM<City>).id))
            val boris = dataService.find(Person.on().filter((f(Person.name) like "Борис") and (f(Person.city().id) eq city.id!!)))!!
            boris[{ name }] = "БорисМосковский"
            dataService.flush() // TODO надо как-то прокинуть
        }
    }

    // тест на удаление дельты в триггере
    @Service
    class DeleteDeltaTestTriggers : DataMapTriggers {

        companion object {
            const val UNVALID_FIELD = "UNVALID_FIELD"
        }

        @Autowired
        lateinit var dataService: DataService

        override val targetEntities: List<String>
            get() = listOf(City.entity)

        override fun beforeInsert(event: TriggerContext) {
            if (event.delta.map[UNVALID_FIELD] != null) {
                val unvalid = event.delta.map[UNVALID_FIELD]
                val title = event.delta.map[City.title.n]
                event.delta.map.remove(UNVALID_FIELD)
                event.delta.map[City.title.n] = title.toString() + unvalid.toString()
            }
        }
    }


    @Test
    fun testChangePersonOnInsertAndUpdate() {

        val service2 = applicationContext.getBean(TestAllEntitiesTriggers::class.java)
        val counter = service2.someCounter

        Person.create {
            it[name] = "Leo Tolsty"
            it[email] = "tolstyachok@yandex.ru"
        }

        dataService.flush()

        val leo = dataService.find_(Person.filter { email eq "tolstyachok@yandex.ru" })

        Assert.assertEquals(leo[Person.lastName], "Yasnaya Poliana")
        Assert.assertTrue(counter + 1 <= service2.someCounter)

        leo[Person.lastName] = "lev"
        dataService.flush()

        val leo2 = dataService.find_(Person.filter { email eq "tolstyachok@yandex.ru" })

        Assert.assertEquals(leo2[Person.lastName], "Neyasnaya Poliana")
        Assert.assertTrue(counter + 2 <= service2.someCounter)

    }

    @Test
    fun testDataMapTriggersRegistry() {

        println(dataMapTriggersRegistry.registry)

        //проверяем что созаднные тесты сервиса жизненного цикла
        //находятся в регистре
        assertTrue(dataMapTriggersRegistry.registry[Person.entity]!!
                .map { it::class.java }.firstOrNull { it.equals(TestTriggers::class.java) } != null)
        assertTrue(dataMapTriggersRegistry.registry["allEntities"]!!
                .map { it::class.java }.firstOrNull { it.equals(TestAllEntitiesTriggers::class.java) } != null)

        val cityTriggers = dataMapTriggersRegistry.registry[City.entity]!!.map { it::class.java }
        assertEquals(2,cityTriggers.size)
        assertTrue(cityTriggers[0].equals(ApocalypseTestTriggers::class.java) || cityTriggers[0].equals(DeleteDeltaTestTriggers::class.java))
        assertTrue(cityTriggers[1].equals(ApocalypseTestTriggers::class.java) || cityTriggers[1].equals(DeleteDeltaTestTriggers::class.java))
    }


    @Test
    fun testChangeProjectOnInsertAndUpdate() {

        val d = Department.create {
            it[name] = "Good Department"
        }

        dataService.flush()

        val d1 = dataService.find_(Department.withId(d.id))

        Assert.assertEquals(d1[Department.name], "Good Department 2042")

        d1[{ name }] = "Good Department 2043"
        d1[{ boss }] = dataService.find_(Person.withId(1))
        dataService.flush()

        val d2 = dataService.find_(Department.withId(d.id).withRefs())
        Assert.assertEquals(d2[Department.boss().id], 2)


    }


    @Test
    fun testTriggerrsWithJsonMaps() {

        val d = Attach.create {
            it[name] = "Good Department"
            it[data2] = Attach.AttachData2 {
                it[name] = "someName"
                it[part] = Attach.AttachData2.AttachData3 {
                    it[bar] = "bar1"
                    it[foo] = "foo1"
                }
            }
        }

        dataService.flush()

        val d1 = dataService.find_(Attach.withId(d.id).withBlobs())
        d1.printAsJson()

        Assert.assertEquals(d1[Attach.data2().name], "Строгое наименование а не финтифлюшки")


    }


    @Test
    fun testAccessToNewObjectInContextIsReadonly() {

        val invman = dataService.find_(Department.filter { id eq 6 })
        invman[Department.name] = "ГородФёрст"
        try {
            dataService.flush()
            Assert.fail()
        } catch (e: UnsupportedOperationException) {
            assertTrue(e.message == "cannot modify readonly map")
        }

        /* //val invman2 = dataService.find_(Department.filter { id eq 6 })
         invman[Department.name] = "ГородЦеконд"
         try {
             dataService.flush()
             Assert.fail()
         } catch (e: UnsupportedOperationException) {
             assertTrue(e.message=="cannot modify readonly map")
         }*/

    }


    @Test
    fun testOnDelete() {

        val city17 = City {
            it[id] = 10000
            it[title] = "City17"
        }
        val berdsk = City {
            it[id] = 10001
            it[title] = "Berdsk"
        }
        val moscow = City {
            it[id] = 10002
            it[title] = "MoscowCity"
        }

        dataService.flush()

        Person {
            it[name] = "Андрей"
            it[city] = city17
        }
        Person {
            it[name] = "Александр"
            it[city] = city17
        }

        Person {
            it[name] = "Алексей"
            it[city] = berdsk
        }
        Person {
            it[name] = "Артем"
            it[city] = berdsk
        }

        Person {
            it[name] = "Сергей"
            it[city] = moscow
        }
        val bor = Person {
            it[name] = "Борис"
            it[city] = moscow
        }

        dataService.flush()

        val selectTestPersons = Person.on()
                .where("{{city.title}} IN (:list)")
                .param("list", listOf("Berdsk", "City17", "MoscowCity"))

        Assert.assertEquals(6, dataService.findAll(selectTestPersons).size)

        // кейс 1 - в триггере происходит удаление через dataService.deleteAll, все ОК
        dataService.delete(city17)
        dataService.flush()

        Assert.assertEquals(4, dataService.findAll(selectTestPersons).size)
        Assert.assertEquals(0, dataService.findAll(Person.on().where("{{name}} like 'Андрей' OR {{name}} like 'Александр'")).size)
        // кейс 2 - удаление по отдельности через dataService.delete
        dataService.delete(berdsk)
        dataService.flush()

        Assert.assertEquals(2, dataService.findAll(selectTestPersons).size)
        Assert.assertEquals(0, dataService.findAll(Person.on().where("{{name}} like 'Алексей' OR {{name}} like 'Артем'")).size)


        // кейс 3 - обновление сторонней мапы в триггере
        moscow[{ title }] = "MoscowCity000"
        dataService.flush()

        val bor2 = dataService.find_(Person.on().id(bor.id))
        Assert.assertEquals("БорисМосковский", bor2[{name}])
    }

    @Test
    fun testOnDeltaDelete() {

        val city = City {
            it[id] = 17777
            it[title] = "City18"
            it[DeleteDeltaTestTriggers.UNVALID_FIELD] = " + some unvalid"
        }

        dataService.flush()

        val testDM = dataService.find(on(City).id(city.id))!! // выгружаем обновленную триггером датамапу
        Assert.assertEquals("City18 + some unvalid", testDM[{title}])
        Assert.assertEquals(null, testDM[DeleteDeltaTestTriggers.UNVALID_FIELD])
        Assert.assertTrue(!testDM.map.containsKey(DeleteDeltaTestTriggers.UNVALID_FIELD))
    }

}

