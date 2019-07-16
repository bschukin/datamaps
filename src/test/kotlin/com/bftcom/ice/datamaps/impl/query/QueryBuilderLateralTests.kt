    package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.datamaps.projection
import com.bftcom.ice.server.Project
import com.bftcom.ice.IfSpringProfileActive
import com.bftcom.ice.datamaps.impl.util.printAsJson
import org.junit.Test

    /**
 * Created by Щукин on 03.11.2017.
 */
open class QueryBuilderLateralTests : BaseSpringTests() {

    @Test
    @IfSpringProfileActive("postgresql")
    fun testLateralExample() {

        //почемму то не применяется @Profile("postgresql")
        if (!isProstgress())
            return

        val dp = projection("Project")
                .full()
                .lateral("projtasks", """
                    (select string_agg(t.name, ';') as tasks1, count(*) as qty1
                            from task t
                            where t.project_id= {{id}}
                            ) projtasks on true
                    """,
                        "tasks1" to "projtasks", "qty1" to "qty")
                .where("{{projtasks.tasks1}} like '%001%'")

        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        val e = dataService.findAll(dp)
        e.forEach {el->
            print(el)
        }

        assert(e.size == 1)
        assert(e[0]["projtasks"] == "SAUMI-001;SAUMI-002")
        assert(e[0]["qty"] == 2L)
    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testLateralExampleNoExplicitMappings() {

        if (!isProstgress())
            return

        val dp = Project.on()
                .lateral("projtasks", """
                    (select string_agg(t.name, ';') as tasks1, count(*) as qty1
                            from task t
                            where t.project_id= {{id}}
                            ) projtasks on true
                    """)
                .where("{{projtasks.tasks1}} like '%001%'")

        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        val e = dataService.findAll(dp)
        e.forEach {el->
            print(el)
        }

        assert(e.size == 1)
        assert(e[0]["tasks1"] == "SAUMI-001;SAUMI-002")
        assert(e[0]["qty1"] == 2L)
    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testLateralEmbeddedExample() {

        val dp = Project.on()
                .embeddedLateral("projtasks", "taskInfo",
                        """
                    (select string_agg(t.name, ';') as tasks1, count(*) as qty1
                            from task t
                            where t.project_id= {{id}}
                            ) projtasks on true
                    """,
                        "tasks1" to "projtasks", "qty1" to "qty")
                .where("{{projtasks.tasks1}} like '%001%'")

        val e = dataService.findAll(dp)

        assert(e.size == 1)
        assert(e[0]["taskInfo.projtasks"] == "SAUMI-001;SAUMI-002")
        assert(e[0]["taskInfo.qty"] == 2L)

        e[0].printAsJson()
    }


    @Test
    @IfSpringProfileActive("postgresql")
    fun testLateralEmbeddedExampleNoMappings() {

        val dp = Project.on()
                .embeddedLateral("taskInfo", "taskInfo",
                        """
                    (select string_agg(t.name, ';') as tasks, count(*) as qty
                            from task t
                            where t.project_id= {{id}}
                            ) taskInfo on true
                    """)
                .where("{{taskInfo.tasks}} like '%001%'")

        val e = dataService.findAll(dp)


        assert(e.size == 1)
        assert(e[0]["taskInfo.tasks"] == "SAUMI-001;SAUMI-002")
        assert(e[0]["taskInfo.qty"] == 2L)

    }


}