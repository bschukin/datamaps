package com.datamaps.marp

import com.datamaps.BaseSpringTests
import com.datamaps.StaffUnit
import com.datamaps.Worker
import com.datamaps.maps.*
import com.datamaps.servicedesk.ORG
import com.datamaps.servicedesk.Organisation
import org.testng.Assert
import org.testng.annotations.Test


/***
 * Базовые примеры на слайсы и проекции
 */
class DataMarp : BaseSpringTests() {


    @Test
    fun basicProjectionUses() {

        //простой пример
        val gender = dataService.find(
                on("JiraGender")
                        .id(2L))!!
        gender["gender"] = "men"
        dataService.flush()

        //более сложный пример
        val worker = dataService.find(
                on("JiraWorker")
                        .id(1L)
                        .field("n")
                        .with {
                            slice("gender")
                                    .field("gender")
                        }
        )!!

        if (worker("gender")["gender"] != gender["gender"])
            worker["gender"] = gender
        //не флашим, дожидаемся окончания транзакции


        //пример с деревом
        val dp2 = on("JiraDepartment")
                .with {
                    slice("parent")
                            .field("n")
                            .field("n")
                }
                .with {
                    slice("childs")
                            .field("n")
                            .with {
                                slice("parent")
                                        .field("n")
                            }
                }
    }


    @Test
    fun basicProjectionSlices01() {

        var dp = on("JiraStaffUnit")
                .withRefs()
                .field("n")
                .with {
                    slice("worker")
                            .scalars().withRefs()
                }
                .field("gender")

        //коллекции
        dp = on("JiraProject")
                .withCollections()
                .with {
                    slice("JiraTasks")
                            .scalars().withRefs()
                }

        //алиасы
        dp = on("JiraProject")
                .full()
                .alias("JP")
                .with {
                    slice("jiraTasks")
                            .alias("JT")
                            .scalars().withRefs()
                }
    }


    /**
     * Пример "плоского" API проекций. Просто перечисляются поля через точку.
     * Только эти поля и будут вытащены
     */
    @Test
    fun flatProjectionsExamples() {

        var dp = on(StaffUnit).with(
                +StaffUnit.name,
                +StaffUnit.worker().name,
                +StaffUnit.worker().email,
                +StaffUnit.gender().gender
        ).filter(-StaffUnit.gender().gender eq "M")
    }


    /**
     * Пример "плоского" API проекций.
     * И пример доступа к полю через вложеннную пропертю фиелдсета
     * (it[ORG.contracts[0].contract().products[0].product().name])
     */
    @Test
    fun flatProjectionsAndNestedPropertyAccess() {

        //1) по организации - показать все имеющиеся SLA
        //списком:  организация, контракт, продукт, услуга, SLA
        //способ 1: от организации
        val res = dataService.findAll(on(Organisation)
                .with(+ORG.contracts().contract().number,
                        +ORG.contracts().contract().products().product().name,
                        +ORG.contracts().contract().products().slas().service().name,
                        +ORG.contracts().contract().products().slas().sla,
                        +ORG.name)
                .filter { -ORG.name eq "ЗАО БИС" }
        )

        val s = StringBuilder()
        res.forEach {
            s.append("${it[ORG.name]} " +
                    "| ${it[ORG.contracts[0].contract().number]}" +
                    "| ${it[ORG.contracts[0].contract().products[0].product().name]}" +
                    "| ${it[ORG.contracts[0].contract().products[0].slas[0].service().name]}" +
                    "| ${it[ORG.contracts[0].contract().products[0].slas[0].sla]}")
        }


    }

    @Test
    fun basicFiltersApiMethod() {

        val dp = on("JiraStaffUnit")
                .withRefs()
                .field("n")
                .with {
                    slice("worker").alias("W")
                            .scalars().withRefs()
                }
                .field("gender")
                .filter({
                    {
                        {
                            (f("W.gender.id") eq f("gender.id"))
                        } or
                                { f("W.n") eq value("nanyr") }
                    } and
                            {
                                f("W.email") eq value("gazman@google.com") or
                                        { f("n") eq value("zzz") }
                            }
                })
                .limit(100).offset(100)
    }

