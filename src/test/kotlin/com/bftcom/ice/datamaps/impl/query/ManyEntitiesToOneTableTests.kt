package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.common.maps.*
import com.bftcom.ice.assertBodyEquals
import com.bftcom.ice.datamaps.*
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Gender
import com.bftcom.ice.server.Person
import com.bftcom.ice.datamaps.impl.mappings.DataMappingsService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired


open class MappingManyEntitiesToOneTable : BaseSpringTests() {

    @Autowired
    lateinit var dataMappingService: DataMappingsService

    object Person2 : MFS<Person2>("Person2") {
        init {
            table = "Person"
        }

        val id = Field.id()
        val name = Field.string("name")
        val lastName = Field.string("lastName")
        val age = Field.int("age")
        val gender = Field.referenceNN("gender", Gender)
    }


    @Test
    fun checkGenedatedMappingForEntitiesOfOneTable() {
        val p2dm = dataMappingService.getDataMapping(Person2.entity)
        val pdm = dataMappingService.getDataMapping(Person.entity)
        assertNotNull(p2dm)
        assertNotNull(pdm)
        assertFalse(p2dm === pdm)

        assertTrue(p2dm.name == Person2.entity)
        assertTrue(p2dm.listsGroup.fields.size == 0)
        assertTrue(p2dm.refsGroup.fields.size == 1)

        assertTrue(pdm.listsGroup.fields.size == 1)


    }

    @Test
    fun selectSecondEntityForOneTable() {
        sqlStatistics.restart()
        val person = dataService.find_(Person2.withId(1).withRefs())
        assertNotNull(person)
        assertTrue(person.entity == Person2.entity)

        val sql = sqlStatistics.lastQuery()
        println(sql)
        assertBodyEquals("""
           SELECT
	    PERSON1."ID"  AS  ID1,  PERSON1."NAME"  AS  NAME1,  PERSON1."LAST_NAME"  AS  LAST_NAME1,
        PERSON1."AGE"  AS  AGE1,  GENDER1."ID"  AS  ID2,  GENDER1."NAME"  AS  NAME2,
        GENDER1."IS_CLASSIC"  AS  IS_CLASSIC1
        FROM PERSON PERSON1
        LEFT JOIN GENDER  GENDER1 ON PERSON1."GENDER_ID"=GENDER1."ID"
        WHERE PERSON1.ID = :_id1
            """, sql)
    }

    @Test
    fun updateSecondEntityForOneTable() {
        val person = dataService.find_(Person2.withId(1).withRefs())
        assertNotNull(person)

        person[{ name }] = "lady madonna"

        dataService.flush()
        val person2 = dataService.find_(Person2.withId(1).withRefs())
        assertNotNull(person2)
        assertTrue(person2[{ name }] == "lady madonna")

    }

    @Test
    fun deleteSecondEntityForOneTable() {
        val person = dataService.find_(Person2.withId(3).withRefs())
        assertNotNull(person)

        dataService.delete(person)
        dataService.flush()

        val person2 = dataService.find(Person2.withId(3).withRefs())
        assertNull(person2)

    }

    @Test
    fun dynamicallyCreateEntityAndRegisterIt() {
        val provider = object : IServerFieldSetProvider {

            override fun staticFields(fieldSet: FieldSet): Map<String, Field<*, *>> {
                return mapOf(
                        "id" to Field.id(),
                        "name" to Field.string("name"),
                        "age" to Field.int("age"))
            }
            override fun findFieldSetDefinition(name: String): MappingFieldSet<*>? {
                return MappingFieldSet<FieldSet>("person3")
                        .apply { table = "PERSON" }
            }

            override fun canHandleStaticFields(fieldSet: FieldSet): Boolean {
                return fieldSet.entity.equals("person3", true)
            }

            override fun entities(): List<String> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun findDynamicField(entity: String, path: List<String>): Field<*, *>? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        FieldSetProviders.registerFieldSetProvider(provider)

        try {
            sqlStatistics.restart()
            val person = dataService.find(on("Person3").id(1))
            assertNotNull(person)


            assertBodyEquals("""
                SELECT
                        PERSON1."ID"  AS  ID1,  PERSON1."NAME"  AS  NAME1,
                            PERSON1."AGE"  AS  AGE1
                            FROM PERSON PERSON1
                            WHERE PERSON1.ID = :_id1""", sqlStatistics.lastQuery())
        } finally {
            FieldSetProviders.unregisterFieldSetProvider(provider)
        }

    }

}