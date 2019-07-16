package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.BinaryOP
import com.bftcom.ice.datamaps.DM
import com.bftcom.ice.datamaps.DataProjection
import com.bftcom.ice.datamaps.ExpressionValue
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Person
import org.junit.Test
import org.springframework.stereotype.Service


open class ProjectionServiceCallTests : BaseSpringTests() {


    @Test
    fun testServiceCallOptionInProjection() {

        val list = dataService.findAll(
                Person.slice {
                    option().serviceCall(
                            className = MyTestProjectionHandler::class.java.name,
                            method = MyTestProjectionHandler::loadSomePersons.name)
                })

        assertTrue(list.size==2)
        assertTrue(list[0][{name}]=="Hello")
        assertTrue(list[1][{name}]=="World")


        val list2 = dataService.findAll(
                Person.slice {
                    option().serviceCall(
                            className = MyTestProjectionHandler::class.java.name,
                            method = MyTestProjectionHandler::loadSomePersons.name)
                    filter { name eq "Hello" }
                })

        assertTrue(list2.size==1)
        assertTrue(list2[0][{name}]=="Hello")
    }

}


@Service
class MyTestProjectionHandler {

    val person1 = Person{it[name]="Hello"}
    val person2 = Person{it[name]="World"}

    fun loadSomePersons(dp: DataProjection): List<DM<Person>> {

        if(dp.filter==null)
            return listOf(person1, person2)

        val value = (dp.filter as BinaryOP).right as ExpressionValue

        return listOf(person1, person2).filter { it[{ name}] == value.v }
    }

}