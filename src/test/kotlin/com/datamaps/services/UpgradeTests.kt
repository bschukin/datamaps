package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.mappings.projection
import com.datamaps.mappings.slice
import org.testng.Assert
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class UpgradeTests : BaseSpringTests() {


    @Test
    fun testUpgradesIdea() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                projection("JiraProject")
                        .scalars())

        println(projects)

        val q = queryBuilder.createUpgradeQueryByMapsAndSlices(projects,
                projection().with {
                    slice("jiraTasks")
                            .full()
                })

        println(q.sql)

        //todo: select надо делать другой
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t  JIRA_PROJECT1.ID  AS  ID1,  JIRA_TASK1.ID  AS  ID2,  JIRA_TASK1.NAME  AS  NAME1,  JIRA_CHECKLIST1.ID  AS  ID3,  JIRA_CHECKLIST1.NAME  AS  NAME2\n" +
                "FROM JIRA_PROJECT as JIRA_PROJECT1\n" +
                "LEFT JOIN JIRA_TASK as JIRA_TASK1 ON JIRA_PROJECT1.ID=JIRA_TASK1.JIRA_PROJECT_ID \n" +
                "LEFT JOIN JIRA_CHECKLIST as JIRA_CHECKLIST1 ON JIRA_TASK1.ID=JIRA_CHECKLIST1.JIRA_TASK_ID \n" +
                "WHERE JIRA_PROJECT1.ID in (:param0)")

        val list = queryExecutor.findAll(q)
        println(list)

    }

    @Test
    fun testUpgrades01() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                projection("JiraProject")
                        .scalars()
                        .where("{{name}} = 'QDP'")
        )

        Assert.assertTrue(projects.size==1)
        Assert.assertTrue(projects[0]["jiraTasks"]==null)

        //2 догружаем коллекции
        dataService.upgrade(projects, projection()
                .with {
                    slice("jiraTasks") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })

        println(projects)

        Assert.assertTrue(projects.size==1)
        Assert.assertTrue(projects[0].list("jiraTasks").size==2)

        Assert.assertTrue(projects[0].nestedl("jiraTasks[0].jiraChecklists").size==2)

        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[0].name")=="foo check")
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[1].name")=="bar check")

        Assert.assertTrue(projects[0].nested("jiraTasks[1].jiraChecklists")==null)


    }


}