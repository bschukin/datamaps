package com.datamaps.services

import com.datamaps.*
import com.datamaps.maps.*
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class QueryBuilderFilterTests : BaseSpringTests() {

    @Test
    fun testQueryAliases01() {

        var dp = DataProjection(Gender.entity)
        var q = queryBuilder.createQueryByDataProjection(dp)

        var alias = q.qr.getAliasByPathFromParent(q.qr.root, null, Gender.gender.n)!!
        assertBodyEquals(alias, "JIRA_GENDER1")

        dp = DataProjection(Gender.entity).alias("jg")
        q = queryBuilder.createQueryByDataProjection(dp)

        alias = q.qr.getAliasByPathFromParent(q.qr.root, null, Gender.gender.n)!!
        assertBodyEquals(alias, "jg")

    }

    @Test(invocationCount = 1)
    fun testQueryAliases02() {
        var dp = DataProjection(WRKR.entity)
                .scalars().withRefs()

        var q = queryBuilder.createQueryByDataProjection(dp)

        var alias = q.qr.getAliasByPathFromParent(q.qr.root, "jira_worker1", "gender")
        assertBodyEquals(alias!!, "JIRA_GENDER1")
    }

    @Test(invocationCount = 1)//собираем инфо-п
    fun testQueryAliases03() {
        var dp = DataProjection(StaffUnit.entity)
                .scalars().withRefs()
                .field(SU.name)
                .with {
                    slice(SU.worker)
                            .scalars().withRefs()
                }
                .field(SU.gender)

        var q = queryBuilder.createQueryByDataProjection(dp)

        var alias = q.qr.getAliasByPathFromParent(q.qr.root, "Jira_Staff_Unit1", "worker")
        assertBodyEquals(alias!!, "jira_worker1")

        alias = q.qr.getAliasByPathFromParent(q.qr.root, "jira_worker1", "gender")
        assertBodyEquals(alias!!, "jira_gender1")


        //часть вторая - использование   алиасов
        dp = DataProjection(StaffUnit.entity).alias("jsu")
                .scalars().withRefs()
                .field(SU.name)
                .with {
                    slice(SU.worker)
                            .scalars().withRefs()
                }
                .field(SU.gender)

        q = queryBuilder.createQueryByDataProjection(dp)

        alias = q.qr.getAliasByPathFromParent(q.qr.root, "jsu", "worker")
        assertBodyEquals(alias!!, "jira_worker1")

        alias = q.qr.getAliasByPathFromParent(q.qr.root, "jira_worker1", "gender")
        assertBodyEquals(alias!!, "jira_gender1")

        alias = q.qr.getAliasByPathFromParent(q.qr.root, "jsu", "gender")
        assertBodyEquals(alias!!, "jira_gender2")

    }


    @Test(invocationCount = 1)
    fun testQueryFilter01() {


        var dp = on(Gender)
                .filter({
                    f(GDR.gender)
                })


        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "jira_gender1.gender")

        //часть вторая
        dp = on(StaffUnit)
                .scalars().withRefs()
                .field(SU.name)
                .with {
                    slice(SU.worker)
                            .scalars().withRefs()
                }
                .filter(f(SU.worker().gender().id) eq f(SU.gender().id))

        q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "jira_gender1.id=jira_gender2.id")

        //часть вторая
        dp = on(StaffUnit)
                .alias("jsu")
                .scalars().withRefs()
                .field(SU.name)
                .with {
                    slice(SU.worker)
                            .scalars().withRefs()
                }
                .field(SU.gender)
                .filter({
                    f(SU.worker().gender().id) eq f(SU.gender().id) and
                            (f(SU.name) eq value("qqq"))
                })

        q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "(jira_gender1.id=jira_gender2.id and jsu.name=:param0)")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID")}")
            }
        })
    }

    @Test(invocationCount = 1)
            //тоже самое но на QOL
    fun testQueryFilter01_oql() {
        var dp = on(Gender)
                .where(
                        "{{gender}}"
                )


        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "jira_gender1.gender")

        //часть вторая
        dp = on(StaffUnit)
                .scalars().withRefs()
                .field(SU.name)
                .with {
                    slice(SU.worker)
                            .scalars().withRefs()
                }
                .where("{{worker.gender.id}} = {{gender.id}}")

        q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "jira_gender1.id=jira_gender2.id")


        //часть вторая
        dp = on(StaffUnit)
                .alias("jsu")
                .scalars().withRefs()
                .fields(SU.name, SU.gender)
                .with {
                    slice("worker")
                            .scalars().withRefs()
                }
                .where("""
                    {{worker.gender.id}} = {{gender.id}}
                    and {{name}} = :param0
                """).param("param0", "qqq")

        q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "jira_gender1.id=jira_gender2.id and jsu.name=:param0")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID")}")
            }
        })
    }


    @Test(invocationCount = 1)//собираем инфо-п
    fun testQueryFilterWithAliases01() {

        val dp = on(StaffUnit)
                .alias("jsu")
                .scalars().withRefs()
                .fields(SU.name, SU.gender)
                .with {
                    slice(SU.worker).alias("www")
                            .scalars().withRefs()
                }
                .filter({
                    {
                        {
                            (f("www.gender.id") eq f("gender.id"))
                        } or
                                { f("www.name") eq value("nanyr") }
                    } and
                            {
                                f("www.email") eq value("gazman@google.com") or
                                        { f("name") eq value("zzz") }
                            }
                })

        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "((JIRA_GENDER1.ID = JIRA_GENDER2.ID OR www.NAME = :param0) " +
                "AND (www.EMAIL = :param1 OR jsu.NAME = :param2))")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID")}")
            }
        })
    }


    @Test(invocationCount = 1)//собираем инфо-п
    fun testQueryFilterWithAliases01_oql() {

        var dp = on(StaffUnit)
                .alias("jsu")
                .scalars().withRefs()
                .fields(SU.name, SU.gender)
                .with {
                    slice("worker").alias("www")
                            .scalars().withRefs()
                }
                .where("""
                    (
                         {{www.gender.id}} = {{gender.id}}
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


        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "(JIRA_GENDER1.ID = JIRA_GENDER2.ID OR www.NAME = :param0) " +
                "AND (www.EMAIL = :param1 OR jsu.NAME = :param2)")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID")}")
            }
        })
    }


    @Test(invocationCount = 1)//собираем инфо-п
    fun testFilterCompareOperations() {

        //GT
        var dp = DataProjection("JiraTask")
                .filter(f("id") gt value(1000))

        var q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.ID > :param0")

        //GE
        dp = DataProjection("JiraTask")
                .filter(f("id") ge value(1000))

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.ID >= :param0")

        //LT
        dp = DataProjection("JiraTask")
                .filter(f("id") lt value(1000))

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.ID < :param0")

        //LE
        dp = DataProjection("JiraTask")
                .filter(f("id") le value(1000))

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.ID <= :param0")


        //EQ
        dp = DataProjection("JiraTask")
                .filter(f("id") eq value(1000))

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.ID = :param0")

        //LIKE
        dp = DataProjection("JiraTask")
                .filter(f("id") like value("%1000%"))

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.ID like :param0")

    }

    @Test(invocationCount = 1)//собираем инфо-п
    fun testFilterCompareOperations_oql() {

        //GT
        var dp = DataProjection("JiraTask")
                .where("{{id}} > :param0")

        var q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.ID > :param0")

        //GE
        dp = DataProjection("JiraTask")
                .where("{{id}} >= :param0")

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.ID >= :param0")

        //LT
        dp = DataProjection("JiraTask")
                .where("{{id}} < :param0")
        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.ID < :param0")

        //LE
        dp = DataProjection("JiraTask")
                .where("{{id}} <= :param0")
        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.ID <= :param0")


        //EQ
        dp = DataProjection("JiraTask")
                .where("{{id}} = :param0")
        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.ID = :param0")

        //LIKE
        dp = DataProjection("JiraTask")
                .where("{{id}} like :param0")
        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.ID like :param0")

    }

    @Test(invocationCount = 1)//собираем инфо-п
    fun testFilterIsNullOperations() {

        //IS NILL
        var dp = DataProjection("JiraTask")
                .filter(
                        f("name") IS NULL()
                )

        var q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.NAME is null")


        //IS NOT NULL
        dp = DataProjection("JiraTask")
                .filter(
                        f("name") ISNOT NULL()
                )

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.NAME is not null")
    }

    @Test
    fun testFilterIsNullOperations_oql() {

        //IS NILL
        var dp = on(Task)
                .where("{{name}} is null")

        var q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.NAME is null")


        //IS NOT NULL
        dp = on("JiraTask")
                .where("{{name}} is not null")

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.NAME is not null")
    }


    @Test(invocationCount = 1)//собираем инфо-п
    fun testQueryFilterWithNotOperation() {

        var dp = on(StaffUnit)
                .alias("jsu")
                .scalars().withRefs()
                .fields(SU.name, SU.gender)
                .with {
                    slice("worker").alias("www")
                            .scalars().withRefs()
                }
                .filter({
                    {
                        {
                            (f("www.gender.id") eq f("gender.id"))
                        } or
                                not { f("www.name") eq value("nanyr") }
                    } and
                            not({
                                f("www.email") eq value("gazman@google.com") or
                                        { f("name") eq value("zzz") }
                            })
                })

        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "((JIRA_GENDER1.ID = JIRA_GENDER2.ID OR NOT (www.NAME = :param0)) " +
                "AND NOT ((www.EMAIL = :param1 OR jsu.NAME = :param2)))")


        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }


    @Test
    fun testQueryFilterWithNotOperation_oql() {

        var dp = on(StaffUnit)
                .alias("jsu")
                .scalars().withRefs()
                .field(SU.name)
                .with {
                    slice(SU.worker).alias("www")
                            .scalars().withRefs()
                }
                .field("gender")
                .where("""
                    (
                         {{www.gender.id}} = {{gender.id}}
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


        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "(JIRA_GENDER1.ID = JIRA_GENDER2.ID OR NOT (www.NAME = :param0)) " +
                "AND NOT (www.EMAIL = :param1 OR jsu.NAME = :param2)")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }

    @Test
    fun testQueryFilterWithIntOperation() {

        var dp = on(Task)
                .filter({
                    f(Task.name) IN listOf("xxx", "bbbb")
                })

        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "Jira_task1.name in (:param0)")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID")}")
            }
        })
    }

    @Test
    fun testQueryFilterWithIntOperation_oql() {

        var dp = on(Task)
                .where("{{name}} in (:param0)")
                .param("param0", listOf("qqq"))

        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "Jira_task1.name in (:param0)")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
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
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }


    @Test
    fun testQueryFilterLimitOffset() {

        var dp = on(Task)
                .limit(1)
                .offset(1)

        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }

    @Test
    fun testQueryOrderBy() {

        var dp = on(StaffUnit)
                .alias("jsu")
                .with {
                    slice(SU.worker).alias("www")
                            .scalars().withRefs()
                }
                .field(SU.gender)
                .order(f(SU.name),
                        f("www.name").asc(),
                        f(SU.gender().gender).desc())
                .limit(2)
                .offset(1)

        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)

        assertBodyEquals(q.qr.orderBy, "jsu.NAME ASC, www.NAME ASC, JIRA_GENDER2.GENDER DESC")
        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }

    //тестируем следующее: использование обращения   в фильтре,
    // к проперте, которая явно не прописана в слайсах
    @Test
    fun testUseFilterPropertyWithNoSlice() {

        var dp = on(StaffUnit)
                .where("""
                         {{gender.id}} = {{worker.gender.id}}
                """)


        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t  JIRA_STAFF_UNIT1.\"ID\"  AS  ID1,  JIRA_STAFF_UNIT1.\"NAME\"  AS  NAME1\n" +
                "FROM JIRA_STAFF_UNIT as JIRA_STAFF_UNIT1\n" +
                "LEFT JOIN JIRA_GENDER as JIRA_GENDER1 ON JIRA_STAFF_UNIT1.\"GENDER_ID\"=JIRA_GENDER1.\"ID\" \n" +
                "LEFT JOIN JIRA_WORKER as JIRA_WORKER1 ON JIRA_STAFF_UNIT1.\"WORKER_ID\"=JIRA_WORKER1.\"ID\" \n" +
                "LEFT JOIN JIRA_GENDER as JIRA_GENDER2 ON JIRA_WORKER1.\"GENDER_ID\"=JIRA_GENDER2.\"ID\" \n" +
                "WHERE \n" +
                "                         JIRA_GENDER1.\"ID\" = JIRA_GENDER2.\"ID\"")

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }

    //тестируем следующее: использование обращения   в фильтре,
    // к проперте, которая явно не прописана в слайсах
    @Test
    fun testUseFilterPropertyWithNoSlice2() {

        var dp = on(StaffUnit)
                .filter(-StaffUnit.worker().gender().id eq -StaffUnit.gender().id)


        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, """
            SELECT
	                JIRA_STAFF_UNIT1."ID"  AS  ID1,  JIRA_STAFF_UNIT1."NAME"  AS  NAME1
                    FROM JIRA_STAFF_UNIT as JIRA_STAFF_UNIT1
                    LEFT JOIN JIRA_WORKER as JIRA_WORKER1 ON JIRA_STAFF_UNIT1."WORKER_ID"=JIRA_WORKER1."ID"
                    LEFT JOIN JIRA_GENDER as JIRA_GENDER1 ON JIRA_WORKER1."GENDER_ID"=JIRA_GENDER1."ID"
                    LEFT JOIN JIRA_GENDER as JIRA_GENDER2 ON JIRA_STAFF_UNIT1."GENDER_ID"=JIRA_GENDER2."ID"
                    WHERE JIRA_GENDER1."ID" = JIRA_GENDER2."ID"
            """)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
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
                on(StaffUnit).order(-StaffUnit.worker().name)


        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, """SELECT
            	  JIRA_STAFF_UNIT1."ID"  AS  ID1,  JIRA_STAFF_UNIT1."NAME"  AS  NAME1
                    FROM JIRA_STAFF_UNIT as JIRA_STAFF_UNIT1
                    LEFT JOIN JIRA_WORKER as JIRA_WORKER1 ON JIRA_STAFF_UNIT1."WORKER_ID"=JIRA_WORKER1."ID"
                ORDER BY JIRA_WORKER1."NAME" ASC
        """)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        namedParameterJdbcTemplate.query(q.sql, q.qr.params, { resultSet, i ->
            run {
                println("${resultSet.getInt("ID1")}")
            }
        })
    }
}