package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.core.delta.DeltaMachine
import com.bftcom.ice.datamaps.core.delta.DeltaStore
import com.bftcom.ice.server.*
import org.junit.Assert
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals


/**
 * Created by Щукин on 03.11.2017.
 */

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
open class DeltaMachineTests : BaseSpringTests() {

    @Autowired
    lateinit var deltaMachine: DeltaMachine


    @Test
    fun testInsertTest() {
        sqlStatistics.start()

        val theCity = City.create {
            it[id] = 444
            it[title] = "Rostov"
        }

        val thePerson = Person.create {
            it[name] = "Ostat"
            it[lastName] = "Bender"
            it[city] = theCity
        }

        dataService.insert(theCity)
        dataService.insert(thePerson)

        assertNotNull(theCity.id)
        assertNotNull(thePerson.id)

        dataService.flush()

        sqlStatistics.stop()
    }


    @Test
    fun testSimpleUpdateQueryies() {
        val gender = dataService.get(Gender.entity, 2L)!!
        println(gender)

        gender[GDR.name] = "men"
        gender[GDR.name] = "men2"

        val list = deltaMachine.createAndExeUpdateStatements(DeltaStore.collectBuckets())

        list.forEach {
            println(it.first)
        }
        assertBodyEquals(list[0].first,
                "UPDATE GENDER SET NAME = :_name \n" +
                        " WHERE ID = :_ID")
    }

    @Test
    fun testSimpleUpdates() {
        val agender = dataService.get(Gender.entity, 2L)!!
        println(agender)

        with(Gender) {
            agender[name] = "men"
            agender[name] = "men2"
        }
        dataService.flush()

        val gender2 = dataService.get(Gender.entity, 2L)!!
        println(gender2)
        Assert.assertTrue(agender["name"] == "men2")
    }

    @Test
    fun testM1Updates() {


        val gender = dataService.find(on(Gender)
                .where("{{name}}='woman'"))!!

        Assert.assertNotNull(gender)

        val worker = dataService.find(
                on(Person).withRefs()
                        .where("{{name}} = 'Fillip Bedrosovich'"))!!

        println(worker)
        Assert.assertNotNull(worker)

        //получай филлипп бедросыч
        worker[Person.gender] = gender

        dataService.flush()


        val worker2 = dataService.find(
                on(Person).withRefs()
                        .where("{{name}} = 'Fillip Bedrosovich'"))!!


        Assert.assertEquals(worker2[WRKR.gender][GDR.name], "woman")

    }


    @Test()
    fun test1MAddExisting() {

        val task001 = dataService.find(
                on(Task)
                        .full()
                        .where("{{name}}='SAUMI-001'"))!!

        Assert.assertTrue(task001[Task.checks].size == 0)

        val checl01 = dataService.find(
                on(Check)
                        .where("{{id}}=1"))!!

        Assert.assertNotNull(checl01)

        Assert.assertNull(checl01[Check.task])

        task001[Task.checks].add(checl01)


        Assert.assertEquals(task001, checl01[Check.task])

        dataService.flush()

        val task0011 = dataService.find(
                on("Task")
                        .full()
                        .where("{{name}}='SAUMI-001'"))!!

        println(task0011)
        Assert.assertTrue(task0011.list("checklists").size == 1)
        Assert.assertEquals(task0011.list("checklists")[0], checl01)

    }

    @Test()
    fun testInsertSimpleNoIdentityId() {

        val gender = DataMap("Gender", 100L, true)

        Assert.assertTrue(gender.isNew())

        dataService.flush()

        Assert.assertFalse(gender.isNew())

        val gender_ = dataService.get("Gender", 100L)
        assertNotNull(gender_)
    }


    @Test()
    fun testInsertSimpleSequenceIdgenerarion() {

        val worker = DataMap("Person")
        worker["name"] = "jack"
        assertTrue(worker.isNew())
        assertNull(worker.id)

        dataService.flush()

        assertFalse(worker.isNew())
        assertNotNull(worker.id)

        val worker_ = dataService.find_(Person.withId(worker.id))
        assertNotNull(worker_)
    }

    @Test
    fun testInsertSimpleIdentity() {

        val department = DataMap("Department")
        department["name"] = "hello w"

        assertTrue(department.isNew())
        assertNull(department.id)

        dataService.flush()

        assertFalse(department.isNew())
        assertNotNull(department.id)

        val department_ = dataService.get("Department", department.id)
        assertNotNull(department_)
    }


    @Test()
    fun test1nAddNewSlave() {

        val task001 = dataService.find(
                on("Task")
                        .full()
                        .where("{{name}}='SAUMI-001'"))!!

        Assert.assertTrue(task001.list("checklists").size == 0)

        val ch01 = DataMap("Checklist")
        ch01["name"] = "mytask"
        task001.list("checklists").add(ch01)

        dataService.flush()

        val task0011 = dataService.find(
                on("Task")
                        .full()
                        .where("{{name}}='SAUMI-001'"))!!

        println(task0011)
        Assert.assertTrue(task0011.list("checklists").size == 1)
        Assert.assertEquals(task0011.list("checklists")[0], ch01)


    }


