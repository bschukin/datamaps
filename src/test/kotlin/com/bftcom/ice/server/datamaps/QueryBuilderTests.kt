package com.bftcom.ice.server.datamaps

import com.bftcom.ice.common.maps.*
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Department
import com.bftcom.ice.server.Person
import com.bftcom.ice.server.Person.city
import com.bftcom.ice.server.assertBodyEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Created by Щукин on 03.11.2017.
 */
open class QueryBuilderTests : BaseSpringTests() {


    @Test
            //простейшие тесты на квери: на лысую таблицу (без вложенных сущностей)
    fun testBuildQuery01() {
        val dp = on("Gender")
        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t GENDER1.ID  AS  ID1,  GENDER1.NAME  AS  NAME1,  " +
                "GENDER1.IS_CLASSIC  AS  IS_CLASSIC1\n" +
                "FROM GENDER  GENDER1", q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }

    @Test()//простейшие тесты на квери: на таблицу c вложенными сущностями M-1
    fun testBuildQuery02() {
        var dp = on("Person")
                .scalars().withRefs()
                .field("name")
                .with {
                    slice("gender")
                            .field("name")
                }

        //1 тест на  структуру по которой построится запрос
        var qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp, null)

        assertEquals(qr.selectColumns.size, 12)

        //2 строим сам запрос
        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1,  PERSON1.\"EMAIL\"  AS  EMAIL1,  " +
                "PERSON1.\"LAST_NAME\"  AS  LAST_NAME1,  PERSON1.\"AGE\"  AS  AGE1,  GENDER1.\"ID\"  " +
                "AS  ID2,  GENDER1.\"NAME\"  AS  NAME2,  " +
                "CITY1.\"ID\"  AS  ID3,  CITY1.\"TITLE\"  AS  TITLE1,  " +
                "GAME1.\"ID\"  AS  ID4,  GAME1.\"NAME\"  AS  NAME3,  GAME1.\"METACRITICID\"  AS  METACRITICID1\n" +
                "FROM PERSON PERSON1\n" +
                "LEFT JOIN GENDER  GENDER1 ON PERSON1.\"GENDER_ID\"=GENDER1.\"ID\" \n" +
                "LEFT JOIN CITY  CITY1 ON PERSON1.\"CITY_ID\"=CITY1.\"ID\" \n" +
                "LEFT JOIN GAME  GAME1 ON PERSON1.\"FAVORITE_GAME_ID\"=GAME1.\"ID\" ", q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })

        //тоже самое но со слайсами
        dp = projection("Person")
                .scalars().withRefs()
                .with {
                    slice("gender")
                            .field("name")
                }

        //1 тест на  структуру по которой построится запрос
        qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp, null)

        assertEquals(qr.selectColumns.size, 12)
        //2 строим сам запрос
        val q2 = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q2.sql, q.sql)
    }


    @Test()//обрежем Person поосновательней
    fun testBuildQuery03() {
        var dp = Projection("Person")
                .field("name")
                .with {
                    slice("gender")
                            .field("name")
                }

        //1 тест на  структуру по которой построится запрос
        var qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp, null)

        assertEquals(qr.selectColumns.size, 4)

        //тоже самое но со слайсами
        dp = projection("Person")
                .field("name")
                .with {
                    slice("gender")
                            .field("name")
                }

        //1 тест на  структуру по которой построится запрос
        qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp, null)

        assertEquals(qr.selectColumns.size, 4)

    }


    @Test()//собираем инфо-п
    fun testBuildQuery05() {
        val dp = Projection("Department")
                .scalars().withRefs()
                .field("name")
                .with {
                    slice("boss")
                            .scalars().withRefs()
                }
                .field("city")

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp, null)

        assertEquals(qr.selectColumns.size, 19)
        assertEquals(qr.joins.size, 6)

        //2 строим сам запрос
        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals( """SELECT
                DEPARTMENT1.ID  AS  ID1,  DEPARTMENT1.NAME  AS  NAME1,  DEPARTMENT2.ID  AS  ID2,
                DEPARTMENT2.NAME  AS  NAME2,  CITY1.ID  AS  ID3,  CITY1.TITLE  AS  TITLE1,  PERSON1.ID  AS  ID4,
                PERSON1.NAME  AS  NAME3,  PERSON1.EMAIL  AS  EMAIL1,  PERSON1.LAST_NAME  AS  LAST_NAME1,
                PERSON1.AGE  AS  AGE1,  GENDER1.ID  AS  ID5,
                GENDER1.NAME  AS  NAME4,  GENDER1.IS_CLASSIC  AS  IS_CLASSIC1,  CITY2.ID  AS  ID6,  CITY2.TITLE  AS  TITLE2,
                GAME1.ID  AS  ID7,  GAME1.NAME  AS  NAME5,  GAME1.METACRITICID  AS  METACRITICID1
                FROM DEPARTMENT  DEPARTMENT1
                LEFT JOIN DEPARTMENT  DEPARTMENT2 ON DEPARTMENT1.PARENT_ID=DEPARTMENT2.ID
                LEFT JOIN CITY  CITY1 ON DEPARTMENT1.CITY_ID=CITY1.ID
                LEFT JOIN PERSON  PERSON1 ON DEPARTMENT1.BOSS_ID=PERSON1.ID
                LEFT JOIN GENDER  GENDER1 ON PERSON1.GENDER_ID=GENDER1.ID
                LEFT JOIN CITY  CITY2 ON PERSON1.CITY_ID=CITY2.ID
                LEFT JOIN GAME  GAME1 ON PERSON1.FAVORITE_GAME_ID=GAME1.ID """, q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })


    }

    @Test()//Department: структура-дерево
    fun testBuildQuery04() {
        val dp = Projection("Department")
                .field("name")
                .with {
                    slice("parent")
                            .field("name")
                }

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp, null)

        assertEquals(qr.selectColumns.size, 4)
        //2 строим сам запрос
        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t DEPARTMENT1.ID  AS  ID1,  DEPARTMENT1.NAME  AS  NAME1,  DEPARTMENT2.ID  AS  ID2,  DEPARTMENT2.NAME  AS  NAME2\n" +
                "FROM DEPARTMENT  DEPARTMENT1\n" +
                "LEFT JOIN DEPARTMENT  DEPARTMENT2 ON DEPARTMENT1.PARENT_ID=DEPARTMENT2.ID", q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }

    @Test()//Коллеция 1-N
    fun testBuildQuery06() {
        val dp = projection("Project")
                .scalars().withRefs()
                .with {
                    slice("Tasks")
                            .scalars().withRefs()
                }
        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp)

        assertEquals(qr.selectColumns.size, 4)
        assertEquals(qr.joins.size, 1)

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t PROJECT1.ID  AS  ID1,  PROJECT1.NAME  AS  NAME1,  TASK1.ID  AS  ID2,  TASK1.NAME  AS  NAME2\n" +
                "FROM PROJECT  PROJECT1\n" +
                "LEFT JOIN TASK  TASK1 ON PROJECT1.ID=TASK1.PROJECT_ID")
    }

    @Test
    fun testBuildQuery07WithId() {
        val dp = projection("Project", 1)
                .scalars().withRefs()
                .with {
                    slice("Tasks")
                            .scalars().withRefs()
                }
        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp)

        assertEquals(qr.selectColumns.size, 4)
        assertEquals(qr.joins.size, 1)

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t PROJECT1.ID  AS  ID1,  PROJECT1.NAME  AS  NAME1,  TASK1.ID  AS  ID2,  TASK1.NAME  AS  NAME2\n" +
                "FROM PROJECT  PROJECT1\n" +
                "LEFT JOIN TASK  TASK1 ON PROJECT1.ID=TASK1.PROJECT_ID \n" +
                "WHERE PROJECT1.ID = :_id1")
    }

    @Test//Тест на использования алиасов
    fun testBuildQuery08WithAlias01() {
        val dp = projection("Project", 1)
                .scalars().withRefs()
                .alias("JP")
                .with {
                    slice("Tasks")
                            .scalars().withRefs()
                }
        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp)

        assertEquals(qr.selectColumns.size, 4)
        assertEquals(qr.joins.size, 1)

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t JP.ID  AS  ID1,  JP.NAME  AS  NAME1,  TASK1.ID  AS  ID2,  TASK1.NAME  AS  NAME2\n" +
                "FROM PROJECT  JP\n" +
                "LEFT JOIN TASK  TASK1 ON JP.ID=TASK1.PROJECT_ID \n" +
                "WHERE JP.ID = :_id1")
    }

    @Test//Тест на использования алиасов
    fun testBuildQuery08WithAlias02() {
        val dp = projection("Project", 1)
                .scalars().withRefs()
                .alias("JP")
                .with {
                    slice("tasks")
                            .alias("JT")
                            .scalars().withRefs()
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
                "FROM PROJECT  JP\n" +
                "LEFT JOIN TASK  JT ON JP.ID=JT.PROJECT_ID \n" +
                "WHERE JP.ID = :_id1")
    }


    @Test
    fun testOnlyId() {
        val dp = projection("Project", 1)
                .onlyId()

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp)


        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)

        assertBodyEquals(q.sql, "SELECT \n" +
                "\t project1.id  AS  id1\n" +
                "FROM project  project1 \n" +
                "WHERE project1.id = :_id1  ")
    }




    @Test
    fun testProjectionWithFieldsFlatEnumerarion() {
        val dp = on(Department).with (
                !Department.name,
                !Department.boss().name,
                !Department.boss().email,
                !Department.city().title
                )


        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp)


        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)

        assertBodyEquals(q.sql, """SELECT
	        DEPARTMENT1.ID  AS  ID1,  DEPARTMENT1.NAME  AS  NAME1,  PERSON1.ID  AS  ID2,  PERSON1.NAME  AS  NAME2,  PERSON1.EMAIL  AS  EMAIL1,  CITY1.ID  AS  ID3,  CITY1.TITLE  AS  TITLE1
            FROM DEPARTMENT  DEPARTMENT1
            LEFT JOIN PERSON  PERSON1 ON DEPARTMENT1.BOSS_ID=PERSON1.ID
            LEFT JOIN CITY  CITY1 ON DEPARTMENT1.CITY_ID=CITY1.ID""")
    }

    @Test
    fun testDeleteWithProjection() {


        val dp = Person.filter { f(Person.name) like ("%ushkin") }

        val q = queryBuilder.createDeleteQueryByDataProjection(dp)
        println(q.sql)

        assertBodyEquals(q.sql, """DELETE
            FROM PERSON  PERSON1
            WHERE PERSON1.NAME LIKE :param0""")
    }

    @Test
    fun testBuildGroupByQuery() {
        val dp = Person.slice {
            count(id)
            city {
                max(title, "max_title")
            }
        }.order(f("count_id").desc())
                .groupBy(city)

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)

        assertBodyEquals("SELECT (max(city1.title)) AS  max_title,  (count(person1.id)) AS  count_id\n" +
                "FROM person person1\n" +
                "LEFT JOIN city city1 ON person1.city_id = city1.id  \n" +
                "GROUP BY person1.city_id \n" +
                "ORDER BY (count(person1.id)) desc", q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        jdbcTemplate.query(q.sql, { resultSet, _ ->
            run {
                println("${resultSet.getString("max_title")} - ${resultSet.getInt("count_id")} человек")
            }
        })
    }

}