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
        assertTrue(sequenceIncrementor.canGenerateIdFromSequence ("Jira_Worker"))

        val l = sequenceIncrementor.getNextId("JiraWorker")

        assertTrue(l > 999)
        println(l)

        assertFalse(sequenceIncrementor.canGenerateIdFromSequence("Jira_Gender"))

    }

}