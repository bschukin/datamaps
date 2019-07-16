package com.bftcom.ice.datamaps.common.maps

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.misc.CiMap
import com.bftcom.ice.datamaps.misc.GUID
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Game
import com.bftcom.ice.server.Gender
import com.bftcom.ice.server.Person
import com.bftcom.ice.datamaps.core.util.printAsJson
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional


/**
 * Created by Щукин on 27.10.2017.
 */
open class DataMapTests : BaseSpringTests() {


    class YetAnotherPerson : DataMapF<UndefinedFieldSet>(name = "Person") {
        var name: String by this.map
    }

    object P : MFS<P>() {
        var name = Field.string("name")
    }

    @Test
    fun testByMap2() {
        println(P.entity)
        assertEquals(P.entity, "P")
    }

    @Test
    fun testByMap1() {
        val person = DataMap("asd")
        println(person.isTransient())
    }

    @Test
    fun testByMap() {
        val person = YetAnotherPerson()
        person.name = "xxxx"

        assertTrue(person["name"]=="xxxx")
        assertTrue(person["name"]==person.name)
    }


    object DM01 : MFS<DM01>() {

        val id = Field.id()
        val name = Field.stringNN("name") { caption = "Наименование" }
        val nameAlt = Field.string("nameAlt") { caption = "Альтернативное имя" }
    }

    @Test
    fun testNullability() {

        val dm = DM01.create { it[name] = "zzxxx" }
        dm[{name}] = "plas"

        println(dm[{name}].length)
        println(dm[{ nameAlt}]?.length)

        assertNotNull(dm[{name}])
        assertNull(dm[{ nameAlt}])

        assertTrue(DM01.name.required)
        assertFalse(DM01.nameAlt.required)
    }


    @Test
    fun testCreateApiWithFieldsSets() {

        //create

        val m1 = Gender{
            it[id] = 100L
            it[name] = "ccc"
        }

        println(m1)
        with(Gender)
        {
            assertTrue(m1[id] == 100L)
            assertTrue(m1[name] == "ccc")
        }

        val myGender = Gender.create {
            it[id] = 100L
            it[name] = "был такой"
        }
        println(myGender)
        with(Gender)
        {
            assertTrue(myGender[id] == 100L)
            assertTrue(myGender[name] == "был такой")
        }


        val worker = Person.create {
            it[id] = 100L
            it[name] = "some hero"
            it[gender] = myGender
            it[gender().name] = "стал другой"
        }
        with(Person)
        {
            assertTrue(worker[id] == 100L)
            assertTrue(worker[name] == "some hero")
            assertTrue(worker[gender().name] == "стал другой")
        }
        println(worker)

        //часть вторая - апдейты
        update(Gender, m1).with {
            it[name] = "zzz"
        }
        println(m1)
        with(Gender)
        {
            assertTrue(m1[name] == "zzz")
        }

        Person.update(worker) {
            it[name] = "some zero"
            it[gender] = m1
            it[gender().name] = "совсем иной"
        }

        with(Person)
        {
            assertTrue(worker[id] == 100L)
            assertTrue(worker[name] == "some zero")
            assertTrue(worker[gender().name] == "совсем иной")
        }
        with(Gender)
        {
            assertTrue(m1[name] == "совсем иной")
            assertTrue(myGender[name] == "стал другой")
        }

    }

    @Test
    fun testCreateApiWithFieldsSets2() {

        //create
        val m1 = Gender.create()
        m1[{ id }] = 100L
        m1[{ name }] = "ccc"

        println(m1)

        assertTrue(m1[{ id }] == 100L)
        assertTrue(m1[{ name }] == "ccc")


        val myGender = Gender.create {
            it[{id}] = 100L
            it[{name}] = "был такой"
        }
        println(myGender)

        myGender[{ id }] == 100L
        assertTrue(myGender[{ name }] == "был такой")


        val worker = Person.create()

        worker.with {
            it[id] = 100L
            it[name] = "some hero"
            it[gender] = myGender
            it[gender().name] = "стал другой"
        }

        assertTrue(worker[{ id }] == 100L)
        assertTrue(worker[{ name }] == "some hero")
        assertTrue(worker[{ gender().name }] == "стал другой")

        println(worker)

        //часть вторая - апдейты
        update(Gender, m1).with {
            it[name] = "zzz"
        }
        println(m1)

        assertTrue(m1[{ name }] == "zzz")

        Person.update(worker) {
            it[name] = "some zero"
            it[gender] = m1
            it[gender().name] = "совсем иной"
        }


        assertTrue(worker[{ id }] == 100L)
        assertTrue(worker[{ name }] == "some zero")
        assertTrue(worker[{ gender().name }] == "совсем иной")


        assertTrue(m1[{ name }] == "совсем иной")
        assertTrue(myGender[{ name }] == "стал другой")
    }


