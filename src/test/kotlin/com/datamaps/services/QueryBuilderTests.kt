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
        val dp = DataProjection("JiraGender")
        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t JIRA_GENDER1.ID  AS  ID1,  JIRA_GENDER1.GENDER  AS  GENDER1,  " +
                "JIRA_GENDER1.IS_CLASSIC  AS  IS_CLASSIC1\n" +
                "FROM JIRA_GENDER as JIRA_GENDER1", q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID")}==>${resultSet.getString("GENDER")}, " +
                        resultSet.getString("IS_CLASSIC"))
            }
        })
    }

    @Test(invocationCount = 1)//простейшие тесты на квери: на таблицу c вложенными сущностями M-1
    fun testBuildQuery02() {
        var dp = DataProjection("JiraWorker")
                .default().refs()
                .field("gender")
                /*  */.inner()
                /*      */.field("gender")
                /*  */.end()

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildDataProjection(qr, dp, null)

        assertEquals(qr.selectColumns.size, 5)

        //2 строим сам запрос
        var q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t JIRA_WORKER1.ID  AS  ID1,  JIRA_WORKER1.NAME  AS  NAME1,  JIRA_WORKER1.EMAIL  AS  EMAIL1,  " +
                "JIRA_GENDER1.ID  AS  ID2,  JIRA_GENDER1.GENDER  AS  GENDER1\n" +
                "FROM JIRA_WORKER as JIRA_WORKER1\n" +
                "LEFT JOIN JIRA_GENDER as JIRA_GENDER1 ON JIRA_WORKER1.GENDER_ID=JIRA_GENDER1.ID", q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID")}==>${resultSet.getString("NAME")}, " +
                        resultSet.getString("ID2"))
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
        val qr = QueryBuildContext()
        queryBuilder.buildDataProjection(qr, dp, null)

        assertEquals(qr.selectColumns.size, 4)
    }


    @Test(invocationCount = 1)//собираем инфо-п
    fun testBuildQuery05() {
        var dp = DataProjection("JiraStaffUnit")
                .default().refs()
                .field("name")
                .field("worker")
                /*  */.inner()
                /*      */.default().refs()
                /*  */.end()
                .field("gender")

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildDataProjection(qr, dp, null)

        assertEquals(qr.selectColumns.size, 11)
        assertEquals(qr.joins.size, 3)

        //2 строим сам запрос
        var q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t JIRA_STAFF_UNIT1.ID  AS  ID1,  JIRA_STAFF_UNIT1.NAME  AS  NAME1,  JIRA_WORKER1.ID  AS  ID2,  JIRA_WORKER1.NAME  AS  NAME2,  JIRA_WORKER1.EMAIL  AS  EMAIL1,  JIRA_GENDER1.ID  AS  ID3,  JIRA_GENDER1.GENDER  AS  GENDER1,  JIRA_GENDER1.IS_CLASSIC  AS  IS_CLASSIC1,  JIRA_GENDER2.ID  AS  ID4,  JIRA_GENDER2.GENDER  AS  GENDER2,  JIRA_GENDER2.IS_CLASSIC  AS  IS_CLASSIC2\n" +
                "FROM JIRA_STAFF_UNIT as JIRA_STAFF_UNIT1\n" +
                "LEFT JOIN JIRA_WORKER as JIRA_WORKER1 ON JIRA_STAFF_UNIT1.WORKER_ID=JIRA_WORKER1.ID \n" +
                "LEFT JOIN JIRA_GENDER as JIRA_GENDER1 ON JIRA_WORKER1.GENDER_ID=JIRA_GENDER1.ID \n" +
                "LEFT JOIN JIRA_GENDER as JIRA_GENDER2 ON JIRA_STAFF_UNIT1.GENDER_ID=JIRA_GENDER2.ID", q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID")}==>${resultSet.getString("NAME")}, " +
                        resultSet.getString("ID2"))
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
                ///*      */.parentLinkField("parent")
                /*  */.end()

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildDataProjection(qr, dp, null)

        assertEquals(qr.selectColumns.size, 4)
        //2 строим сам запрос
        var q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t JIRA_DEPARTMENT1.ID  AS  ID1,  JIRA_DEPARTMENT1.NAME  AS  NAME1,  JIRA_DEPARTMENT2.ID  AS  ID2,  JIRA_DEPARTMENT2.NAME  AS  NAME2\n" +
                "FROM JIRA_DEPARTMENT as JIRA_DEPARTMENT1\n" +
                "LEFT JOIN JIRA_DEPARTMENT as JIRA_DEPARTMENT2 ON JIRA_DEPARTMENT1.PARENT_ID=JIRA_DEPARTMENT2.ID", q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID")}==>${resultSet.getString("NAME")}, " +
                        resultSet.getString("ID2"))
            }
        })
    }

    @Test(invocationCount = 1)//Коллеция 1-N
    fun testBuildQuery06() {
        var dp = DataProjection("JiraProject")
                .full()
                /*  */.field("jiraTasks")
                /*  *//*  */.inner().full().end()

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildDataProjection(qr, dp)

        assertEquals(qr.selectColumns.size, 4)
        assertEquals(qr.joins.size, 1)

        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t JIRA_PROJECT1.ID  AS  ID1,  JIRA_PROJECT1.NAME  AS  NAME1,  JIRA_TASK1.ID  AS  ID2,  JIRA_TASK1.NAME  AS  NAME2\n" +
                "FROM JIRA_PROJECT as JIRA_PROJECT1\n" +
                "LEFT JOIN JIRA_TASK as JIRA_TASK1 ON JIRA_PROJECT1.ID=JIRA_TASK1.JIRA_PROJECT_ID")
    }

    @Test//Коллеция 1-N
    fun testBuildQuery07WithId() {
        var dp = DataProjection("JiraProject", 1)
                .full()
                /*  */.field("jiraTasks")
                /*  *//*  */.inner().full().end()

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildDataProjection(qr, dp)

        assertEquals(qr.selectColumns.size, 4)
        assertEquals(qr.joins.size, 1)

        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t JIRA_PROJECT1.ID  AS  ID1,  JIRA_PROJECT1.NAME  AS  NAME1,  JIRA_TASK1.ID  AS  ID2,  JIRA_TASK1.NAME  AS  NAME2\n" +
                "FROM JIRA_PROJECT as JIRA_PROJECT1\n" +
                "LEFT JOIN JIRA_TASK as JIRA_TASK1 ON JIRA_PROJECT1.ID=JIRA_TASK1.JIRA_PROJECT_ID \n" +
                "WHERE JIRA_PROJECT1.ID = :_id1")
    }

}