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

fun eraseAllWs(string:String):String =  string.replace("\\s".toRegex(),"")


fun assertBodyEquals(string1:String, string2:String) {
    Assert.assertEquals(eraseAllWs(string1).toLowerCase(), eraseAllWs(string2).toLowerCase())
}

fun assertEqIgnoreCase(string1:String, string2:String) {
    Assert.assertEquals(string1.toLowerCase(), string2.toLowerCase())
}

fun assertEqIgnoreCase(list1:List<*>, list2:List<*>) {
    if(list1.size!=list2.size)
        Assert.fail("lists have different size: ${list1.size} vs ${list2.size}")
    for (item: Int in list2.indices) {
        var i1 = list1[item].toString()
        var i2 = list2[item].toString()

        Assert.assertTrue(i1.equals(i2, true ), "items №${item} of " +
                "lists are different:  ${i1} vs ${i2} ")
    }
}