package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.FooBar
import com.bftcom.ice.datamaps.impl.util.printAsJson
import org.junit.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
open class GroupByTests : BaseSpringTests() {

    @Test
    fun testGroupByQuery() {

        FooBar { it[name] = "Beatles"; it[value] = 1100 }
        FooBar { it[name] = "Beatles"; it[value] = 2000 }
        FooBar { it[name] = "Beatles"; it[value] = 4000 }
        FooBar { it[name] = "Metallica"; it[value] = 100 }
        FooBar { it[name] = "Metallica"; it[value] = 100 }
        FooBar { it[name] = "Karel Got"; it[value] = 1000000 }

        dataService.flush()

        val dp = FooBar
                .slice {
                    name
                    count(name)
                    sum(value, "tuhliyaPomodors")
                    max(value); min(value); avg(value)

                }
                .groupBy(FooBar.name)
                .order(FooBar.name)

        val ds = dataService.findAll(dp)

        ds.printAsJson(false)

        assertTrue(ds.size==3)
        assertTrue(ds[0][{ name}]=="Beatles")
        assertTrue(ds[0]["tuhliyaPomodors"].toString()=="7100")
        assertTrue(ds[0]["max_value"].toString()=="4000")

        assertTrue(ds[2][{ name}]=="Metallica")
        assertTrue(ds[2]["tuhliyaPomodors"].toString()=="200")
        assertTrue(ds[2]["max_value"].toString()=="100")
    }

}

