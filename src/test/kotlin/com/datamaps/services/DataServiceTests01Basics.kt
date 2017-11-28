package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.mappings.projection
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


}