package com.datamaps.services

import com.datamaps.BaseSpringTests
import org.springframework.beans.factory.annotation.Autowired
import org.testng.annotations.Test

class SequenceIncrementorTests : BaseSpringTests() {

    @Autowired
    lateinit var sequenceIncrementor:SequenceIncrementor

    @Test
    fun testGetNextSeqId()
    {
        assertTrue(sequenceIncrementor.canGenerateId("JiraWorker"))

        val l = sequenceIncrementor.getNextId("JiraWorker")

        assertTrue(l > 999)
        println(l)

        assertFalse(sequenceIncrementor.canGenerateId("JiraGender"))

    }

}