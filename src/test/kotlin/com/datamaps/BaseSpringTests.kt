package com.datamaps

import com.datamaps.services.DataService
import com.datamaps.services.QueryBuilder
import com.datamaps.services.QueryExecutor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.Assert
import javax.annotation.Resource


/**
 * Created by Щукин on 01.11.2017.
 */


@SpringBootTest(
        classes = arrayOf(KotlinDemoApplication::class),
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
//@EnableAutoConfiguration()
@ContextConfiguration("classpath:test-app-context.xml")
class BaseSpringTests : AbstractTransactionalTestNGSpringContextTests() {

    @Autowired
    lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var env: Environment

    @Autowired
    lateinit var queryBuilder: QueryBuilder

    @Autowired
    lateinit var dataService: DataService

    @Resource
    lateinit var queryExecutor: QueryExecutor

    fun isProstgress(): Boolean = env.activeProfiles.contains("postgresql")

    fun assertNull(res:Any?) {
        Assert.assertNull(res)
    }
    fun assertNotNull(obj:Any?) {
        Assert.assertNotNull(obj)
    }

    fun assertTrue(res:Boolean) {
        Assert.assertTrue(res)
    }

    fun assertFalse(res:Boolean) {
        Assert.assertFalse(res)
    }



}

fun eraseAllWs(string: String): String = string.replace("\\s".toRegex(), "")


fun assertBodyEquals(string1: String, string2: String) {
    Assert.assertEquals(eraseAllWs(string1).toLowerCase(), eraseAllWs(string2).toLowerCase())
}

fun assertEqIgnoreCase(string1: String, string2: String) {
    Assert.assertEquals(string1.toLowerCase(), string2.toLowerCase())
}

fun assertEqIgnoreCase(obj: Any?, string2: String) {
    Assert.assertEquals(obj.toString().toLowerCase(), string2.toLowerCase())
}

fun assertEqIgnoreCase(list1: List<*>, list2: List<*>) {
    if (list1.size != list2.size)
        Assert.fail("lists have different size: ${list1.size} vs ${list2.size}")
    for (item: Int in list2.indices) {
        val i1 = list1[item].toString()
        val i2 = list2[item].toString()

        Assert.assertTrue(i1.equals(i2, true), "items №${item} of " +
                "lists are different:  ${i1} vs ${i2} ")
    }
}

