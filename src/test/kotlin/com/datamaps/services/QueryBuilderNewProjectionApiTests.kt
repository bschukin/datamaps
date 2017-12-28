package com.datamaps.services

import com.datamaps.*
import com.datamaps.maps.on
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class QueryBuilderNewProjectionApiTests : BaseSpringTests() {


    @Test
            //простейшие тесты на квери: на лысую таблицу (без вложенных сущностей)
    fun testBuildQuery01() {
        val dp = Gender.dice { }
        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t JIRA_GENDER1.ID  AS  ID1,  JIRA_GENDER1.GENDER  AS  GENDER1,  " +
                "JIRA_GENDER1.IS_CLASSIC  AS  IS_CLASSIC1\n" +
                "FROM JIRA_GENDER as JIRA_GENDER1", q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }

    @Test(invocationCount = 1)//простейшие тесты на квери: на таблицу c вложенными сущностями M-1
    fun testBuildQuery02() {

        var dp = Worker
                .dice {
                    scalars().withRefs()
                    +name
                    gender {
                        +gender
                    }
                }

        //1 тест на  структуру по которой построится запрос
        var qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp, null)

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
                println("${resultSet.getInt("ID1")}")
            }
        })

        //тоже самое но со слайсами
        dp = Worker
                .dice {
                    scalars().withRefs()
                    gender {
                        +gender
                    }
                }

        //1 тест на  структуру по которой построится запрос
        qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp, null)

        assertEquals(qr.selectColumns.size, 5)

        //2 строим сам запрос
        val q2 = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q2.sql, q.sql)
    }


    @Test(invocationCount = 1)//обрежем jiraWorker поосновательней
    fun testBuildQuery03() {
        var dp = Worker
                .dice {
                    +name
                    gender {
                        +gender
                    }
                }

        //1 тест на  структуру по которой построится запрос
        var qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp, null)

        assertEquals(qr.selectColumns.size, 4)

        //тоже самое но со слайсами
        dp = Worker.dice {
            +name
            gender {
                +gender
            }
        }


        //1 тест на  структуру по которой построится запрос
        qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp, null)

        assertEquals(qr.selectColumns.size, 4)

    }


    @Test(invocationCount = 1)//собираем инфо-п
    fun testBuildQuery05() {
        var dp = StaffUnit
                .dice {
                    scalars().withRefs()
                    +name
                    worker {
                        scalars().withRefs()
                    }
                    +gender
                }

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp, null)

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
                println("${resultSet.getInt("ID1")}")
            }
        })


    }

    @Test(invocationCount = 1)//JiraDepartment: структура-дерево
    fun testBuildQuery04() {
        var dp = Department
                .dice {
                    +name
                    parent {
                        name
                    }
                }

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp, null)

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
                println("${resultSet.getInt("ID1")}")
            }
        })
    }

    @Test(invocationCount = 1)//Коллеция 1-N
    fun testBuildQuery06() {
        var dp = Project
                .dice {
                    scalars().withRefs()
                    tasks {
                        scalars().withRefs()
                    }
                }

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp)

        assertEquals(qr.selectColumns.size, 4)
        assertEquals(qr.joins.size, 1)

        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t JIRA_PROJECT1.ID  AS  ID1,  JIRA_PROJECT1.NAME  AS  NAME1,  JIRA_TASK1.ID  AS  ID2,  JIRA_TASK1.NAME  AS  NAME2\n" +
                "FROM JIRA_PROJECT as JIRA_PROJECT1\n" +
                "LEFT JOIN JIRA_TASK as JIRA_TASK1 ON JIRA_PROJECT1.ID=JIRA_TASK1.JIRA_PROJECT_ID")
    }

    @Test
    fun testBuildQuery07WithId() {
        var dp = Project
                .dice {
                    withId(1L)
                    scalars().withRefs()
                    tasks {
                        scalars().withRefs()
                    }
                }
        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp)

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

    @Test//Тест на использования алиасов
    fun testBuildQuery08WithAlias01() {
        var dp = Project
                .dice {
                    withId(1)
                    alias("JP")
                    scalars().withRefs()
                    tasks {
                        scalars().withRefs()
                    }
                }

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp)

        assertEquals(qr.selectColumns.size, 4)
        assertEquals(qr.joins.size, 1)

        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t JP.ID  AS  ID1,  JP.NAME  AS  NAME1,  JIRA_TASK1.ID  AS  ID2,  JIRA_TASK1.NAME  AS  NAME2\n" +
                "FROM JIRA_PROJECT as JP\n" +
                "LEFT JOIN JIRA_TASK as JIRA_TASK1 ON JP.ID=JIRA_TASK1.JIRA_PROJECT_ID \n" +
                "WHERE JP.ID = :_id1")
    }

    @Test//Тест на использования алиасов
    fun testBuildQuery08WithAlias02() {
        val dp = Project
                .dice {
                    withId(1)
                    alias("JP")
                    scalars().withRefs()
                    tasks {
                        alias("JT")
                        scalars().withRefs()
                    }
                }

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp)

        assertEquals(qr.selectColumns.size, 4)
        assertEquals(qr.joins.size, 1)

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t JP.ID  AS  ID1,  JP.NAME  AS  NAME1,  JT.ID  AS  ID2,  JT.NAME  AS  NAME2\n" +
                "FROM JIRA_PROJECT as JP\n" +
                "LEFT JOIN JIRA_TASK as JT ON JP.ID=JT.JIRA_PROJECT_ID \n" +
                "WHERE JP.ID = :_id1")
    }


    @Test
    fun testOnlyId() {
        val dp = Project
                .dice {
                    withId(1L)
                    onlyId()
                }

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp)


        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)

        assertBodyEquals(q.sql, "SELECT \n" +
                "\t jira_project1.id  AS  id1\n" +
                "FROM jira_project as jira_project1 \n" +
                "WHERE jira_project1.id = :_id1  ")
    }


    @Test
    fun testProjectionWithFieldsFlatEnumerarion() {
        val dp = on(StaffUnit).with(
                !StaffUnit.name,
                !StaffUnit.worker().name,
                !StaffUnit.worker().email,
                !StaffUnit.gender().gender
        )


        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp)


        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)

        assertBodyEquals(q.sql, "SELECT \n" +
                "\t  JIRA_STAFF_UNIT1.\"ID\"  AS  ID1,  JIRA_STAFF_UNIT1.\"NAME\"  AS  NAME1,  JIRA_WORKER1.\"ID\"  AS  ID2,  JIRA_WORKER1.\"NAME\"  AS  NAME2,  JIRA_WORKER1.\"EMAIL\"  AS  EMAIL1,  JIRA_GENDER1.\"ID\"  AS  ID3,  JIRA_GENDER1.\"GENDER\"  AS  GENDER1\n" +
                "FROM JIRA_STAFF_UNIT as JIRA_STAFF_UNIT1\n" +
                "LEFT JOIN JIRA_WORKER as JIRA_WORKER1 ON JIRA_STAFF_UNIT1.\"WORKER_ID\"=JIRA_WORKER1.\"ID\" \n" +
                "LEFT JOIN JIRA_GENDER as JIRA_GENDER1 ON JIRA_STAFF_UNIT1.\"GENDER_ID\"=JIRA_GENDER1.\"ID\"  ")
    }

}