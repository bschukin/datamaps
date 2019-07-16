package com.bftcom.ice.server.datamaps

import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.test.IfSpringProfileActive
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

open class SequenceIncrementorTests : BaseSpringTests() {

    @Autowired
    lateinit var sequenceIncrementor: SequenceIncrementor

    @Test
    @IfSpringProfileActive("postgresql", "oracle", "hsqldb")
    fun testGetNextSeqId()
    {
        assertTrue(sequenceIncrementor.canGenerateIdFromSequence ("Person"))

        val l = sequenceIncrementor.getNextId("Person")

        assertTrue(l > 999)
        println(l)

        assertFalse(sequenceIncrementor.canGenerateIdFromSequence("Gender"))

    }

}