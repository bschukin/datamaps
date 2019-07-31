package com.bftcom.ice.datamaps.common.general

import com.bftcom.ice.datamaps.BaseSpringTests
import com.bftcom.ice.datamaps.Gender
import org.junit.Assert
import org.junit.Test
import com.bftcom.ice.datamaps.assertBodyEquals
import com.bftcom.ice.datamaps.misc.DbRecordNotFound
import com.bftcom.ice.datamaps.misc.toExceptionInfo
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