package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.DataMapF.Companion.entityDiscriminator
import com.bftcom.ice.datamaps.f
import com.bftcom.ice.datamaps.firstOf
import com.bftcom.ice.server.*
import junit.framework.TestCase.assertEquals
import org.junit.Test
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 */
open class UnionTests : BaseSpringTests() {

    @Resource
    private lateinit var queryUnionBuilder: QueryUnionBuilder

    @Test
    fun testUnionQuery01() {

        val dp1 = Person.slice {
            +name
            filter { name eq "Oleg Gazmanov" }
        }

        val dp2 = City.slice {
            +title
            filter { title eq "Novosibirsk" }
        }

        val res = dataService.unionAll(dp1 UNION dp2)

        res.forEach {
            println(if (it.map.containsKey("name")) it[Person.name] else it[City.title])
        }
    }

    @Test
    fun testUnionQuery02() {

        val dp1 = Person.slice {
            +name
            +gender
            filter { name eq "Oleg Gazmanov" }
        }

        val dp2 = Person.slice {
            +name
            +gender
        }

        val res = dataService.unionAll(dp1 UNION dp2)

        res.forEach {
            println(it[Person.name])
            println(it[Person.gender().name])
        }

        //
        val u =
                (Person.slice {
                    +name
                } UNION City.slice {
                    +title
                })
        //.filter(f("name") ilike "%M%")
        //.limit(10)
        //  .order(f(entityDiscriminator))

        val res2 = dataService.unionAll(u)
        assertTrue(res2.size == 8)

    }

    @Test
    fun testUnionQuery03UnionFilter() {

        val u =
                (Person.slice {
                    +name
                } UNION City.slice {
                    +title
                }).filter(
                        f("name") eq "Oleg Gazmanov" or
                                (f("name") ilike "Moscow")
                )

        val res = dataService.unionAll(u)

        res.forEach {
            println(it[Person.name] + " " + it[City.title])
        }
        assertTrue(res.size == 2)
    }

    @Test
    fun testUnionQuery031UnionWhere() {

        val u =
                (Person.slice {
                    +name
                } UNION City.slice {
                    +title
                }).where("{{name}} = :param0 or {{name}} like 'Moscow'")
                        .param("param0", "Oleg Gazmanov")


        val res = dataService.unionAll(u)

        res.forEach {
            println(it[Person.name] + " " + it[City.title])
        }
        assertTrue(res.size == 2)
    }


    @Test
    fun testUnionQuery04UnionOrders() {

        val u =
                (Person.slice {
                    +name
                } UNION City.slice {
                    +title
                }).filter(
                        f("name") eq "Oleg Gazmanov" or
                                (f("name") ilike "Moscow")
                ).order(f("name").desc())

        val res = dataService.unionAll(u)

        res.forEach {
            println(it.firstOf(Person.name, City.title))
        }
        assertEquals(res[0].firstOf(Person.name, City.title), "Oleg Gazmanov")
        assertEquals(res[1].firstOf(Person.name, City.title), "Moscow")
    }

    @Test
    fun testUnionQuery04UnionOrdersByEntityDiscriminator() {

        val u =
                (Person.slice {
                    +name
                } UNION City.slice {
                    +title
                }).filter(
                        f("name") eq "Oleg Gazmanov" or
                                (f("name") ilike "Moscow")
                ).order(f(entityDiscriminator), f("name"))

        val res = dataService.unionAll(u)

        res.forEach {
            println(it.firstOf(Person.name, City.title))
        }
        assertEquals(res[0].firstOf(Person.name, City.title), "Moscow")
        assertEquals(res[1].firstOf(Person.name, City.title), "Oleg Gazmanov")

        dataService.flush()

    }

    @Test
    fun testUnionQuery05LimitOffset() {

        val u =
                (Person.slice {
                    +name
                } UNION City.slice {
                    +title
                })
                        .filter(f("name") ilike "%M%")
                        .limit(2).offset(0)
                        .order(f(entityDiscriminator))

        val res = dataService.unionAll(u)
        //println(res.size)
        res.forEach {
            println(it.firstOf(Person.name, City.title))
        }
        assertTrue(res.size == 2)
        //       assertEquals(res[1].firstOf(Person.name, City.title), "Oleg Gazmanov")
        //assertEquals(res[0].firstOf(Person.name, City.title), "Moscow")
    }

    @Test
    fun testUnionQueryBuild01() {

        val dp1 = Person.slice {
            +name
            filter { lastName eq "borian" }
        }

        val dp2 = Game.slice {
            +name
            filter { metacriticId eq 5555 }
        }

        val q = queryUnionBuilder.createUnionQueryByDataProjections(dp1 UNION dp2)

        println(q.sql)
        assertTrue(q.params.size == 2)


    }
}