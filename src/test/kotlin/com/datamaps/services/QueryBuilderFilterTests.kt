package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.mappings.*
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class QueryBuilderFilterTests : BaseSpringTests() {

    @Test
    fun testQueryAliases01() {

        var dp = DataProjection("JiraGender")
        var q = queryBuilder.createQueryByDataProjection(dp)

        var alias = q.qr.getAliasByPathFromParent(null, "gender")!!
        assertBodyEquals(alias, "JIRA_GENDER1")

        dp = DataProjection("JiraGender").alias("jg")
        q = queryBuilder.createQueryByDataProjection(dp)

        alias = q.qr.getAliasByPathFromParent(null, "gender")!!
        assertBodyEquals(alias, "jg")

    }

    @Test(invocationCount = 1)
    fun testQueryAliases02() {
        var dp = DataProjection("JiraWorker")
                .scalars().withRefs()

        var q = queryBuilder.createQueryByDataProjection(dp)

        var alias = q.qr.getAliasByPathFromParent("jira_worker1", "gender")
        assertBodyEquals(alias!!, "JIRA_GENDER1")
    }

    @Test(invocationCount = 1)//собираем инфо-п
    fun testQueryAliases03() {
        var dp = DataProjection("JiraStaffUnit")
                .scalars().withRefs()
                .field("name")
                .with {
                    slice("worker")
                            .scalars().withRefs()
                }
                .field("gender")

        var q = queryBuilder.createQueryByDataProjection(dp)

        var alias = q.qr.getAliasByPathFromParent("Jira_Staff_Unit1", "worker")
        assertBodyEquals(alias!!, "jira_worker1")

        alias = q.qr.getAliasByPathFromParent("jira_worker1", "gender")
        assertBodyEquals(alias!!, "jira_gender1")


        //часть вторая - использование   алиасов
        dp = DataProjection("JiraStaffUnit").alias("jsu")
                .scalars().withRefs()
                .field("name")
                .with {
                    slice("worker")
                            .scalars().withRefs()
                }
                .field("gender")

        q = queryBuilder.createQueryByDataProjection(dp)

        alias = q.qr.getAliasByPathFromParent("jsu", "worker")
        assertBodyEquals(alias!!, "jira_worker1")

        alias = q.qr.getAliasByPathFromParent("jira_worker1", "gender")
        assertBodyEquals(alias!!, "jira_gender1")

        alias = q.qr.getAliasByPathFromParent("jsu", "gender")
        assertBodyEquals(alias!!, "jira_gender2")

    }


    @Test(invocationCount = 1)
    fun testQueryFilter01() {
        var dp = projection("JiraGender")
                .filter({
                    f("gender")
                })


        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "jira_gender1.gender")

        //часть вторая
        dp = projection("JiraStaffUnit")
                .scalars().withRefs()
                .field("name")
                .with {
                    slice("worker")
                            .scalars().withRefs()
                }
                .filter(f("worker.gender.id") eq f("gender.id"))

        q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "jira_gender1.id=jira_gender2.id")


        //часть вторая
        dp = projection("JiraStaffUnit")
                .alias("jsu")
                .scalars().withRefs()
                .field("name")
                .with {
                    slice("worker")
                            .scalars().withRefs()
                }
                .field("gender")
                .filter({
                    f("worker.gender.id") eq f("gender.id") and
                            (f("name") eq value("qqq"))
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
        var dp = projection("JiraGender")
                .where(
                        "{{gender}}"
                )


        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "jira_gender1.gender")

        //часть вторая
        dp = projection("JiraStaffUnit")
                .scalars().withRefs()
                .field("name")
                .with {
                    slice("worker")
                            .scalars().withRefs()
                }
                .where("{{worker.gender.id}} = {{gender.id}}")

        q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "jira_gender1.id=jira_gender2.id")


        //часть вторая
        dp = projection("JiraStaffUnit")
                .alias("jsu")
                .scalars().withRefs()
                .field("name")
                .with {
                    slice("worker")
                            .scalars().withRefs()
                }
                .field("gender")
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

        val dp = DataProjection("JiraStaffUnit")
                .alias("jsu")
                .scalars().withRefs()
                .field("name")
                .with {
                    slice("worker").alias("www")
                            .scalars().withRefs()
                }
                .field("gender")
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

        var dp = DataProjection("JiraStaffUnit")
                .alias("jsu")
                .scalars().withRefs()
                .field("name")
                .with {
                    slice("worker").alias("www")
                            .scalars().withRefs()
                }
                .field("gender")
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
        var dp = DataProjection("JiraTask")
                .where("{{name}} is null")

        var q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.NAME is null")


        //IS NOT NULL
        dp = DataProjection("JiraTask")
                .where("{{name}} is not null")

        q = queryBuilder.createQueryByDataProjection(dp)
        assertBodyEquals(q.qr.where, "JIRA_TASK1.NAME is not null")
    }


    @Test(invocationCount = 1)//собираем инфо-п
    fun testQueryFilterWithNotOperation() {

        var dp = DataProjection("JiraStaffUnit")
                .alias("jsu")
                .scalars().withRefs()
                .field("name")
                .with {
                    slice("worker").alias("www")
                            .scalars().withRefs()
                }
                .field("gender")
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

        var dp = projection("JiraStaffUnit")
                .alias("jsu")
                .scalars().withRefs()
                .field("name")
                .with {
                    slice("worker").alias("www")
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

        var dp = DataProjection("JiraTask")
                .filter({
                    f("name") IN listOf("xxx", "bbbb")
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

        var dp = DataProjection("JiraTask")
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

        var dp = DataProjection("JiraTask")
                .id(1L)
                .filter({
                    f("name") IN listOf("SAUMI-001", "SAUMI-002")
                })

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
    fun testQueryFilterLimitOffset() {

        var dp = DataProjection("JiraTask")
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

        var dp = on("JiraStaffUnit")
                .alias("jsu")
                .with {
                    slice("worker").alias("www")
                            .scalars().withRefs()
                }
                .field("gender")
                .order(f("name"),
                        f("www.name").asc(),
                        f("gender.gender").desc())
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


}




