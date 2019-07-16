package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.assertBodyEquals
import org.junit.Assert
import org.junit.Test

/**
 * Created by Щукин on 03.11.2017.
 */
open  class UpgradeTests : BaseSpringTests() {


    @Test
    fun testUpgradesIdea() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                projection("Project")
                        .scalars())

        println(projects)

        //подгурзим только таски  и их чеклисты
        val q = queryBuilder.createUpgradeQueryByMapsAndSlices(projects,
                projection().with {
                    slice("Tasks")
                            .scalars().withCollections()
                })

        println(q.sql)

        assertBodyEquals(q.sql, "SELECT \n" +
                "\t  TASK1.\"ID\"  AS  ID1,  TASK1.\"NAME\"  AS  NAME1,  CHECKLIST1.\"ID\"  AS  ID2,  CHECKLIST1.\"NAME\"  AS  NAME2,   (task1.\"project_id\") AS  backRefId__\n" +
                "FROM TASK  TASK1\n" +
                "LEFT JOIN CHECKLIST  CHECKLIST1 ON TASK1.\"ID\"=CHECKLIST1.\"TASK_ID\" \n" +
                "WHERE TASK1.\"PROJECT_ID\" in (:param0)")

        val list = queryExecutor.findAll<UndefinedFieldSet>(q)
        println(list)

    }

    @Test
    fun testUpgrades01() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                on("Project")
                        .scalars()
                        .where("{{name}} = 'QDP'")
        )

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0]["tasks"] == null)

        //2 догружаем коллекции
        dataService.upgrade(projects, projection()
                .with {
                    slice("tasks") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })

        println(projects)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("tasks").size == 2)

        Assert.assertTrue(projects[0].nestedl("tasks[0].checklists").size == 2)

        Assert.assertTrue(projects[0].nested("tasks[0].checklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("tasks[0].checklists[1].name") == "bar check")

        Assert.assertTrue(projects[0].nested("tasks[1].checklists") == null)


    }

    @Test
    fun testUpgradesWith2Collections() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                on("Project")
                        .scalars()
                        .where("{{name}} = 'QDP'")
        )

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0]["tasks"] == null)
        Assert.assertTrue(projects[0]["projectWorkers"] == null)

        //2 догружаем 2 коллекции
        dataService.upgrade(projects, projection()
                .with {
                    slice("tasks") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                }
                .with {
                    slice("projectWorkers") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })

        println(projects)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("tasks").size == 2)
        Assert.assertTrue(projects[0].list("projectWorkers").size == 2)

        Assert.assertTrue(projects[0].nestedl("tasks[0].checklists").size == 2)

        Assert.assertTrue(projects[0].nested("tasks[0].checklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("tasks[0].checklists[1].name") == "bar check")

        Assert.assertTrue(projects[0].nested("tasks[1].checklists") == null)

    }

    @Test
            //тоже самое что и в предыдущем тесте, но последовательно
            //последовательно апгрейдим коллекции
    fun testUpgradesWith2CollectionsSerial() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                on("Project")
                        .scalars()
                        .where("{{name}} = 'QDP'")
        )

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0]["tasks"] == null)
        Assert.assertTrue(projects[0]["projectWorkers"] == null)

        //2 догружаем 1ю коллекцию
        dataService.upgrade(projects, projection()
                .with {
                    slice("tasks") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })

        println(projects)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("tasks").size == 2)
        Assert.assertTrue(projects[0].list("projectWorkers").size == 0)

        Assert.assertTrue(projects[0].nestedl("tasks[0].checklists").size == 2)

        Assert.assertTrue(projects[0].nested("tasks[0].checklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("tasks[0].checklists[1].name") == "bar check")

        //Assert.assertTrue(projects[0].nested("Tasks[1].checklists") == null)

        //3 догружаем 2ю коллекцию
        dataService.upgrade(projects, projection()
                .with {
                    slice("projectWorkers") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })

        println(projects)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("tasks").size == 2)
        Assert.assertTrue(projects[0].list("projectWorkers").size == 2)

        Assert.assertTrue(projects[0].nestedl("tasks[0].checklists").size == 2)
        Assert.assertTrue(projects[0].nested("tasks[0].checklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("tasks[0].checklists[1].name") == "bar check")

        Assert.assertTrue(projects[0].nested("tasks[1].checklists") == null)

    }

    @Test
            //сделаем апгрейд на третем уровне:
            //Project(1)-->Tasks(2)-->Checklist(3) - загрузим чеклисты
    fun testUpgradesOnSecondLevel() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                on("Project")
                        .scalars().withCollections()
                        .where("{{name}} = 'QDP'")
                        .order(f("Tasks.id"))
        )

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("Tasks").size == 2)
        Assert.assertTrue(projects[0].nested("Tasks[0].checklists") == null)
        //2 догружаем 1ю коллекцию
        //todo: запрос здесь генерится не оптимальный:есть лишний джойн на  Task хотя он не нужен
        dataService.upgrade(projects, projection()
                .with {
                    slice("Tasks") //загружаем коллекуию тасков
                            .with {
                                slice("checklists") //загружаем коллекуию чеков
                                        .scalars()
                            }
                }
        )

        println(projects)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].nestedl("Tasks[0].checklists").size == 2)
        Assert.assertTrue(projects[0].nested("Tasks[0].checklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("Tasks[0].checklists[1].name") == "bar check")

    }


    //сделаем апгрейд на третем уровне:
    //Project(1)-->Tasks(2)-->Checklist(3) - загрузим чеклисты
    //тест такой же, но на самом деле мы апгрейдим таски
    @Test
    fun testUpgradesOnSecondLevel2() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                on("Project")
                        .scalars().withCollections()
                        .where("{{name}} = 'QDP'")
                        .order(f("Tasks.id"))
        )

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("Tasks").size == 2)
        Assert.assertTrue(projects[0].nested("Tasks[0].checklists") == null)

        //2 догружаем 1ю коллекцию
        dataService.upgrade(projects.flatMap { p -> p.list("Tasks") }, projection()
                .with {
                    slice("checklists") //загружаем коллекуию чеков
                            .scalars()
                }
        )

        println(projects)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].nestedl("Tasks[0].checklists").size == 2)
        Assert.assertTrue(projects[0].nested("Tasks[0].checklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("Tasks[0].checklists[1].name") == "bar check")

    }


    //тест на использолвание отдельного запроса на вытягивание коллекции
    @Test
    fun testSelectCollectionWithSeparateSelect() {

        sqlStatistics.start()
        //1 грузим  проекты без коллекций
        val projects = dataService
                .findAll(on("Project")
                        .scalars()
                        .with {
                            slice("tasks") //загружаем коллекуию тасков
                                    .scalars().asSelect()
                                    .with {
                                        slice("checklists") //загружаем коллекуию чеклистов
                                                .scalars().asSelect()
                                    }
                        }
                        .where("{{name}} = 'QDP'")
                )

        println(projects)

        Assert.assertTrue(sqlStatistics.queries().size==3)

        assertBodyEquals(sqlStatistics.queries()[0].sql,
                "SELECT \n" +
                        "\t project1.\"id\"  AS  id1,  project1.\"name\"  AS  name1\n" +
                        "FROM project  project1 WHERE project1.\"name\" = 'QDP' ")

        assertBodyEquals(sqlStatistics.queries()[1].sql,
                "SELECT \n" +
                        "\t task1.\"id\"  AS  id1,  task1.\"name\"  AS  name1,  (task1.\"project_id\") AS  backRefId__\n" +
                        "FROM task  task1 \n" +
                        "WHERE task1.\"project_id\" in (:param0)   ")

        assertBodyEquals(sqlStatistics.queries()[2].sql,
                "SELECT \n" +
                        "\t checklist1.\"id\"  AS  id1,  checklist1.\"name\"  AS  name1,  (checklist1.\"task_id\") AS  backRefId__\n" +
                        "FROM checklist  checklist1 \n" +
                        "WHERE checklist1.\"task_id\" in (:param0)   ")

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].nestedl("tasks[0].checklists").size == 2)
        Assert.assertTrue(projects[0].nested("tasks[0].checklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("tasks[0].checklists[1].name") == "bar check")

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
                .findAll(on("Project")
                        .scalars()
                        .with {
                            slice("tasks") //загружаем коллекуию тасков
                                    .scalars().asSelect()
                                    .with {
                                        slice("checklists") //загружаем коллекуию чеклистов
                                                .scalars()
                                    }
                        }
                        .where("{{name}} = 'QDP'")
                )

        println(projects)

        Assert.assertTrue(sqlStatistics.queries().size==2)

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].nestedl("tasks[0].checklists").size == 2)
        Assert.assertTrue(projects[0].nested("tasks[0].checklists[0].name") == "foo check")
        Assert.assertTrue(projects[0].nested("tasks[0].checklists[1].name") == "bar check")

        sqlStatistics.stop()
    }

}