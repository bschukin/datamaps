package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.BaseSpringTests
import com.bftcom.ice.datamaps.Person
import com.bftcom.ice.datamaps.assertBodyEquals
import com.bftcom.ice.datamaps.core.util.IfSpringProfileActive
import org.junit.Test

/**
 * Created by Щукин on 03.11.2017.
 */
open class BlobsTests : BaseSpringTests()
{

    @Test
    @IfSpringProfileActive("hsqldb", "oracle")
    fun testInsertAndReadClob()
    {
        val wiki = "Through a number of successful wars, he expanded the Tsardom " +
                "into a much larger empire that became a major European power"

       val p = Person.create {
           it[name] = "Peter I"
           it[bio] = wiki
       }
        dataService.flush()
        println(p.id)

        val pp = find_(Person.slice {
            withId(p.id)
            withBlobs()
        })
        println(pp[Person.id])
        println(pp[Person.bio])

        assertBodyEquals(wiki, pp[Person.bio])
    }

    @Test
    fun testInsertAndReadBlob()
    {
        val image = "Peter's reforms made a lasting impact on Russia, and many institutions " +
                "of Russian government trace their origins to his reign.".toByteArray()
        val bytearray = ByteArray(image.length)
        var i = 0
        image.forEach {
            bytearray.set(i++, it.toByte())
        }

        val p = Person.create {
            it[name] = "Peter I"
            it[photo] = bytearray
        }
        dataService.flush()
        println(p.id)

        val pp = find_(Person.slice {
            withId(p.id)
            withBlobs()
        })
        println(pp[Person.id])
        println(pp[Person.photo])

        val image2 = String(pp[Person.photo]!!)
        println(image2)
        assertBodyEquals(image, image2)
    }
}