package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.f
import com.bftcom.ice.datamaps.slice
import com.bftcom.ice.server.Attach
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.IfSpringProfileActive
import com.bftcom.ice.datamaps.impl.util.printAsJson
import org.junit.Test

/**
 * Created by Щукин on 03.11.2017.
 */
open class JsonQueriesTests : BaseSpringTests() {


    /***** тесты на фильтры в JSON*****************/
    @Test
    @IfSpringProfileActive("postgresql")
    fun testFilterOnJsonB() {
        val a = Attach.withBlobs().filter { f("data2.name") eq "hella" }
        val aa = dataService.findAll(a)
        assertTrue(aa.size == 1)

        aa.forEach {
            it.printAsJson(false)
        }
    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testFilterOnJsonB2() {
        val a =
                Attach.withBlobs().filter { f("data2.part.bar") eq "314" }
        val aa = dataService.find_(a)
        assertNotNull(aa)
        assertTrue(aa["data2.part.bar"] == "314")

        //тоже самое но с Field

        val a2 =
                Attach.filter { data2().part().bar eq "314" }.withBlobs()
        val aa2 = dataService.find_(a2)
        assertNotNull(aa2)
        assertTrue(aa2["data2.part.bar"] == "314")
    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testFilterOnNumericJsonField() {
       val a =
                Attach.withBlobs().filter { f("data.age") gt 30 }
        val aa = dataService.findAll(a)
        assertTrue(aa.size ==1)


        val a1 =
                Attach.withBlobs().where ( "({{data.age}})::numeric >= 30" )
        val aa1 = dataService.findAll(a1)
        assertTrue(aa1.size ==2)

    }

    /***** тесты на проекции в JSON*****************/
    @Test
    fun testProjectionOnJson() {

        //вырезаем проекцией только то что нужно
        val a = dataService.find_(Attach.slice {
            data2 {
                +part
            }
            filter { name eq "test05" }
        })
        assertNotNull(a)
        assertTrue(a["data2.name"] == null)
        assertTrue(a["data2.part.bar"] == "314")
        assertTrue(a["data2.part.foo"] == "unknown")

        //тест на проекцию, в которой foo убереется

        val aa = dataService.find_(Attach.slice {
            data2 {
                part {
                    +bar
                }
            }
            filter { name eq "test05" }
        })
        assertNotNull(aa)
        assertTrue(aa["data2.name"] == null)
        assertTrue(aa["data2.part.bar"] == "314")
        assertTrue(aa["data2.part.foo"] == null)

    }

    @Test
    fun testProjectionsOnJsonShouldNotWorkWhenBlobGroupIsSwitched() {


        val aa = dataService.find_(
                Attach.slice {
                    withBlobs()
                    data2 {
                        part {
                            +bar
                        }
                    }
                    filter { name eq "test05" }
                })
        assertNotNull(aa)
        assertTrue(aa["data2.name"] != null)
        assertTrue(aa["data2.part.bar"] != null)
        assertTrue(aa["data2.part.foo"] != null)

    }

    @Test
    //проекции на коллекции в json
    fun testProjectionOnJsonCollection() {

        //вырезаем проекцией только то что нужно
        val a = dataService.find_(
                Attach.on().with {
                    slice("data").with {
                        slice("menu")
                                .field("value")
                                .with(slice("popup")
                                        .with {
                                            slice("menuitem")
                                                    .field("onclick")
                                        }
                                )
                    }
                }.filter { Attach.name eq "test01" })

        assertTrue(a["data.name"] == null)
        assertTrue(a["data.menu.id"] == "file")        //nb - явно не включали в проекцю, но id всегда есть
        assertTrue(a["data.menu.value"] != null)
        assertTrue(a["data.menu.popup.menuitem"] != null)
        assertTrue(a.list("data.menu.popup.menuitem").size == 3)
        assertTrue(a["data.menu.popup.menuitem[0].value"] == null)
        assertTrue(a["data.menu.popup.menuitem[0].onclick"] != null)

        a.printAsJson(true)
    }

    @Test
    fun testUpgradeOnJsonField() {

        val a =
                Attach.slice {
                    filter { name eq "test05" }
                }

        val aa = dataService.find_(a)
        assertNull(aa[{ data2 }])
        assertNotNull(aa)

        dataService.upgrade(listOf(aa), Attach.slice { +data2 })

        assertNotNull(aa[{ data2 }])
        assertTrue(aa["data2.part.bar"] == "314")
    }
}