    @Test()
    fun test1nAddNewMasterAndSlaves() {

        val task001 = DataMap("Task")
        task001["name"] = "SAUMI-6666"

        val ch01 = DataMap("Checklist")
        ch01["name"] = "mytask"
        task001.list("checklists").add(ch01)

        dataService.flush()

        val task0011 = dataService.find(
                on("Task")
                        .full()
                        .where("{{name}}='SAUMI-6666'"))!!

        println(task0011)
        assertEquals(task001["n"], task0011["n"])
        Assert.assertTrue(task0011.list("checklists").size == 1)
        Assert.assertEquals(task0011.list("checklists")[0], ch01)

        //добавим ищо
        val ch02 = DataMap("Checklist")
        ch02["name"] = "mytask2"
        task001.list("checklists").add(ch02)

        dataService.flush()

        val task0012 = dataService.find(
                on("Task")
                        .full()
                        .where("{{name}}='SAUMI-6666'")
                        .order(f("checklists.id"))
        )!!

        Assert.assertTrue(task0012.list("checklists").size == 2)
        Assert.assertEquals(task0012.list("checklists")[1], ch02)
    }

    @Test()
    fun test1nRemoveSlaves() {

        val task001 = DataMap("Task")
        task001["name"] = "SAUMI-6666"

        val ch01 = DataMap("Checklist")
        ch01["name"] = "mytask"
        task001.list("checklists").add(ch01)

        dataService.flush()

        val task0011 = dataService.find(
                on("Task")
                        .full()
                        .where("{{name}}='SAUMI-6666'"))!!

        //хочу и удаляю
        task0011.list("checklists").remove(DataMap("Checklist", ch01.id))

        assertTrue(task0011.list("checklists").size == 0)

        dataService.flush()


        val task0012 = dataService.find(
                on("Task")
                        .full()
                        .where("{{name}}='SAUMI-6666'"))!!

        assertTrue(task0012.list("checklists").size == 0)

    }

    @Test()
    fun test1nRemoveCascade() {

        val task001 = DataMap("Task")
        task001["name"] = "SAUMI-6666"

        val ch01 = DataMap("Checklist")
        ch01["name"] = "mytask"
        task001.list("checklists").add(ch01)

        dataService.flush()

        println(ch01.id)
        assertFalse(ch01.isNew())

        dataService.delete(task001)
        dataService.flush()


        val task001_ = dataService.find(
                on("Task")
                        .id(task001.id))

        val ch01_ = dataService.find(
                on("Checklist")
                        .id(ch01.id))

        assertNull(task001_)
        assertNull(ch01_)


    }

    @Test()
    fun testDeleteAll() {

        val count = dataService.count(Check.on().onlyId())
        println(count)
        Assert.assertTrue(count > 0)

        (dataService as DataServiceExtd).deleteAll(Check.entity)

        val count2 = dataService.count(Check.on().onlyId())
        println(count2)
        Assert.assertTrue(count2 == 0)

    }

    @Test
    fun caseOsenny() {
        sqlStatistics.restart()

        val task = Task.create()
        val check = Check.create()

        check[Check.name] = "mod01"
        task[Task.name] = "product01"
        task[Task.checks].add(check)

        dataService.flush()

        assertTrue(sqlStatistics.updates().size == 0)
        assertTrue(sqlStatistics.inserts().size == 2)
    }

    @Test
    fun caseOsenny2() {

        sqlStatistics.restart()

        val task = Task.create()
        task[Task.name] = "product01"
        dataService.flush()

        val task1 = dataService.find_(on(Task).id(task.id))
        val check1 = Check.create()
        check1[Check.name] = "module01"
        val check2 = Check.create()
        check2[Check.name] = "module02"

        task1[Task.checks].add(check1)
        task1[Task.checks].add(check2)

        dataService.flush()

        assertTrue(sqlStatistics.updates().size == 0)
        assertTrue(sqlStatistics.inserts().size == 3)
    }


    @Test
    fun testDataMapsSilentModes() {

        sqlStatistics.restart()

        val task = Task.createInSilence {
            it[name] = "hello"
        }

        dataService.flush()
        assertTrue(sqlStatistics.inserts().isEmpty())
        assertTrue(sqlStatistics.updates().isEmpty())

        Task.updateInSilence(task) {
            task[name] = "product01"
        }

        dataService.flush()
        assertTrue(sqlStatistics.inserts().isEmpty())
        assertTrue(sqlStatistics.updates().isEmpty())

        inSilence {
            task[{ name }] = "product01"
        }

        dataService.flush()
        assertTrue(sqlStatistics.inserts().isEmpty())
        assertTrue(sqlStatistics.updates().isEmpty())

        val task2 = inSilence {
            Task {
                it.id = 100500
                it[name] = "trulala"
            }
        }

        dataService.flush()
        assertTrue(sqlStatistics.inserts().isEmpty())
        assertTrue(sqlStatistics.updates().isEmpty())

    }

    @Test
    fun `oldValue must be an initial state of the object, not intermediate`() {

        val man = find_(Gender.filter { name eq "man" })
        //создаем ЧЕЛОВЕКА
        val person = Person {
            it[name] = "Boris"
            it[lastName] = "Schukin"
            it[age] = 38
            //референс
            it[gender] = man
        }
        dataService.flush()

        person[{name}] =  "Boooooooris"
        person[{name}] =  "BORIIIIIS"
        person[{name}] =  "Boris-3000"

        val buckets = DeltaStore.flushAllToBuckets()



        assertEquals("Boris",  buckets[0].deltas[0].oldValue?.value())
    }

}