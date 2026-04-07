package com.bftcom.ice.datamaps

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PersonProjectionTest : BaseSpringTests() {

    @Test
    fun testPersonProjectionWithChildFilter() {
        // Seed data for the test
        val person = Person {
            it[name] = "Boris"
            it[childs].add(Child { it[name] = "Sasha" })
            it[childs].add(Child { it[name] = "Родион" })
            it[childs].add(Child { it[name] = "Masha" })
        }
        dataService.insert(person)
        dataService.flush()

        // Create a projection on Person with filter on childs
        // Using withJoinFilter to filter the collection in the JOIN clause
        val dp = Person.slice {
            +name
            childs {
                +name
                withJoinFilter(f("childs.name") eq "Родион")
            }
        }.filter { Person.id eq person.id!! }

        // Execute the query
        val loadedPerson = dataService.find_(dp)

        // Verify the results
        assertNotNull(loadedPerson)
        assertEquals("Boris", loadedPerson[Person.name])

        val children = loadedPerson[Person.childs]
        assertNotNull(children)
        // Note: Depending on framework implementation, unfiltered childs might still be loaded if they were in session,
        // but the query itself will have the filter.
        assertEquals(1, children.size)
        assertEquals("Родион", children[0][Child.name])

        println("Person: ${loadedPerson[Person.name]}, Filtered Children count: ${children.size}")
        children.forEach { println("Child: ${it[Child.name]}") }
    }
}
