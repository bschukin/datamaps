package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.on
import com.bftcom.ice.datamaps.projection
import com.bftcom.ice.datamaps.slice
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Gender
import com.bftcom.ice.server.Person
import com.bftcom.ice.datamaps.core.util.printAsJson
import org.junit.Test

/**
 * Created by Щукин on 03.11.2017.
 */
open class DataServiceTests01Basics : BaseSpringTests()
{

    @Test
    fun testGetMethod()
    {
        var dm = dataService.get("Department", 555L)
    }

    @Test
    fun testFind1ethod()
    {
        val res = dataService.find(
                on(Gender).full()
                        .where("{{id}} = 1")
        )!!
        println(res)
        println(res[Gender.isClassic])
    }

    @Test
    fun testFindAllMethod()
    {
        val res = dataService.findAll(
                projection("Person")
                        .field("name")
        )
        res.forEach {r->
            println(r)
        }
    }


    @Test
    fun testSqlToMapMethod()
    {
        val res = dataService.sqlToFlatMap(Person.entity,
                "select w.id,w.name, g.name as name1 from person w " +
                        "left join gender g on g.id = w.gender_id " +
                        "where g.name = :_name",
                mapOf("_name" to "man"))!!

        assertTrue(res[Person.name]=="John Lennon")
    }


    @Test
    fun test2SlicesOnOneFloor()
    {
        val res = dataService.findAll(
                projection("Department")
                        .with { slice("boss").field("name") }
                        .with { slice("city").field("title") }
        )
        res.forEach {r->
            println(r)
        }
    }

    @Test
    fun testRefRetrieveStyle()
    {
        val res = dataService.find_(Person.slice {
            withId(1L)
            gender{
                option().asMapWithOnlyId()
            }
        })

        res.printAsJson()
    }




}