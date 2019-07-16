package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.DataMap
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Person
import org.junit.Test
import org.springframework.stereotype.Service
import kotlin.test.assertEquals

/**
 * Created by Щукин on 03.11.2017.
 */
open class DataServiceRpcTests : BaseSpringTests() {

    interface TestService0 {
        fun testProcedure(): String
    }

    @Service
    class TestService : TestService0 {
        override fun testProcedure(): String {
            return "helloWorld"
        }

        fun testProcedure(int: Int, string: String, dm: DataMap): String {
            return "$int $string ${dm.id}"
        }
    }


    @Test
    fun testRpc01() {

        val res = dataService.springBeanMethodCall(TestService0::class.java.name!!,
                TestService0::testProcedure.name)
        assertEquals("helloWorld", res)
    }

    @Test
    fun testRpc02() {

        val res = dataService.springBeanMethodCall(TestService0::class.java.name!!,
                "testProcedure",
                1, "Hello", Person.create { it.id = 666; it[name] = "world" })
        assertEquals(res, "1 Hello 666")
    }


}