    @Test
    fun basicFiltersBindingMethod() {

        //без алиасов
        var dp = on("JiraStaffUnit")
                .withRefs()
                .field("n")
                .with {
                    slice("worker")
                            .scalars().withRefs()
                }
                .field("gender")
                .where("""
                    (
                         {{worker.gender.id}} = {{gender.id}}
                        or
                         {{worker.n}} = :param0
                    ) and
                    (
                        {{worker.email}} = :param1 or
                        {{n}} = 'zzz'
                    )
                """)
                .param("param0", "nanyr")
                .param("param1", "gazman@google.com")


        //с алиасом
        dp = on("JiraStaffUnit")
                .withRefs()
                .field("n")
                .with {
                    slice("worker").alias("W")
                            .scalars().withRefs()
                }
                .field("gender")
                .where("""
                    (
                         {{W.gender.id}} = {{gender.id}}
                        or
                         {{W.n}} = :param0
                    ) and
                    (
                        {{W.email}} = :param1 or
                        {{n}} = 'zzz'
                    )
                """)
    }

    @Test
    fun testExistsInQueryWithBindinds() {

        //пример использования байндинга объектов графа запроса в WHERE-выражении

        val p = (on("JiraProject")
                .with {
                    slice("jiraTasks") //загружаем коллекцию тасков
                            .with {
                                slice("jiraChecklists") //загружаем коллекцию чеков
                                        .scalars()
                            }
                }
                .where("{{name}} = 'QDP' AND " +
                        "EXISTS (SELECT j.id FROM JIRA_CHECKLIST j WHERE j.JIRA_TASK_ID = {{jiraTasks.id}})")
                )

        val projects = dataService.findAll(p)
        println(projects)
    }


    @Test
    fun basicFormulas() {

        //формула
        val gender = dataService.find(on("JiraGender")
                .formula("caption", """
                    case when {{id}}=1 then 'Ж'
                         when {{id}}=2 then 'М'
                         else 'О' end
                """)
                .filter(f("caption") eq 'Ж'))!!

        println(gender["caption"])

        //lateral
        val dp = on("JiraProject")
                .lateral("tasks", """
                    (select string_agg(t.n, ';') as tasks1, count(*) as qty1
                            from jira_task t
                            where t.jira_project_id= {{id}}
                            ) tasks on true
                    """,
                        "tasks1" to "tasks", "qty1" to "qty")
                .where("{{tasks.tasks1}} like '%001%'")

    }

    @Test
    fun basicUpgrades() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                on("JiraProject")
                        .scalars()
                        .where("{{n}} = 'QDP'")
        )

        //2 догружаем коллекции
        dataService.upgrade(projects, projection()
                .with {
                    slice("jiraTasks") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })


        //use complex indexator
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[0].n") == "foo check")
    }

    /***
     * Пример с отдельными запросами для вложенных коллекций
     *
     * мне короче удалсоь сделать так, что будет три запроса ровно:
    - первый на проекты.
    - второй на все задачи выбраных проектов
    - третий - на все чеклисты всех выбранных задач
     */
    @Test

    fun testSelectCollectionWithSeparateSelect() {

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
    }


    @Test
            /**
             * пример создания датамапа на основе выполнения SQL.
             * маппинг в адатамап осуществляет по именам колонок (columnLabel) представленным в резулт-сете.
             * */
    fun testSqlToMapMethod() {
        var res = dataService.sqlToFlatMap(Worker.entity,
                "select w.id , w.name as noname, g.gender, g.id as gid from jira_worker w " +
                        "left join jira_gender g on g.id = w.gender_id " +
                        "where g.gender = :_name",
                mapOf("_name" to "man"))!!

        println(res)

        """
            {
                "entity": "JiraWorker",
                "id": 2,
                "GID": "2",
                "GENDER": "man",
                "NONAME": "John Lennon"
        }"""

    }


    /**
     * Примеры выгрузки в json
     */
    @Test
    fun testPrintToJson() {
        var list = dataService.findAll(on("JiraProject")
                .full()
                .with {
                    slice("jiraTasks")
                            .scalars().withRefs()
                })

        //1 тест на  структуру по которой построится запрос
        val res = StringBuilder()
        list.forEach { e -> res.append(e.toString()) }
        list.forEach { e -> println(e) }

    }

}