package com.bftcom.ice.datamaps.examples

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.core.util.printAsJson
import org.junit.Assert
import org.junit.Test


/***
 * Basic Examples
 */
open class DeltaMachineExamples : BaseSpringTests() {

    @Test
    fun `insert new objects`() {

        val theCity = City {
            it[id] = 444
            it[title] = "Rostov"
        }

        val thePerson = Person {
            it[name] = "Ostat"
            it[lastName] = "Bender"
            it[city] = theCity
        }

        dataService.flush()

        assertNotNull(theCity.id)
        assertNotNull(thePerson.id)

    }

    @Test
    fun `implicitly insert new objects`() {

        val theCity = City {
            it[id] = 444
            it[title] = "Rostov"
        }

        val thePerson = Person {
            it[name] = "Ostat"
            it[lastName] = "Bender"
            it[city] = theCity
        }

        dataService.insert(theCity)
        dataService.insert(thePerson)

        assertNotNull(theCity.id)
        assertNotNull(thePerson.id)
    }

    @Test
    fun `simple updates`() {
        val agender = dataService.find_(Gender.on().id(2L))

        with(Gender) {
            agender[name] = "men2"
        }
        dataService.flush()

        val gender2 = dataService.find_(Gender.on().id(2L))
        println(gender2)
        Assert.assertTrue(agender["name"] == "men2")
    }

    @Test
    fun `update reference`() {


        val gender = dataService.find(on(Gender)
                .where("{{name}}='woman'"))!!


        val worker = dataService.find(
                Person.on().where("{{name}} = 'Fillip Bedrosovich'"))!!

        //получай филлипп бедросыч
        worker[Person.gender] = gender

        dataService.flush()


        val worker2 = dataService.find(
                Person.on().withRefs()
                        .where("{{name}} = 'Fillip Bedrosovich'"))!!


        Assert.assertEquals(worker2[Person.gender().name], "woman")

    }

    @Test()
    fun `update list by adding new element`() {

        val task001 = dataService.find_(
                Task.slice {
                    full()
                    filter { name eq "SAUMI-001" }
                })

        Assert.assertTrue(task001[Task.checks].size == 0)

        val checl01 = dataService.find(
                Check.withId(1))!!


        task001[Task.checks].add(checl01)


        Assert.assertEquals(task001, checl01[Check.task])

        dataService.flush()

        val task0011 = dataService.find_(
                Task.slice {
                    full()
                    filter { name eq "SAUMI-001" }
                })

        println(task0011)
        Assert.assertTrue(task0011.list("checklists").size == 1)
        Assert.assertEquals(task0011.list("checklists")[0], checl01)

    }
}