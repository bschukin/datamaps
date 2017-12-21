package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.maps.f
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

    @Test
            //сделаем апгрейд на третем уровне:
            //JiraProject(1)-->Tasks(2)-->Checklist(3) - загрузим чеклисты
    fun testUpgradesOnSecondLevel() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                on("JiraProject")
                        .scalars().withCollections()
                        .where("{{name}} = 'QDP'")
                        .order(f("jiraTasks.id"))
        )

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("jiraTasks").size == 2)
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists") == null)
        //2 догружаем 1ю коллекцию
        //todo: запрос здесь генерится не оптимальный:есть лишний джойн на  jiraTask хотя он не нужен
        dataService.upgrade(projects, projection()
                .with {
                    slice("jiraTasks") //загружаем коллекуию тасков
                            .with {
                                slice("jiraChecklists") //загружаем коллекуию чеков
                                        .scalars()
                            }
                }
        )

        println(projects)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].nestedl("jiraTasks[0].jiraChecklists").size == 2)
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[1].name") == "bar check")

    }


    //сделаем апгрейд на третем уровне:
    //JiraProject(1)-->Tasks(2)-->Checklist(3) - загрузим чеклисты
    //тест такой же, но на самом деле мы апгрейдим таски
    @Test
    fun testUpgradesOnSecondLevel2() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                on("JiraProject")
                        .scalars().withCollections()
                        .where("{{name}} = 'QDP'")
                        .order(f("jiraTasks.id"))
        )

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("jiraTasks").size == 2)
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists") == null)

        //2 догружаем 1ю коллекцию
        dataService.upgrade(projects.flatMap { p -> p.list("jiraTasks") }, projection()
                .with {
                    slice("jiraChecklists") //загружаем коллекуию чеков
                            .scalars()
                }
        )

        println(projects)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].nestedl("jiraTasks[0].jiraChecklists").size == 2)
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[1].name") == "bar check")

    }


    //тест на использолвание отдельного запроса на вытягивание коллекции
    @Test
    fun testSelectCollectionWithSeparateSelect() {

        sqlStatistics.start()
        //1 грузим  проекты без коллекций
        val projects = dataService
                .findAll(on("JiraProject")
                        .scalars()
                        .with {
                            slice("jiraTasks") //загружаем коллекуию тасков
                                    .scalars().asSelect()
                                    .with {
                                        slice("jiraChecklists") //загружаем коллекуию чеклистов
                                                .scalars().asSelect()
                                    }
                        }
                        .where("{{name}} = 'QDP'")
                )

        println(projects)

        Assert.assertTrue(sqlStatistics.queries().size==3)

        assertBodyEquals(sqlStatistics.queries()[0].sql,
                "SELECT \n" +
                        "\t jira_project1.\"id\"  AS  id1,  jira_project1.\"name\"  AS  name1\n" +
                        "FROM jira_project as jira_project1 WHERE jira_project1.\"name\" = 'QDP' ")

        assertBodyEquals(sqlStatistics.queries()[1].sql,
                "SELECT \n" +
                        "\t jira_task1.\"id\"  AS  id1,  jira_task1.\"name\"  AS  name1,  (jira_project_id) AS  _backRefId\n" +
                        "FROM jira_task as jira_task1 \n" +
                        "WHERE jira_task1.\"jira_project_id\" in (:param0)   ")

        assertBodyEquals(sqlStatistics.queries()[2].sql,
                "SELECT \n" +
                        "\t jira_checklist1.\"id\"  AS  id1,  jira_checklist1.\"name\"  AS  name1,  (jira_task_id) AS  _backRefId\n" +
                        "FROM jira_checklist as jira_checklist1 \n" +
                        "WHERE jira_checklist1.\"jira_task_id\" in (:param0)   ")

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].nestedl("jiraTasks[0].jiraChecklists").size == 2)
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[1].name") == "bar check")

        sqlStatistics.stop()
    }


    //тест на использолвание отдельного запроса на вытягивание коллекции
    //Здесь смотрится, что проекция после asSelect() - будет применена.
    //в конкретном случае - просто говорим что вместе с селектом коллекции тасков
    //загружайте сразу и чеклисты
    @Test
    fun testSelectCollectionWithSeparateSelect2() {

        sqlStatistics.start()
        //1 грузим  проекты без коллекций
        val projects = dataService
                .findAll(on("JiraProject")
                        .scalars()
                        .with {
                            slice("jiraTasks") //загружаем коллекуию тасков
                                    .scalars().asSelect()
                                    .with {
                                        slice("jiraChecklists") //загружаем коллекуию чеклистов
                                                .scalars()
                                    }
                        }
                        .where("{{name}} = 'QDP'")
                )

        println(projects)

        Assert.assertTrue(sqlStatistics.queries().size==2)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].nestedl("jiraTasks[0].jiraChecklists").size == 2)
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[1].name") == "bar check")

        sqlStatistics.stop()
    }

}