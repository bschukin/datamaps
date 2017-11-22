package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.mappings.DataProjection
import com.datamaps.mappings.f
import org.springframework.beans.factory.annotation.Autowired
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class QueryBuilderFilterTests : BaseSpringTests() {

    @Autowired
    lateinit var queryBuilder: QueryBuilder

    @Test
    fun tesQueryAliases01() {

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
    fun tesQueryAliases02() {
        var dp = DataProjection("JiraWorker")
                .default().refs()

        var q = queryBuilder.createQueryByDataProjection(dp)

        var alias = q.qr.getAliasByPathFromParent("jira_worker1", "gender")
        assertBodyEquals(alias!!, "JIRA_GENDER1")
    }

    @Test(invocationCount = 1)//собираем инфо-п
    fun tesQueryAliases03() {
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
    fun tesQueryFilter01() {
        var dp = DataProjection("JiraGender")
                .filter({
                    f("gender")
                })


        var q = queryBuilder.createQueryByDataProjection(dp)
        println(q.qr.where)
        assertBodyEquals(q.qr.where, "gender1")

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
        assertBodyEquals(q.qr.where, "ID3 = ID4")
    }
}