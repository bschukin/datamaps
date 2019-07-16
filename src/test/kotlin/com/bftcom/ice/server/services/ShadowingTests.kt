package com.bftcom.ice.server.services

import com.bftcom.ice.common.State
import com.bftcom.ice.common.StateMachine
import com.bftcom.ice.common.maps.dataMapToString
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Gender
import com.bftcom.ice.server.Person
import com.bftcom.ice.server.Person.email
import com.bftcom.ice.server.assertBodyEquals
import com.bftcom.ice.server.util.printAsJson
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Created by Щукин on 03.11.2017.
 */
open class ShadowingTests : BaseSpringTests() {


    @Test
    fun testShadowingIdea() {

        val man = Gender.shadow { it.id = 2; it[name] = "man"; it[isClassic] = true   }
        val johnLennon = Person.shadow { it[name] = "John Lennon"; it[gender] = man }

        println(johnLennon.dataMapToString())
    }


    @Autowired
    lateinit var testDao: TestDao

    @Test
    fun testShadowingWithDao() {
        assertBodyEquals(testDao.someLogic(), "john@google.com")
    }


}

@Component
@org.springframework.context.annotation.Lazy
class TestDao
{
    val man = Gender.shadow { it.id = 2; it[name] = "man"; it[isClassic] = true   }
    val johnLennon = Person.shadow { it[name] = "John Lennon"; it[gender] = man }

    fun someLogic():String
    {
        //хоп, объект у нас есть
        println( johnLennon[email] )
        return johnLennon[email]!!
    }

}