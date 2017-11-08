package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.mappings.DataProjection
import org.springframework.beans.factory.annotation.Autowired
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class QueryBuilderTests : BaseSpringTests() {

    @Autowired
    lateinit var queryBuilder: QueryBuilder

    @Test
            //простейшие тесты на квери: на лысую таблицу (без вложенных сущностей)
    fun testBuildQuery01() {
        var dp = DataProjection("JiraGender")
        var q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\tJIRAGENDER1.ID, JIRAGENDER1.GENDER, JIRAGENDER1.ISCLASSIC\n" +
                "FROM JIRAGENDER as JIRAGENDER1", q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID")}==>${resultSet.getString("GENDER")}, " +
                        resultSet.getString("ISCLASSIC"))
            }
        })
    }


}