package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.maps.on
import com.datamaps.maps.projection
import com.datamaps.maps.slice
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

        //подгурзим только таски  и их чеклисты
        val q = queryBuilder.createUpgradeQueryByMapsAndSlices(projects,
                projection().with {
                    slice("jiraTasks")
                            .scalars().withCollections()
                })

        println(q.sql)

        //todo: select надо делать другой
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t  JIRA_TASK1.\"ID\"  AS  ID1,  JIRA_TASK1.\"NAME\"  AS  NAME1,  JIRA_CHECKLIST1.\"ID\"  AS  ID2,  JIRA_CHECKLIST1.\"NAME\"  AS  NAME2,  (JIRA_PROJECT_ID) AS  _backRefId\n" +
                "FROM JIRA_TASK as JIRA_TASK1\n" +
                "LEFT JOIN JIRA_CHECKLIST as JIRA_CHECKLIST1 ON JIRA_TASK1.\"ID\"=JIRA_CHECKLIST1.\"JIRA_TASK_ID\" \n" +
                "WHERE JIRA_TASK1.\"JIRA_PROJECT_ID\" in (:param0)")

        val list = queryExecutor.findAll(q)
        println(list)

    }

    @Test
    fun testUpgrades01() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                on("JiraProject")
                        .scalars()
                        .where("{{name}} = 'QDP'")
        )

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0]["jiraTasks"] == null)

        //2 догружаем коллекции
        dataService.upgrade(projects, projection()
                .with {
                    slice("jiraTasks") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })

        println(projects)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("jiraTasks").size == 2)

        Assert.assertTrue(projects[0].nestedl("jiraTasks[0].jiraChecklists").size == 2)

        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[1].name") == "bar check")

        Assert.assertTrue(projects[0].nested("jiraTasks[1].jiraChecklists") == null)


    }

    @Test
    fun testUpgradesWith2Collections() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                on("JiraProject")
                        .scalars()
                        .where("{{name}} = 'QDP'")
        )

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0]["jiraTasks"] == null)
        Assert.assertTrue(projects[0]["jiraProjectWorkers"] == null)

        //2 догружаем 2 коллекции
        dataService.upgrade(projects, projection()
                .with {
                    slice("jiraTasks") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                }
                .with {
                    slice("jiraProjectWorkers") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })

        println(projects)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("jiraTasks").size == 2)
        Assert.assertTrue(projects[0].list("jiraProjectWorkers").size == 2)

        Assert.assertTrue(projects[0].nestedl("jiraTasks[0].jiraChecklists").size == 2)

        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[1].name") == "bar check")

        Assert.assertTrue(projects[0].nested("jiraTasks[1].jiraChecklists") == null)

    }

    @Test
    //тоже самое что и в предыдущем тесте, но последовательно
    //последовательно апгрейдим коллекции
    fun testUpgradesWith2CollectionsSerial() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                on("JiraProject")
                        .scalars()
                        .where("{{name}} = 'QDP'")
        )

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0]["jiraTasks"] == null)
        Assert.assertTrue(projects[0]["jiraProjectWorkers"] == null)

        //2 догружаем 1ю коллекцию
        dataService.upgrade(projects, projection()
                .with {
                    slice("jiraTasks") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })

        println(projects)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("jiraTasks").size == 2)
        Assert.assertTrue(projects[0].list("jiraProjectWorkers").size == 0)

        Assert.assertTrue(projects[0].nestedl("jiraTasks[0].jiraChecklists").size == 2)

        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[1].name") == "bar check")

        //Assert.assertTrue(projects[0].nested("jiraTasks[1].jiraChecklists") == null)

        //3 догружаем 2ю коллекцию
        dataService.upgrade(projects, projection()
                .with {
                    slice("jiraProjectWorkers") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })

        println(projects)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("jiraTasks").size == 2)
        Assert.assertTrue(projects[0].list("jiraProjectWorkers").size == 2)

        Assert.assertTrue(projects[0].nestedl("jiraTasks[0].jiraChecklists").size == 2)
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[1].name") == "bar check")

        Assert.assertTrue(projects[0].nested("jiraTasks[1].jiraChecklists") == null)

    }


}