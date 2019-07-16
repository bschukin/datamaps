package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.MFS
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Person
import com.bftcom.ice.server.assertBodyEquals
import com.bftcom.ice.datamaps.core.util.printAsJson
import org.junit.Test
import org.springframework.test.annotation.DirtiesContext

/**
 * Created by Щукин on 03.11.2017.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
open class RemappingWithViewsTests : BaseSpringTests() {


    //тестим обращение ко вью
    @Test
    fun testViewUsage01() {

        val res = dataService.find_(PersonView.on().filter {
            Person.name eq "Madonna"
        })
        assertNotNull(res)
    }

    @Test
    //тестим ссылку на вью
    fun testViewUsage02AsReference() {

        val res = dataService.find_(
                Monument.on()
                        .withRefs()
                .filter {
                    Monument.name eq "John the Unknown"
                })

        res.printAsJson()

        assertNotNull(res)
        assertNotNull(res[{ person }])

    }

    @Test
    //тестим коллекцию из вью на обычный объект
    fun testViewUsage03ListInView() {

        sqlStatistics.restart()

        val res = dataService.find_(PersonView2.on()
                .withCollections()
                .filter {
                    PersonView2.name eq "John Lennon"
                })

        res.printAsJson()

        assertNotNull(res)
        assertTrue(res[PersonView2.monuments].size == 1)

        println(sqlStatistics.queries()[0].sql)
        assertBodyEquals("""
            SELECT PERSON_VIEW21."ID"  AS  ID1,  PERSON_VIEW21."NAME"  AS  NAME1,
            MONUMENT1."ID"  AS  ID2,  MONUMENT1."NAME"  AS  NAME2
            FROM PERSON_VIEW2 PERSON_VIEW21
                LEFT JOIN MONUMENT  MONUMENT1 ON PERSON_VIEW21."ID"=MONUMENT1."PERSON_ID"
                WHERE PERSON_VIEW21."NAME" = :param0
            """, sqlStatistics.queries()[0].sql)

    }
}


object PersonView : MFS<PersonView>("PersonView") {
    val name = Field.string("name")
}

object Monument : MFS<Monument>("Monument") {
    val name = Field.string("name")
    val person = Field.reference("person", PersonView).apply { thisJoinColumn = "person_id" }
}

object PersonView2 : MFS<PersonView2>("PersonView2") {
    val name = Field.string("name")
    val monuments = Field.list("monuments", Monument) .apply{ thatJoinColumn = "PERSON_ID" }
}