package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.Gender
import com.datamaps.maps.on
import com.datamaps.maps.projection
import com.datamaps.maps.slice
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class DataServiceTests01Basics : BaseSpringTests()
{

    @Test
    fun testGetMethod()
    {
        var dm = dataService.get("JiraDepartment", 555L)
    }

    @Test
    fun testFind1ethod()
    {
        var res = dataService.find(
                on(Gender).full()
                        .where("{{id}} = 1")
        )!!
        println(res)
        println(res[Gender.isClassic])
    }

    @Test
    fun testFindAllMethod()
    {
        var res = dataService.findAll(
                projection("JiraWorker")
                        .field("name")
        )
        res.forEach {r->
            println(r)
        }
    }


    @Test
    fun test2SlicesOnOneFloor()
    {
        var res = dataService.findAll(
                projection("JiraStaffUnit")
                        .with { slice("worker").field("name") }
                        .with { slice("gender").field("gender") }
        )
        res.forEach {r->
            println(r)
        }
    }


}