    @Test
    fun testEquality() {
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
        //NB: БЩ: воще так не должно работать (более поздняя вставка)
        val dm05 = DataMap("C", 1L)//типо из базы
        val dm051 = DataMap("C", 1L, true)//типо новый и с присвоенным айдишником сразу от нас
        //Assert.assertNotEquals(dm05, dm051) -- НЕТ
        Assert.assertEquals(dm05, dm051) //-- ДА

    }

    @Test
    fun testEqualityWithStrings() {
        val dm01 = DataMap("A", "1")
        val dm011 = DataMap("A", "1")

        Assert.assertEquals(dm01, dm011)

        val dm02 = DataMap("A", GUID.constGUID)
        val dm021 = DataMap("A", GUID.constGUID)

        Assert.assertEquals(dm02, dm021)

        /*val dm02 = DataMap("A", 2L)
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
        Assert.assertNotEquals(dm05, dm051)*/

    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
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

        Assert.assertEquals(a("b")!!["prop1"], 101)
        Assert.assertEquals(a("b")!!["prop2"], 200)

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


    @Test
    fun testDataMapIdAccess() {

        val foo = DataMap("foo", 100L)
        val bar = DataMap("bar", 200L, mapOf("foo" to foo))

        kotlin.test.assertTrue { foo[DataMap.ID] == 100L }
        kotlin.test.assertTrue { bar.nested("foo.id") == 100L }

    }

    @Test
    fun testFieldSetExistingBuilder() {

        val game = Game.existing { it[name] = "Call of Duty"; it[metacriticId] = 666 }

        kotlin.test.assertFalse { game.isNew() }

    }

    object ExampleFieldSet : MFS<ExampleFieldSet>("ExampleFieldSet") {

        var counter = 0

        val name = Field.string("name") { defaultValue = {"Hello world"} }
        val weight = Field.double("weight") { defaultValue = {100.500} }
        val number = Field.enum("number", E123.values()) { defaultValue = {E123.two} }

        val prop1 = Field.intNN("prop1") { defaultValue = { ++counter} }
        val prop2 = Field.intNN("prop2") { defaultValue = { it[prop1] + 1000 } }

        val prop3 = Field.jsonObj("prop3", ExampleDescriptor)
    }

    object ExampleDescriptor : MFS<ExampleDescriptor>("DynamicDescriptor", "", Dynamic) {
        val xxxx = Field.string("tableName") { caption = "Имя таблицы"; defaultValue = {"sdadasdad"}  }
    }

    @Test
    fun testFieldSetDefaultValues() {

        val aaa = ExampleFieldSet.create()

        assertEquals(aaa[ExampleFieldSet.name], "Hello world")
        assertEquals(aaa[{ weight }], 100.5)
        assertEquals(aaa[{ number }], E123.two)

        assertEquals(aaa[{ prop1 }], 1)
        assertEquals(aaa[{ prop2 }], 1001)
    }

    @Test
    fun testDynamicSetDefaultValues() {

        val aaa = ExampleDescriptor.create()
        aaa.printAsJson()
    }

    object ExampleFieldSet2 : MFS<ExampleFieldSet2>("ExampleFieldSet2") {

        val number = Field.enum("number", E123.values())
        val number2 = Field.enum("number2", E123.values())
    }

    enum class E123(override val displayName: String): StringEnum {
        one("__one__"),
        two("__two__");
    }

    @Test
    fun testEnumFields() {

        val aaa = ExampleFieldSet2.create()
        aaa[{ number }] = E123.one
        aaa[{ number2 }] = E123.one

        assertEquals(aaa[{ number }], E123.one)
        assertEquals(aaa[{ number2 }], E123.one)
        assertEquals(aaa.map["number"], E123.one)
        assertEquals(aaa.map["number2"],  E123.one)
    }

    @Test
    fun testReadonlyMaps() {
        val map = CiMap<String>()
        map["p1"] = "pp1"
        map["p2"] = "pp2"

        val map2 = map.toReadonly()

        try {
            map2["p1"] = "ddfsdfsds"
            Assert.fail()
        } catch (e: Exception) {
        }

        try {
            map2.clear()
            Assert.fail()
        } catch (e: Exception) {
        }

        try {
            map2.entries.first().setValue("tttttt")
            Assert.fail()
        } catch (e: Exception) {
        }


    }

    @Test
    fun testReadonlyDataMaps() {

        val p = Person{
            it[age] = 500
            it[name] = "Helloman"
        }
        val pr = p.toReadonly()
        assertTrue(pr == p)
        assertTrue(pr.newMapGuid == p.newMapGuid)
        assertFalse(pr === p)

        try {
            pr[{name}] = "Helloman111"
            Assert.fail()
        } catch (e: Exception) {
        }
    }

}



