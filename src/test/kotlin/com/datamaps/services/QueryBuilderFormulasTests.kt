package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.mappings.DataProjection
import org.springframework.beans.factory.annotation.Autowired
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class QueryBuilderFormulasTests : BaseSpringTests() {

    @Autowired
    lateinit var queryBuilder: QueryBuilder

    //базовый тест на
    @Test fun testSelectCOlumn01() {
        val dp = DataProjection("JiraGender")
                .formula("genderCaption", """
                    case when {{id}}=1 then 'Ж' when {{id}}=2 then 'М' else 'О' end
                """)
        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, i ->
            run {
                println("${resultSet.getString("genderCaption")}")
            }
        })
    }


}