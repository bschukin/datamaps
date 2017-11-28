package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertEqIgnoreCase
import com.datamaps.mappings.DataProjection
import com.datamaps.mappings.f
import com.datamaps.mappings.projection
import com.datamaps.mappings.slice
import org.testng.Assert
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNotEquals
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class QueryBuilderFormulasTests : BaseSpringTests() {



    //базовый тест на
    @Test
    fun testSelectCOlumn01() {
        val dp = DataProjection("JiraGender")
                .formula("genderCaption", """
                    case when {{id}}=1 then 'Ж'
                         when {{id}}=2 then 'М'
                         else 'О' end
                """)
                .filter(f("genderCaption") eq 'Ж')

        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        val e = dataService.findAll(dp)
        e.forEach {
            print(e)
        }
        Assert.assertEquals(e.size, 1)
        assertEqIgnoreCase(e[0]["genderCaption"], "Ж")
    }


    @Test
    fun testSelectColumn01WithWhere() {
        val dp = DataProjection("JiraGender")
                .formula("genderCaption", """
                    case when {{id}}=1 then 'Ж'
                         when {{id}}=2 then 'М'
                         else 'О' end
                """)
                .where("{{genderCaption}} = :param0")
                .param("param0", 'Ж')

        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        val e = dataService.findAll(dp)
        e.forEach {
            print(e)
        }
        Assert.assertEquals(e.size, 1)
        assertEqIgnoreCase(e[0]["genderCaption"], "Ж")
    }

    @Test(invocationCount = 1)
    fun testFormula02OnNestedEntity() {
        var dp = projection("JiraWorker")
                .with {
                    slice("gender")
                            .formula("genderCaption", """
                    case when {{id}}=1 then 'Ж'
                         when {{id}}=2 then 'М'
                         else 'О' end
                """)
                }
                .filter(f("gender.genderCaption") eq "Ж")

        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        var res = dataService.findAll(dp)
        res.forEach { r -> println(r) }

        assertEquals(res.size, 2)
        assertEquals(res[0]("gender")["genderCaption"], "Ж")
        assertEquals(res[1]("gender")["genderCaption"], "Ж")
        assertNotEquals(res[0].id, res[1].id)
    }

}