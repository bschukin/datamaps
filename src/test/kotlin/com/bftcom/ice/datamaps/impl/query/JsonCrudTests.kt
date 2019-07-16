package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.DeltaStore
import com.bftcom.ice.datamaps.DynamicEntity
import com.bftcom.ice.datamaps.DynamicFieldSet
import com.bftcom.ice.datamaps.projection
import com.bftcom.ice.datamaps.misc.Date
import com.bftcom.ice.server.Attach
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Person
import com.bftcom.ice.datamaps.impl.util.dataMapFromJson
import com.bftcom.ice.datamaps.impl.util.printAsJson
import com.bftcom.ice.datamaps.impl.util.toJson
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Created by Щукин on 03.11.2017.
 */
open class JsonCrudTests : BaseSpringTests() {

    ///тесты используют несколько
    // INSERT INTO ATTACH ...
    // из файла create-test-db-postgresql.sql

    @Test
    fun testReadJson() {
        val dat = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test04" }
                })

        dat.printAsJson(writeSystemProps = false)

        assertEquals(dat["data.name"], "hello")
        assertEquals(dat["data.value"], 100500)
        assertEquals(dat["data.bool"], true)
    }

    @Test
    fun testReadJsonWithSpecifiedDataMapForJson() {

        val data = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test05" }
                })


        assertEquals(data[{ data2().name }], "hella")
        assertEquals(data[{ data2().part().foo }], "unknown")
        assertEquals(data[{ data2().part().bar }], "314")
    }

    @Test
    fun testUpgradeJson() {
        val dat = dataService
                .find_(Attach.slice {
                    filter { name eq "test04" }
                })

        dat.printAsJson(writeSystemProps = false)
        assertEquals(dat["data"], null)

        val res = dataService.upgrade(listOf(dat), Attach.slice { withBlobs() })
        val dat2 = res[0]
        dat2.printAsJson(writeSystemProps = false)
        assertEquals(dat["data.name"], "hello")
        assertEquals(dat["data.value"], 100500)
        assertEquals(dat["data.bool"], true)
    }


    @Test
    fun testInsertJsonIntoExistingRow() {
        val dat = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test02" }
                })

        Assert.assertTrue(DeltaStore.isEmpty())

        dat["data"] = DynamicEntity.create {
            it["menu"] = DynamicEntity.create {
                it["popup"] = DynamicEntity.create {
                    it["menuitem"] = listOf(
                            DynamicEntity.create {
                                it["value"] = "New"
                                it["onclick"] = "CreateNewBlock()"
                            },
                            DynamicEntity.create {
                                it["value"] = "Open"
                                it["onclick"] = "CreateNewSock()"
                            })
                }
            }
        }
        dat.printAsJson(false)

        dataService.flush()

        val dat2 = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test02" }
                })
        dat2.printAsJson(writeSystemProps = false)
        assertEquals(dat2["data.menu.popup.menuitem[0].onclick"], "CreateNewBlock()")
        /* assertEquals(dat2["data.menu.popup.menuitem[1].onclick"], "CreateNewSock()")*/
    }

    @Test
    fun testInsertRowWithJsonFromScratch() {


        val attach = Attach.create { it[name] = "test03" }

        attach["data"] = DynamicEntity.create {
            it["menu"] = DynamicEntity.create {
                it["popup"] = DynamicEntity.create {
                    it["menuitem"] = listOf(
                            DynamicEntity.create {
                                it["value"] = "New"
                                it["onclick"] = "CreateNewBlock()"
                            },
                            DynamicEntity.create {
                                it["value"] = "Open"
                                it["onclick"] = "CreateNewSock()"
                            })
                }
            }
        }
        attach.printAsJson(false)

        dataService.flush()

        val dat2 = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test03" }
                })
        dat2.printAsJson(writeSystemProps = false)

        assertEquals(dat2["data.menu.popup.menuitem[0].onclick"], "CreateNewBlock()")
        assertEquals(dat2["data.menu.popup.menuitem[1].onclick"], "CreateNewSock()")
    }


    @Test
    fun testReadAndUpdateInnerJson() {
        val dat = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test01" }
                })

        Assert.assertTrue(DeltaStore.isEmpty())

        //1. UPDATE изменяем пару вложенных значений
        dat("data")!!("menu")!!("popup")!!.list("menuitem")[0]["onclick"] = "CreateNewBlock()"
        dat["data.menu.popup.menuitem[1].onclick"] = "CreateNewSock()"

        dat.printAsJson(writeSystemProps = true)

        dataService.flush()

        //проверяем что сохранилось
        val dat2 = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test01" }
                })
        dat2.printAsJson(writeSystemProps = false)

        assertEquals(dat2["data.menu.popup.menuitem[0].onclick"], "CreateNewBlock()")
        assertEquals(dat2["data.menu.popup.menuitem[1].onclick"], "CreateNewSock()")
    }

    @Test
    fun testReadAndAddNewProperty() {
        val dat = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test01" }
                })


        //1. добавляем новое свойство
        dat["data.value"] = "newValue"

        dat.printAsJson(writeSystemProps = true)

        dataService.flush()

        //проверяем что сохранилось
        val dat2 = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test01" }
                })
        dat2.printAsJson(writeSystemProps = false)

        assertEquals(dat2["data.value"], "newValue")
    }


    @Test
    fun testReadAndUpdateInnerJsonAddToList() {
        val dat = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test01" }
                })


        //1. ADD TO LIST
        dat("data.menu.popup")!!.list("menuitem")
                .add(DynamicEntity.create {
                    it["value"] = "Search"
                    it["onclick"] = "SearchDb()"
                })

        dat.printAsJson(writeSystemProps = true)

        dataService.flush()

        //проверяем что сохранилось
        val dat2 = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test01" }
                })
        dat2.printAsJson(writeSystemProps = false)

        assertEquals(dat2("data.menu.popup")!!.list("menuitem").size, 4)
        assertEquals(dat2["data.menu.popup.menuitem[0].onclick"], "CreateNewDoc()")
        assertEquals(dat2["data.menu.popup.menuitem[1].onclick"], "OpenDoc()")
        assertEquals(dat2["data.menu.popup.menuitem[3].value"], "Search")
        assertEquals(dat2["data.menu.popup.menuitem[3].onclick"], "SearchDb()")
    }

    @Test
    fun testReadAndUpdateInnerJsonRemoveFromList() {
        val dat = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test01" }
                })


        //1. ADD TO LIST
        dat("data.menu.popup")!!.list("menuitem").removeAt(0)

        dataService.flush()

        //проверяем что сохранилось
        val dat2 = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test01" }
                })
        dat2.printAsJson(writeSystemProps = false)

        assertEquals(dat2("data.menu.popup")!!.list("menuitem").size, 2)
        assertEquals(dat2["data.menu.popup.menuitem[0].onclick"], "OpenDoc()")
        assertEquals(dat2["data.menu.popup.menuitem[1].onclick"], "CloseDoc()")
    }

    @Test
    fun testReadAndUpdateInnerJsonDeleteInnerNode() {
        val dat = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test01" }
                })


        //1. NULL
        dat["data.menu.popup"] = null

        dataService.flush()

        //проверяем что сохранилось
        val dat2 = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test01" }
                })
        dat2.printAsJson(writeSystemProps = false)

        assertEquals(dat2["data.menu.popup"], null)
    }

    @Test
    fun testReadAndUpdateDates() {
        val dat = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test04" }
                })

        //1. DATE

        dat["data.date"] = java.util.Date()

        dataService.flush()

        //проверяем что сохранилось
        val dat2 = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test04" }
                })
        dat2.printAsJson(writeSystemProps = false)
        assertTrue(dat2["data.date"] is Date)
    }

    @Test
    fun testReadAndUpdateInt() {
        val dat = dataService
                .find_(Person.withId(1))
        dat[Person.age] = 100
        println(dat.toJson(false))
        val dataMapFromJson = dataMapFromJson(dat.toJson(false))
        assertEquals(100, dat[Person.age])
        assertEquals(100, dataMapFromJson[Person.age])
    }


    @Test
    fun testReadJsonWithStringArray() {
        val dat = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test06" }
                })

        dat.printAsJson(writeSystemProps = false)

        assertEquals(dat["data.name"], "John")

        Assert.assertEquals(listOf("Ford", "BMW", "Fiat"), dat.primeList<String>("data.cars"))


        dat["data.cars"] = listOf("Ford", "BMW", "Fiat", "Honda")
        dataService.flush()

        val dat1 = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test06" }
                })
        Assert.assertEquals(listOf("Ford", "BMW", "Fiat", "Honda"), dat1.primeList<String>("data.cars"))

        dat1["data.numbers"] = listOf(100, 500, 666)
        dataService.flush()
        val dat2 = dataService
                .find_(Attach.slice {
                    withBlobs()
                    filter { name eq "test06" }
                })
        Assert.assertEquals(listOf(100, 500, 666), dat2.primeList<Int>("data.numbers"))

        dat2.printAsJson(writeSystemProps = false)
    }

    /**
     * Тесты на использование ссылок на внешние по отношению к жисон сущности.
     * Присваивание таких ссылок в структуру json должно приводить к денормализации
     * */
    @Test
    fun testWriteInJsonHeaderOfTableEntity() {
        val person = dataService.find_(Person.withId(1))
        val att1 = Attach {
            it[data] = DynamicEntity {
                it["foo"] = 100500
                it["bar"] = 100600
                it["person"] = person
            }
        }

        dataService.flush()

        val att11 = dataService.find_(Attach.on().id(att1.id).withBlobs())

        att11.printAsJson(true)
        assertTrue(att11("data.person")!!.entity == "Person")
        assertTrue(att11("data.person")!!.fieldSet == Person)
        assertTrue(att11["data.person.name"] == "Madonna")
        assertTrue(att11["data.person.city"] == null)
        assertTrue(att11["data.person.gender"] == null)

        val p11 = dataService.upgrade(listOf(att11("data.person")!!),
                projection().full())[0]

        p11.printAsJson()

        assertTrue(p11["city"] != null)
        assertTrue(p11["gender"] != null)

    }

    @Test
    fun testWorkWithCollections() {

        val att1 = Attach {}
        att1.list("data.list").add(DynamicEntity {
            it["foo"] = 100500
            it["bar"] = 100600
        })
        assertTrue(att1.list("data.list").size==1)
        dataService.flush()
    }


    /**
     * Проверяем, что если в json-объекте есть поле "entity", но реально такой entity нет->
     * то ничего не упадет, а просто вынется dynamic entity
     * */
    @Test
    fun testReadNoEntityInJson() {

        //I. первая часть - убедимся что в динамик можно проставить свойство ентити (==Person)
        // и тогда при доставаньи из базы - такой динамик будет развернут как DM<Person>
        val att1 = Attach {
            it[data] = DynamicEntity {
                it["foo"] = 100500
                it["bar"] = 100600
                it["entity"] = "Person"
            }
        }

        dataService.flush()

        val att11 = dataService.find_(Attach.on().id(att1.id).withBlobs())

        att11.printAsJson(true)
        assertTrue(att11("data")!!.fieldSet == Person)


        //II. но если в ентити чтотоо левое - просто развернется динамический датамап
        val att2 = Attach {
            it[data] = DynamicEntity {
                it["foo"] = 100500
                it["bar"] = 100600
                it["entity"] = "Person666"
            }
        }

        dataService.flush()

        val att22 = dataService.find_(Attach.on().id(att2.id).withBlobs())

        att22.printAsJson(true)
        assertTrue(att22("data")!!.fieldSet == DynamicFieldSet)
    }

}