package com.datamaps.services

import com.datamaps.KotlinDemoApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
@SpringBootTest(
        classes = arrayOf(KotlinDemoApplication::class),
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DataServiceBasicTests: AbstractTransactionalTestNGSpringContextTests()
{

    @Autowired
    lateinit var dataService: DataService

    @Test(expectedExceptions = arrayOf(kotlin.NotImplementedError::class))
    public fun testGetMethod()
    {
        var dm = dataService.get("Notes", 555L)
    }


}