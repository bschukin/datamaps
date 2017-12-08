package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.mappings.projection
import org.springframework.context.annotation.Profile
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
@Profile("postgresql")
//@SpringBootTest
class QueryBuilderLateralTests : BaseSpringTests() {



    @Test
    fun testLateralExample() {

        //почемму то не применяется @Profile("postgresql")
        if (!isProstgress())
            return

        var dp = projection("JiraProject")
                .full()
                .lateral("tasks", """
                    (select string_agg(t.n, ';') as tasks1, count(*) as qty1
                            from jira_task t
                            where t.jira_project_id= {{id}}
                            ) tasks on true
                    """,
                        "tasks1" to "tasks", "qty1" to "qty")
                .where("{{tasks.tasks1}} like '%001%'")

        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        val e = dataService.findAll(dp)
        e.forEach {el->
            print(el)
        }

        assert(e.size==1)
        assert(e[0]["tasks"]=="SAUMI-001;SAUMI-002")
        assert(e[0]["qty"]==2L)
    }


}