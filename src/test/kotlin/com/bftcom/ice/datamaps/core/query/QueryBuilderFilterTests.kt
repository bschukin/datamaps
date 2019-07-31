package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.*
import org.junit.Assert
import org.junit.Test

/**
 * Created by Щукин on 03.11.2017.
 */

open class QueryBuilderFilterTests : BaseSpringTests() {

    @Test
    fun testQueryAliases01() {

        var dp = DataProjectionF(Gender)
        var q = queryBuilder.createQueryByDataProjection(dp)

        var alias = q.qr.getAliasByPathFromParent(q.qr.root, null, Gender.name.n)!!
        assertBodyEquals(alias, "GENDER1")

        dp = DataProjectionF(Gender).alias("jg")
        q = queryBuilder.createQueryByDataProjection(dp)

        alias = q.qr.getAliasByPathFromParent(q.qr.root, null, Gender.name.n)!!
        assertBodyEquals(alias, "jg")

    }

    @Test()
    fun testQueryAliases02() {
        val dp = Projection(WRKR.entity)
                .scalars().withRefs()

        val q = queryBuilder.createQueryByDataProjection(dp)

        val alias = q.qr.getAliasByPathFromParent(q.qr.root, "person1", "gender")
        assertBodyEquals(alias!!, "GENDER1")
    }

    @Test()//собираем инфо-п
    fun testQueryAliases03() {
        var dp = Projection(Department.entity)
                .scalars().withRefs()
                .field(Department.name)
                .with {
                    slice(Department.boss)
                            .scalars().withRefs()
                }
                .field(Department.city)

        var q = queryBuilder.createQueryByDataProjection(dp)

        var alias = q.qr.getAliasByPathFromParent(q.qr.root, "department1", "boss")
        assertBodyEquals(alias!!, "person1")

        alias = q.qr.getAliasByPathFromParent(q.qr.root, "person1", "city")
        assertBodyEquals(alias!!, "city2")


        //часть вторая - использование   алиасов
        dp = Projection(Department.entity).alias("jsu")
                .scalars().withRefs()
                .field(Department.name)
                .with {
                    slice(Department.boss)
                            .scalars().withRefs()
                }
                .field(Department.city)

        q = queryBuilder.createQueryByDataProjection(dp)

        alias = q.qr.getAliasByPathFromParent(q.qr.root, "jsu", "boss")
        assertBodyEquals(alias!!, "person1")

        alias = q.qr.getAliasByPathFromParent(q.qr.root, "person1", "city")
        assertBodyEquals(alias!!, "city2")

        alias = q.qr.getAliasByPathFromParent(q.qr.root, "jsu", "city")
        assertBodyEquals(alias!!, "city1")

    }


    @Test()
    fun testQueryFilter01() {


        var dp: DataProjection = on(Gender)
                .filter({
                    f(GDR.name)
                })


        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "gender1.name")

        //часть вторая
        dp = on(Department)
                .scalars().withRefs()
                .field(Department.name)
                .with {
                    slice(Department.boss)
                            .scalars().withRefs()
                }
                .filter(f(Department.boss().city().id) eq f(Department.city().id))

