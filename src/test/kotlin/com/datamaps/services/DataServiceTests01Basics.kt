package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.mappings.projection
import org.springframework.beans.factory.annotation.Autowired
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class DataServiceTests01Basics : BaseSpringTests()
{

    @Autowired
    lateinit var dataService: DataService

    @Test
    public fun testGetMethod()
    {
        var dm = dataService.get("JiraDepartment", 555L)
    }


    @Test
    public fun testFindAllMethod()
    {
        var res = dataService.findAll(
                projection("JiraWorker").field("name")
        )
        res.forEach {r->
            println(r)
        }
    }


}