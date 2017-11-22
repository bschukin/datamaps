package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.mappings.*
import org.springframework.beans.factory.annotation.Autowired
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class QueryBuilderFilterTests : BaseSpringTests() {

    @Autowired
    lateinit var queryBuilder: QueryBuilder

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
                .default().refs()

        var q = queryBuilder.createQueryByDataProjection(dp)

        var alias = q.qr.getAliasByPathFromParent("jira_worker1", "gender")
        assertBodyEquals(alias!!, "JIRA_GENDER1")
    }

    @Test(invocationCount = 1)//собираем инфо-п
    fun testQueryAliases03() {
        var dp = DataProjection("JiraStaffUnit")
                .default().refs()
                .field("name")
                .field("worker")
                /*  */.inner()
                /*      */.default().refs()
                /*  */.end()
                .field("gender")

        var q = queryBuilder.createQueryByDataProjection(dp)

        var alias = q.qr.getAliasByPathFromParent("Jira_Staff_Unit1", "worker")
        assertBodyEquals(alias!!, "jira_worker1")

        alias = q.qr.getAliasByPathFromParent("jira_worker1", "gender")
        assertBodyEquals(alias!!, "jira_gender1")


        //часть вторая - использование   алиасов
        dp = DataProjection("JiraStaffUnit").alias("jsu")
                .default().refs()
                .field("name")
                .field("worker")
                /*  */.inner()
                /*      */.default().refs()
                /*  */.end()
                .field("gender")

        q = queryBuilder.createQueryByDataProjection(dp)

        alias = q.qr.getAliasByPathFromParent("jsu", "worker")
        assertBodyEquals(alias!!, "jira_worker1")

        alias = q.qr.getAliasByPathFromParent("jira_worker1", "gender")
        assertBodyEquals(alias!!, "jira_gender1")

        alias = q.qr.getAliasByPathFromParent("jsu", "gender")
        assertBodyEquals(alias!!, "jira_gender2")

    }


    @Test(invocationCount = 1)//собираем инфо-п
    fun testQueryFilter01() {
        var dp = DataProjection("JiraGender")
                .filter({
                    f("gender")
                })


        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "jira_gender1.gender")

        //часть вторая
        dp = DataProjection("JiraStaffUnit")
                .default().refs()
                .field("name")
                .field("worker")
                /*  */.inner()
                /*      */.default().refs()
                /*  */.end()
                .field("gender")
                .filter(f("worker.gender.id") eq f("gender.id"))

        q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "jira_gender1.id=jira_gender2.id")


        //часть вторая
        dp = DataProjection("JiraStaffUnit")
                .alias("jsu")
                .default().refs()
                .field("name")
                .field("worker")
                /*  */.inner()
                /*      */.default().refs()
                /*  */.end()
                .field("gender")
                .filter({
                    f("worker.gender.id") eq f("gender.id") and
                            (f("name") eq value(1000))
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


    @Test(invocationCount = 1)//собираем инфо-п
    fun testQueryFilterWithAliases01() {

        var dp = DataProjection("JiraStaffUnit")
                .alias("jsu")
                .default().refs()
                .field("name")
                .field("worker")
                /*  */.inner().alias("www")
                /*      */.default().refs()
                /*  */.end()
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


    @Test(invocationCount = 1)//собираем инфо-п
    fun testQueryFilterWithNotOperation() {

        var dp = DataProjection("JiraStaffUnit")
                .alias("jsu")
                .default().refs()
                .field("name")
                .field("worker")
                /*  */.inner().alias("www")
                /*      */.default().refs()
                /*  */.end()
                .field("gender")
                .filter({
                    {
                        {
                            (f("www.gender.id") eq f("gender.id"))
                        } or
                                not{ f("www.name") eq value("nanyr") }
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
                println("${resultSet.getInt("ID")}")
            }
        })
    }

}




