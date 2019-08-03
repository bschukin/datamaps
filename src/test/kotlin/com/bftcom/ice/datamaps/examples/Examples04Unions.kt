package com.bftcom.ice.datamaps.examples

import com.bftcom.ice.datamaps.*
import org.junit.Assert
import org.junit.Test


/***
 * Примеры на работу с датамапсами и проекциями
 * (Fieldset-API)
 */
open class Examples04Unions : BaseSpringTests() {

    @Test
    fun `example of an union projection`() {

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

    }

    @Test
    fun `mix entities in union`() {

        val dp1 = Person.slice {
            +name
            filter { name eq "Oleg Gazmanov" }
        }

        val dp2 = City.slice {
            +title
            filter { title eq "Novosibirsk" }
        }

        val res = dataService.unionAll(dp1 UNION dp2)
    }

    @Test
    fun `filter on union`() {

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

    }

    @Test
    fun `order in union`() {

        val u =
                (Person.slice {
                    +name
                } UNION City.slice {
                    +title
                }).order(f(DataMapF.entityDiscriminator), f("name"))

        val res = dataService.unionAll(u)
    }

}