        q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "city2.id=city1.id")

        //часть вторая
        dp = on(Department)
                .alias("jsu")
                .scalars().withRefs()
                .field(Department.name)
                .with {
                    slice(Department.boss)
                            .scalars().withRefs()
                }
                .field(Department.city)
                .filter({
                    f(Department.boss().city().id) eq f(Department.city().id) and
                            (f(Department.name) eq value("qqq"))
                })

        q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "(city2.id=city1.id and jsu.name=:param0)")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID")}")
            }
        })
    }

    @Test()
    fun testQueryFilter01_FSApi() {

        var dp = Department
                .slice {
                    scalars().withRefs()
                    +name
                    Department.boss {
                        scalars().withRefs()
                    }
                }
                .filter {
                    f(Department.boss().city().id) eq f(Department.city().id)
                }

        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "city2.id=city1.id")

        //часть вторая
        dp = Department.slice {
            alias("jsu")
            scalars().withRefs()
            +name
            boss {
                scalars().withRefs()
            }
            +city
        }.filter {
            f(Department.boss().city().id) eq f(Department.city().id) and
                    (f(Department.name) eq value("qqq"))
        }

        q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "(city2.id=city1.id and jsu.name=:param0)")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID")}")
            }
        })
    }

    @Test()
    //тоже самое но на QOL
    fun testQueryFilter01_oql() {
        var dp: DataProjection = on(Gender)
                .where(
                        "{{name}}"
                )


        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "gender1.name")

        //часть вторая
        dp = on(Department)
                .scalars().withRefs()
                .field(Department.name)
                .with {
                    slice(Department.boss)
                            .scalars().withRefs()
                }
                .where("{{boss.city.id}} = {{city.id}}")

        q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "city2.id=city1.id")


        //часть вторая
        dp = on(Department)
                .alias("jsu")
                .scalars().withRefs()
                .fields(Department.name, Department.city)
                .with {
                    slice("boss")
                            .scalars().withRefs()
                }
                .where("""
                    {{boss.city.id}} = {{city.id}}
                    and {{name}} = :param0
                """).param("param0", "qqq")

        q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "city2.id=city1.id and jsu.name=:param0")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID")}")
            }
        })
    }

    @Test()
    //тоже самое но на QOL
    fun testQueryFilter01_oql_FSAPi() {

        val dp = Department
                .slice {
                    scalars().withRefs()
                    +name
                    boss {
                        scalars().withRefs()
                    }
                }
                .where("{{boss.city.id}} = {{city.id}}")

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "city2.id=city1.id")


    }


    @Test()//собираем инфо-п
    fun testQueryFilterWithAliases01() {

        val dp = on(Department)
                .alias("jsu")
                .scalars().withRefs()
                .fields(Department.name, Department.city)
                .with {
                    slice(Department.boss).alias("www")
                            .scalars().withRefs()
                }
                .filter({
                    {
                        {
                            (f("www.city.id") eq f("city.id"))
                        } or
                                { f("www.name") eq value("nanyr") }
                    } and
                            {
                                f("www.email") eq value("gazman@google.com") or
                                        { f("name") eq value("zzz") }
                            }
                })

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "((city2.ID = city1.ID OR www.NAME = :param0) " +
                "AND (www.EMAIL = :param1 OR jsu.NAME = :param2))")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID")}")
            }
        })
    }


    @Test()//собираем инфо-п
    fun testQueryFilterWithAliases01_oql() {

        val dp = on(Department)
                .alias("jsu")
                .scalars().withRefs()
                .fields(Department.name, Department.city)
                .with {
                    slice("boss").alias("www")
                            .scalars().withRefs()
                }
                .where("""
                    (
                         {{www.city.id}} = {{city.id}}
                        or
                         {{www.name}} = :param0
                    ) and
                    (
                        {{www.email}} = :param1 or
                        {{name}} = :param2
                    )
                """)
                .param("param0", "nanyr")
                .param("param1", "gazman@google.com")
                .param("param2", "zzz")


        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "(city2.ID = city1.ID OR www.NAME = :param0) " +
                "AND (www.EMAIL = :param1 OR jsu.NAME = :param2)")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID")}")
            }
        })
    }


    @Test()//собираем инфо-п
    fun testFilterCompareOperations() {

        //GT
        var dp = Projection("Task")
                .filter(f("id") gt value(1000))

        var q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.ID > :param0")

        //GE
        dp = Projection("Task")
                .filter(f("id") ge value(1000))

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.ID >= :param0")

        //LT
        dp = Projection("Task")
                .filter(f("id") lt value(1000))

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.ID < :param0")

        //LE
        dp = Projection("Task")
                .filter(f("id") le value(1000))

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.ID <= :param0")


        //EQ
        dp = Projection("Task")
                .filter(f("id") eq value(1000))

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.ID = :param0")

        //LIKE
        dp = Projection("Task")
                .filter(f("id") like value("%1000%"))

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.ID like :param0")


        //ILIKE
        dp = Projection("Task")
                .filter(f("id") ilike value("%1000%"))

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "UPPER(TASK1.ID) like UPPER(:param0)")

    }

    @Test()//собираем инфо-п
    fun testFilterCompareOperations_oql() {

        //GT
        var dp = Projection("Task")
                .where("{{id}} > :param0")

        var q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.ID > :param0")

        //GE
        dp = Projection("Task")
                .where("{{id}} >= :param0")

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.ID >= :param0")

        //LT
        dp = Projection("Task")
                .where("{{id}} < :param0")
        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.ID < :param0")

        //LE
        dp = Projection("Task")
                .where("{{id}} <= :param0")
        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.ID <= :param0")


        //EQ
        dp = Projection("Task")
                .where("{{id}} = :param0")
        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.ID = :param0")

        //LIKE
        dp = Projection("Task")
                .where("{{id}} like :param0")
        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.ID like :param0")

    }

    @Test()//собираем инфо-п
    fun testFilterIsNullOperations() {

        //IS NILL
        var dp = Projection("Task")
                .filter(
                        f("name") IS NULL
                )

        var q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.NAME is null")


        //IS NOT NULL
        dp = Projection("Task")
                .filter(
                        f("name") ISNOT NULL
                )

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.NAME is not null")
    }

    @Test
    fun testFilterIsNullOperations_oql() {

        //IS NILL
        var dp: DataProjection = on(Task)
                .where("{{name}} is null")

        var q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.NAME is null")


        //IS NOT NULL
        dp = on("Task")
                .where("{{name}} is not null")

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "TASK1.NAME is not null")
    }


    @Test()//собираем инфо-п
    fun testQueryFilterWithNotOperation() {

        val dp = on(Department)
                .alias("jsu")
                .scalars().withRefs()
                .fields(Department.name, Department.city)
                .with {
                    slice("boss").alias("www")
                            .scalars().withRefs()
                }
                .filter({
                    {
                        {
                            (f("www.city.id") eq f("city.id"))
                        } or
                                not { f("www.name") eq value("nanyr") }
                    } and
                            not({
                                f("www.email") eq value("gazman@google.com") or
                                        { f("name") eq value("zzz") }
                            })
                })

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "((city2.ID = city1.ID OR NOT (www.NAME = :param0)) " +
                "AND NOT ((www.EMAIL = :param1 OR jsu.NAME = :param2)))")


        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }


    @Test
    fun testQueryFilterWithNotOperation_oql() {

        val dp = on(Department)
                .alias("jsu")
                .scalars().withRefs()
                .field(Department.name)
                .with {
                    slice(Department.boss).alias("www")
                            .scalars().withRefs()
                }
                .field("city")
                .where("""
                    (
                         {{www.city.id}} = {{city.id}}
                        or
                         not ({{www.name}} = :param0)
                    ) and
                    not (
                        {{www.email}} = :param1 or
                        {{name}} = :param2
                    )
                """)
                .param("param0", "nanyr")
                .param("param1", "gazman@google.com")
                .param("param2", "zzz")


        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "(city2.ID = city1.ID OR NOT (www.NAME = :param0)) " +
                "AND NOT (www.EMAIL = :param1 OR jsu.NAME = :param2)")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }

    @Test
    fun testQueryFilterWithIntOperation() {

        val dp = on(Task)
                .filter({
                    Task.name IN listOf("xxx", "bbbb")
                })

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "task1.name in (:param0)")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID")}")
            }
        })
    }

    @Test
    fun testQueryFilterWithParam() {

        val dp = on(Task)
                .filter({
                    { Task.name eq param("param1") } and
                            { Task.name like param("param1") }
                }).param("param1", "SAUMI-001")

        val t = dataService.find_(dp)

    }

    @Test
    fun testQueryFilterWithIntOperation_oql() {

        val dp = on(Task)
                .where("{{name}} in (:param0)")
                .param("param0", listOf("qqq"))

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "task1.name in (:param0)")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID")}")
            }
        })
    }


    @Test
    fun testQueryFilterWithId() {

        val dp = on(Task)
                .id(1L)
                .filter({
                    f(Task.name) IN listOf("SAUMI-001", "SAUMI-002")
                })

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }


    @Test
    fun testQueryFilterLimitOffset() {

        val dp = on(Task)
                .limit(1)
                .offset(1)

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }

    @Test
    //тест на то что, в запрос автоматом будет добавлена сортировка по ID
    //если в запросе присутствует оффсет и отсутстует любая сортировка
    fun testQueryWithOffsetShouldBeOrderedAtLeastById() {

        //1 проверяем что в запрос с оффсетом но без сортировки
        //подставится сортировка по ID
        val dp = Person.slice { +name }.limit(10).offset(1)
        assertTrue(dp._orders.isEmpty())
        queryBuilder.createQueryByDataProjection(dp)

        assertTrue(dp._orders.size == 1 && dp._orders[0].name == DataMap.ID)


        //2 проверяем что в запрос с оффсетом и сортировкой
        //не подставится еще и сортировка по ID
        val dp2 = Person.slice { +name }.order(Person.name).limit(10).offset(1)
        queryBuilder.createQueryByDataProjection(dp2)

        assertTrue(dp2._orders.size == 1 && dp2._orders[0].name.equals(Person.name.fieldName, true))
        queryBuilder.createQueryByDataProjection(dp2)
        assertTrue(dp2._orders.size == 1 && dp2._orders[0].name.equals(Person.name.fieldName, true))

    }


    @Test
    fun testQueryOrderBy() {

        val dp = on(Department)
                .alias("jsu")
                .with {
                    slice(Department.boss).alias("www")
                            .scalars().withRefs()
                }
                .field(Department.city)
                .order(f(Department.name),
                        f("www.name").asc_(),
                        f(Department.city().title).desc())
                .limit(2)
                .offset(1)

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)

        assertBodyEquals(q.qr.orderBy, "jsu.NAME ASC, www.NAME ASC, city2.title DESC")
        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }

    //тестируем следующее: использование обращения   в фильтре,
    // к проперте, которая явно не прописана в слайсах
    @Test
    fun testUseFilterPropertyWithNoSlice() {

        val dp = on(Department)
                .where("""
                         {{city.id}} = {{boss.city.id}}
                """)


        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t DEPARTMENT1.ID  AS  ID1,  DEPARTMENT1.NAME  AS  NAME1\n" +
                "FROM DEPARTMENT  DEPARTMENT1\n" +
                "LEFT JOIN CITY  CITY1 ON DEPARTMENT1.CITY_ID=CITY1.ID \n" +
                "LEFT JOIN PERSON  PERSON1 ON DEPARTMENT1.BOSS_ID=PERSON1.ID \n" +
                "LEFT JOIN CITY  CITY2 ON PERSON1.CITY_ID=CITY2.ID \n" +
                "WHERE \n" +
                "CITY1.ID = CITY2.ID\n")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }

    //тестируем следующее: использование обращения   в фильтре,
    // к проперте, которая явно не прописана в слайсах
    @Test
    fun testUseFilterPropertyWithNoSlice2() {

        val dp = on(Department)
                .filter(f(Department.boss().city().id) eq f(Department.city().id))


        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, """SELECT
                DEPARTMENT1.ID  AS  ID1,  DEPARTMENT1.NAME  AS  NAME1
                FROM DEPARTMENT  DEPARTMENT1
                LEFT JOIN PERSON  PERSON1 ON DEPARTMENT1.BOSS_ID=PERSON1.ID
                LEFT JOIN CITY  CITY1 ON PERSON1.CITY_ID=CITY1.ID
                LEFT JOIN CITY  CITY2 ON DEPARTMENT1.CITY_ID=CITY2.ID
                WHERE
                CITY1.ID = CITY2.ID""")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }

    //тестируем следующее: использование обращения   в фильтре,
    // к проперте, которая явно не прописана в слайсах
    @Test
    fun testUseSortPropertyWithNoSlice() {

        val dp =
                on(Department).order(Department.boss().name)


        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, """SELECT
                DEPARTMENT1.ID  AS  ID1,  DEPARTMENT1.NAME  AS  NAME1
                FROM DEPARTMENT  DEPARTMENT1
                LEFT JOIN PERSON  PERSON1 ON DEPARTMENT1.BOSS_ID=PERSON1.ID
                ORDER BY PERSON1."NAME" ASC""")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, _ ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }


    @Test
    //тест на поиск по M-1 полю по датамапа-значению ссылки
    fun testQueryFilterWithReferencedDatamap() {

        val gender = dataService.find_(on(Gender).filter { Gender.id eq 1 })

        val q = queryBuilder.createQueryByDataProjection(on(Person)
                .filter { Person.gender eq gender })
        println(q.sql)
        assertBodyEquals(q.sql, """SELECT
        	  PERSON1."ID"  AS  ID1,  PERSON1."NAME"  AS  NAME1,  PERSON1."EMAIL"  AS  EMAIL1,
          PERSON1."LAST_NAME"  AS  LAST_NAME1,  PERSON1."AGE"  AS  AGE1
                FROM PERSON PERSON1
            WHERE PERSON1."GENDER_ID" = :param0
        """)
        val w = dataService.findAll(on(Person).filter { Person.gender eq gender })
        println(w)
    }


    @Test
    fun testExistsInQuery() {
        val p = (on("Project")
                .with {
                    slice("tasks") //загружаем коллекцию тасков
                            .with {
                                slice("checklists") //загружаем коллекцию чеков
                                        .scalars()
                            }
                }
                .where("{{name}} = 'QDP' AND " +
                        "EXISTS (SELECT j.id FROM CHECKLIST j WHERE j.TASK_ID = {{Tasks.id}})")
                )

        val projects = dataService.findAll(p)
        println(projects)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("tasks").size == 1)
        Assert.assertTrue(projects[0].nestedl("tasks[0].checklists").size == 2)
    }


    @Test
    fun testJoinFilter() {

        val p = dataService.find_(Game.slice {
            episodes {
                withJoinFilter(f("gameEpisodes.name") eq "HEROES 2")
            }
            filter { id eq "HEROES" }
        })
        assertTrue(p[{ episodes }].size == 1)
    }

    @Test
    fun testBuildWhereByExp() {
        dataService.findAll(
                Game.on().filter {
                    alwaysFalse()
                }
        )
    }
}