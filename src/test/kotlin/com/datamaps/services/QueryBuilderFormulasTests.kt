package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.mappings.DataProjection
import com.datamaps.mappings.f
import org.springframework.beans.factory.annotation.Autowired
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class QueryBuilderFormulasTests : BaseSpringTests() {

    @Autowired
    lateinit var queryBuilder: QueryBuilder
    @Autowired
    lateinit var queryExecutor: QueryExecutor

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
        val e = queryExecutor.findAll(q)
        e.forEach {
            print(e)
        }
    }


}