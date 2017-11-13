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
                "\t JIRAGENDER1.ID  AS  ID1,  JIRAGENDER1.GENDER  AS  GENDER1,  JIRAGENDER1.ISCLASSIC  AS  ISCLASSIC1\n" +
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

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuilder.QueryBuildContext()
        queryBuilder.buildDataProjection(qr, dp, null)

        assertEquals(qr.selectColumns.size, 6)

        //2 строим сам запрос
        var q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t JIRAWORKER1.ID  AS  ID1,  JIRAWORKER1.NAME  AS  NAME1,  JIRAWORKER1.EMAIL  AS  EMAIL1,  JIRAWORKER1.GENDERID  AS  GENDERID1,  JIRAGENDER1.ID  AS  ID2,  JIRAGENDER1.GENDER  AS  GENDER1\n" +
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

    @Test(invocationCount = 1)//обрежем jiraWorker поосновательней
    fun testBuildQuery03() {
        var dp = DataProjection("JiraWorker")
                .field("name")
                .field("gender")
                /*  */.inner()
                /*      */.field("gender")
                /*  */.end()

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuilder.QueryBuildContext()
        queryBuilder.buildDataProjection(qr, dp, null)

        assertEquals(qr.selectColumns.size, 5)
    }


    @Test(invocationCount = 1)//собираем инфо-п
    fun testBuildQuery05() {
        var dp = DataProjection("JiraStaffUnit")
                .gfull()
                .field("name")
                .field("worker")
                /*  */.inner()
                /*      */.gfull()
                /*  */.end()
                .field("gender")

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuilder.QueryBuildContext()
        queryBuilder.buildDataProjection(qr, dp, null)

        assertEquals(qr.selectColumns.size, 14)
        assertEquals(qr.joins.size, 3)

        //2 строим сам запрос
        var q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t JIRASTAFFUNIT1.ID  AS  ID1,  JIRASTAFFUNIT1.NAME  AS  NAME1,  JIRASTAFFUNIT1.WORKER_ID  AS  WORKER_ID1,  " +
                "JIRAWORKER1.ID  AS  ID2,  JIRAWORKER1.NAME  AS  NAME2,  JIRAWORKER1.EMAIL  AS  EMAIL1,  JIRAWORKER1.GENDERID  AS  GENDERID1,  " +
                "JIRAGENDER1.ID  AS  ID3,  JIRAGENDER1.GENDER  AS  GENDER1,  JIRAGENDER1.ISCLASSIC  AS  ISCLASSIC1,  " +
                "JIRASTAFFUNIT1.GENDERID  AS  GENDERID2,  JIRAGENDER2.ID  AS  ID4,  JIRAGENDER2.GENDER  AS  GENDER2,  " +
                "JIRAGENDER2.ISCLASSIC  AS  ISCLASSIC2\n" +
                "FROM JIRASTAFFUNIT as JIRASTAFFUNIT1\n" +
                "LEFT JOIN JIRAWORKER as JIRAWORKER1 ON JIRASTAFFUNIT1.WORKER_ID=JIRAWORKER1.ID \n" +
                "LEFT JOIN JIRAGENDER as JIRAGENDER1 ON JIRAWORKER1.GENDERID=JIRAGENDER1.ID \n" +
                "LEFT JOIN JIRAGENDER as JIRAGENDER2 ON JIRASTAFFUNIT1.GENDERID=JIRAGENDER2.ID", q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID")}==>${resultSet.getString("NAME")}, " +
                        resultSet.getString("GENDERID"))
            }
        })


    }

    @Test(invocationCount = 1)//JiraDepartment: структура-дерево
    fun testBuildQuery04() {
        var dp = DataProjection("JiraDepartment")
                .field("name")
                .field("parent")
                /*  */.inner()
                /*      */.field("name")
                ///*      */.field("parent")
                /*  */.end()

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuilder.QueryBuildContext()
        queryBuilder.buildDataProjection(qr, dp, null)

        assertEquals(qr.selectColumns.size, 5)
        //2 строим сам запрос
        var q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t JIRADEPARTMENT1.ID  AS  ID1,  JIRADEPARTMENT1.NAME  AS  NAME1,  JIRADEPARTMENT1.PARENTID  AS  PARENTID1,  JIRADEPARTMENT2.ID  AS  ID2,  JIRADEPARTMENT2.NAME  AS  NAME2\n" +
                "FROM JIRADEPARTMENT as JIRADEPARTMENT1\n" +
                "LEFT JOIN JIRADEPARTMENT as JIRADEPARTMENT2 ON JIRADEPARTMENT1.PARENTID=JIRADEPARTMENT2.ID", q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID")}==>${resultSet.getString("NAME")}, " +
                        resultSet.getString("GENDERID"))
            }
        })
    }

}