package com.bftcom.ice.common.general

import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Gender
import org.junit.Assert
import org.junit.Test
import com.bftcom.ice.common.utils.assertBodyEquals
import kotlin.test.assertEquals


open class ExceptionTests : BaseSpringTests() {

    @Test
    fun testExceptionInfo() {
        try {
            jdbcTemplate.query("select * from huemae", {})
        } catch (e: Exception) {
            val ei = e.toExceptionInfo()
            println(ei)
            println("===================")
            println(ei.stackTrace)
            assertTrue(ei.clazz.isNotEmpty())
            assertTrue(ei.stackTrace.isNotEmpty())
            assertTrue(ei.cause!!.stackTrace.isEmpty())

        }
    }

    @Test
    fun testObjectNotFoundException() {
        try {
            dataService.find_(Gender.withId(66))
        } catch (e: DbRecordNotFound) {

            assertBodyEquals(e.entity, Gender.entity)
            assertEquals(e.id, 66)
            return
        }

        Assert.fail()
    }

}