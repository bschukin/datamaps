package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.MFS
import com.bftcom.ice.datamaps.and
import com.bftcom.ice.datamaps.misc.Date
import com.bftcom.ice.datamaps.misc.Timestamp
import com.bftcom.ice.datamaps.misc.__infinity
import com.bftcom.ice.datamaps.misc.infinity__
import com.bftcom.ice.datamaps.BaseSpringTests
import com.bftcom.ice.datamaps.DateTest
import com.bftcom.ice.datamaps.IfSpringProfileActive
import com.bftcom.ice.datamaps.core.util.dataMapFromJson
import com.bftcom.ice.datamaps.core.util.printAsJson
import com.bftcom.ice.datamaps.core.util.toJson
import org.junit.Test
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
open class DateTests : BaseSpringTests() {

    @Test
    fun caseCrudWithDateAndTimeStamp() {

        //создаем объектик  с датой и таймстампом
        val dateTest = DateTest.create {
            it[timestamp] = Timestamp()
            it[tdate] = Date()
        }
        assert((dateTest[DateTest.timestamp] as Date).date.hours > 0 || (dateTest[DateTest.timestamp] as Date).date.minutes > 0)
        assert((dateTest[DateTest.tdate] as Date).date.hours > 0 || (dateTest[DateTest.tdate] as Date).date.minutes > 0)//увы так
        dataService.flush()

        //проверяем что из базы приезжает дейтсвительно дата и метка времени
        val dateTest1 = dataService.find_(DateTest.withId(dateTest.id))

        assert((dateTest[DateTest.timestamp] as Date).date.hours ==
                (dateTest1[DateTest.timestamp] as Date).date.hours)
        assert((dateTest[DateTest.timestamp] as Date).date.minutes ==
                (dateTest1[DateTest.timestamp] as Date).date.minutes)
        assert((dateTest[DateTest.timestamp] as Date).date.seconds ==
                (dateTest1[DateTest.timestamp] as Date).date.seconds)

        assert(0 ==
                (dateTest1[DateTest.tdate] as Date).date.hours)
        assert(0 ==
                (dateTest1[DateTest.tdate] as Date).date.minutes)
        assert(0 ==
                (dateTest1[DateTest.tdate] as Date).date.seconds)


        //тестируем корректную работу с датами при конвертации - деконвертации из json

        val dataMapFromJson = dataMapFromJson(dateTest1.toJson(false))
        val dateTest3 = DateTest.create {
            it[tdate] = dataMapFromJson[DateTest.tdate]
            it[timestamp] = dataMapFromJson[DateTest.timestamp]

        }
        dataService.flush()

        val dateTest31 = dataService.find_(DateTest.withId(dateTest3.id))
        assert((dateTest31[DateTest.timestamp] as Date).date.hours ==
                (dateTest1[DateTest.timestamp] as Date).date.hours)
        assert((dateTest31[DateTest.timestamp] as Date).date.minutes ==
                (dateTest1[DateTest.timestamp] as Date).date.minutes)
        assert((dateTest31[DateTest.timestamp] as Date).date.seconds ==
                (dateTest1[DateTest.timestamp] as Date).date.seconds)

        assert(0 ==
                (dateTest31[DateTest.tdate] as Date).date.hours)
        assert(0 ==
                (dateTest31[DateTest.tdate] as Date).date.minutes)
        assert(0 ==
                (dateTest31[DateTest.tdate] as Date).date.seconds)

        assert((dateTest31[DateTest.tdate] as Date).date.day ==
                (dateTest1[DateTest.tdate] as Date).date.day)
    }


    @Test
    @IfSpringProfileActive("postgresql")
    fun testInfinityDateQuery() {
        val t1 = dataService.find_(
                Temporal1.filter { name eq "test01" })

        t1.printAsJson()

        t1.with {
            assertTrue(it[startDate] == __infinity)
            assertTrue(it[endDate] == infinity__)
        }


        val t2 = dataService.find_(
                Temporal1.filter { startDate eq __infinity }
        )
        assertNotNull(t2)

        val t3 = dataService.find_(
                Temporal1.filter { endDate eq infinity__ }
        )
        assertNotNull(t3)


        val currDate = Date()
        val t4 = dataService.find_(
                Temporal1.filter { { startDate le currDate} and { endDate gt currDate} }
        )
        assertNotNull(t4)
    }


    object Temporal1 : MFS<Temporal1>("TEMPORAL1") {
        val name = Field.string("name")
        val startDate = Field.date("startDate")
        val endDate = Field.date("endDate")
    }
}