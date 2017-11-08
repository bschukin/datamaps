package com.datamaps

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.Assert


/**
 * Created by Щукин on 01.11.2017.
 */


@SpringBootTest(
        classes = arrayOf(KotlinDemoApplication::class),
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
//@EnableAutoConfiguration()
@ContextConfiguration("classpath:test-app-context.xml")
class BaseSpringTests : AbstractTransactionalTestNGSpringContextTests() {






}

fun eraseAllWs(string:String):String =  string.replace("\\s+","")


fun assertBodyEquals(string1:String, string2:String) {
    Assert.assertEquals(eraseAllWs(string1), eraseAllWs(string2))
}