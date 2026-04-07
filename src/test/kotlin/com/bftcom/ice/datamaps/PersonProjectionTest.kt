package com.bftcom.ice.datamaps

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PersonProjectionTest : BaseSpringTests() {

    @Test
    fun testPersonProjection() {
        // Create a projection on Person
        val dp = Person.slice {
            +name
            +age
            gender {
                +name
            }
            childs {
                +name
            }
        }.filter { Person.name eq "Oleg Gazmanov" }

        // Execute the query
        val person = dataService.find_(dp)

        // Verify the results
        assertNotNull(person)
        assertEquals("Oleg Gazmanov", person[Person.name])
        assertNotNull(person[Person.gender])
        assertNotNull(person[Person.gender][Gender.name])
        assertNotNull(person[Person.childs])

        println("Person: ${person[Person.name]}, Gender: ${person[Person.gender][Gender.name]}")
    }
}
