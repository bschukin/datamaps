package com.datamaps.services

import com.datamaps.BaseSpringTests
import org.springframework.beans.factory.annotation.Autowired
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class DataServiceBasicTests: BaseSpringTests()
{

    @Autowired
    lateinit var dataService: DataService

    @Test
    public fun testGetMethod()
    {
        var dm = dataService.get("JiraDepartment", 555L)
    }


}