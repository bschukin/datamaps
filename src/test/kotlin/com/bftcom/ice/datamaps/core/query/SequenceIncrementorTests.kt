package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.BaseSpringTests
import com.bftcom.ice.datamaps.IfSpringProfileActive
import com.bftcom.ice.datamaps.core.util.SequenceIncrementor
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