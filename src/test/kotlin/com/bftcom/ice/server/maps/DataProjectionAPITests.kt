package com.bftcom.ice.common.maps

import com.bftcom.ice.server.Person
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Created by Щукин on 07.11.2017.
 */
open class DataProjectionAPITests {

    @Test
    fun testProjectionsStringApi() {

        val dp = projection("Person")
                .group("scalars").group("withCollections")
                .field("gender")
                .with { slice("gender") }

        Assert.assertEquals(dp.entity, "Person")
        Assert.assertEquals(dp.groups.size, 2)
        Assert.assertEquals(dp.fields.size, 1)

        val dp2 = projection("Department")
                .scalars().withRefs().withCollections()
                .with {
                    slice("parent")
                            .field("n")
                            .field("n")
                }
                .with {
                    slice("childs")
                            .field("n")
                            .with {
                                slice("parent")
                                        .field("n")
                            }
                }

        Assert.assertEquals(dp2.entity, "Department")
        Assert.assertEquals(dp2.groups.size, 3)
        Assert.assertEquals(dp2.fields.size, 2)
    }

    /*@Test
    fun testProjectionsFieldSetApi() {

        val dp =
                Person.on()
                        .scalars()
                        .withCollections()
                        .staticFields({ gender }, { age })
                        .field { bio }
                        .field { city }
                        .with (
                            slice{Person.gender}
                                    .field { isClassic }
                        )

        Assert.assertEquals(dp.entity, "Person")
        Assert.assertEquals(dp.groups.size, 2)

        val dp2 = projection("Department")
                .scalars().withRefs().withCollections()
                .with {
                    slice("parent")
                            .field("n")
                            .field("n")
                }
                .with {
                    slice("childs")
                            .field("n")
                            .with {
                                slice("parent")
                                        .field("n")
                            }
                }

        Assert.assertEquals(dp2.entity, "Department")
        Assert.assertEquals(dp2.groups.size, 3)
        Assert.assertEquals(dp2.staticFields.size, 2)
    }*/


    @Test
    fun testProjectionsCloning() {

        val dp = projection("JiraDepartment")
                .scalars().withRefs().withCollections()
                .with {
                    slice("parent")
                            .field("name")
                            .field("name")
                }
                .with {
                    slice("childs")
                            .field("name")
                            .with {
                                slice("parent")
                                        .field("name")
                            }
                }
                .filter {
                    (f("parent") eq "parent01") or (f("parent") eq "parent02")
                }
                .formula("formula01", "(helo w)")
                .lateral("table01", "this is sql", "key" to "value")
                .param("key02", "value02")
                .order(f("parent"))

        val dp2 = dp.copy()
        assertTrue(dp.entity == dp2.entity)
        assertTrue(dp.queryAlias == dp2.queryAlias)

        assertFalse(dp.groups === dp2.groups)
        assertTrue(dp.groups == dp2.groups)

        assertFalse(dp.formulas === dp2.formulas)
        assertTrue(dp.formulas.equals(dp2.formulas))

        assertFalse(dp.fields === dp2.fields)
        assertTrue(dp.fields == dp2.fields)


        assertFalse(dp.laterals === dp2.laterals)
        assertTrue(dp.laterals == dp2.laterals)

        assertFalse(dp.params === dp2.params)
        assertTrue(dp.params == dp2.params)

        assertTrue(dp.filter === dp2.filter)
        assertTrue((((dp2.filter as OR).left as BinaryOP).left as f).name == "parent")
        assertTrue((((dp.filter as OR).left as BinaryOP).left as f).name == "parent")

        assertTrue((((dp2.filter as OR).right as BinaryOP).right as value).v == "parent02")
        assertTrue((((dp.filter as OR).right as BinaryOP).right as value).v == "parent02")

        assertTrue(dp.where() === dp2.where())

        assertFalse(dp.orders() === dp2.orders())
        assertTrue(dp.orders() == dp2.orders())

        assertTrue(dp.limit == dp2.limit)
        assertTrue(dp.offset == dp2.offset)
    }


    @Test
    fun shouldBuildGroupByClause() {
        val dp = Person.slice {
            count(id)
            min(name)
            sum(age)
            city {
                avg(id)
                max(title)
            }
        }.order(f(Person.id).desc(), f(Person.city().title))
         .groupBy(Person.city)

        assertEquals(dp.entity, "Person")
        assertEquals(dp.groupByFields.size, 1)
        assertEquals(dp.groupByFields[0], "city")
        assertEquals(dp.formulas.size, 3)
        assertEquals(dp.formulas["count_id"], "count({{id}})")
        assertEquals(dp.formulas["min_name"], "min({{name}})")
        assertEquals(dp.formulas["sum_age"], "sum({{age}})")
        assertEquals(dp.fields.get("city")?.formulas?.size, 2)
        assertEquals(dp.fields.get("city")?.formulas?.get("avg_id"), "avg({{id}})")
        assertEquals(dp.fields.get("city")?.formulas?.get("max_title"), "max({{title}})")
    }
}