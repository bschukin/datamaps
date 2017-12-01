package com.datamaps.maps

import org.testng.Assert
import org.testng.annotations.Test

/**
 * Created by Щукин on 27.10.2017.
 */
class DataMapTests {


    @Test
    fun testEquality()
    {
        val dm01 = DataMap("A", 1L)
        val dm011 = DataMap("A", 1L)

        Assert.assertEquals(dm01, dm011)

        val dm02 = DataMap("A", 2L)
        Assert.assertNotEquals(dm01, dm02)

        val dm03 = DataMap("B", 2L)
        Assert.assertNotEquals(dm02, dm03)

        //два новых
        val dm04 = DataMap("C")
        val dm041 = DataMap("C")
        Assert.assertNotEquals(dm04, dm041)

        //странный кейс, но должно работать так
        val dm05 = DataMap("C", 1L)//типо из базы
        val dm051 = DataMap("C", 1L, true)//типо новый и с присвоенным айдишником сразу от нас
        Assert.assertNotEquals(dm05, dm051)


    }

    @Test
    fun testMerge() {

        val a = DataMap("A", 1L)
        a["xxx"] = "zzz"
        a["yyy"] = "yyyy"

        val a1 = DataMap("A", 1L)
        a1["xxx"] = "zzz1"

        mergeDataMaps(a, a1)

        //убеждаемся что свойство из a1 перезаписало свойство в a
        Assert.assertEquals(a["xxx"], "zzz1")

        //но если свойства из a1 нет в a - оно останется нетронутым
        Assert.assertEquals(a["yyy"], "yyyy")


        //добавим вложенную мапу
        val b = DataMap("B", 1L)

        b["prop1"] = 100
        b["prop2"] = 200
        a["b"] = b


        val b1 = DataMap("B", 1L)
        b1["prop1"] = 101
        a1["b"] = b1

        mergeDataMaps(a, a1)

        Assert.assertEquals(a["xxx"], "zzz1")
        Assert.assertEquals(a["yyy"], "yyyy")

        Assert.assertEquals(a("b")["prop1"], 101)
        Assert.assertEquals(a("b")["prop2"], 200)

        //ассертимся что не будет зацикливания на обратных ссылках
        b["a"] = a
        b1["a"] = a1
        mergeDataMaps(a, a1)


        //поиграемся с коллекциями
        val c = DataMap("C", 1L)
        a1.list("c").add(c)


        mergeDataMaps(a, a1)
        Assert.assertNotNull(a.list("c"))
        Assert.assertTrue(a.list("c").size == 1)
        Assert.assertTrue(a.list("c")[0] == c)


        val c2 = DataMap("C", 2L)
        a1.list("c").add(c2)

        mergeDataMaps(a, a1)
        Assert.assertTrue(a.list("c").size == 2)
        Assert.assertTrue(a.list("c")[1] == c2)


        val c3 = DataMap("C", 3L)
        c3["cc"] = "cccp"
        a.list("c").add(c3)

        val c31 = DataMap("C", 3L)
        c31["cc"] = "cccc"
        a1.list("c").add(c31)

        mergeDataMaps(a, a1)
        Assert.assertTrue(a.list("c").size == 3)
        Assert.assertTrue(a.list("c")[2] == c3)
        Assert.assertTrue(a.list("c")[2]["cc"] == "cccc")

    }


}



