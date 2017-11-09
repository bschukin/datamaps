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

    @Test(invocationCount = 1)//простейшие тесты на квери: на таблицу c вложенными сущностями M-1
    fun testBuildQuery02() {
        var dp = DataProjection("JiraWorker")
                .group("full")
                .field("gender")
                /*  */.inner()
                /*      */.field("gender")
                /*  */.end()

        var q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\tJIRAWORKER1.ID, JIRAWORKER1.NAME, JIRAWORKER1.EMAIL, JIRAWORKER1.GENDERID, " +
                "JIRAGENDER1.ID, JIRAGENDER1.GENDER, JIRAGENDER1.ISCLASSIC\n" +
                "FROM JIRAWORKER as JIRAWORKER1\n" +
                "LEFT JOIN JIRAGENDER as JIRAGENDER1 ON JIRAWORKER1.GENDERID=JIRAGENDER1.ID", q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID")}==>${resultSet.getString("NAME")}, " +
                        resultSet.getString("GENDERID"))
            }
        })
    }

}