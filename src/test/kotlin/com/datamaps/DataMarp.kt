package com.datamaps

import com.datamaps.mappings.*
import org.testng.Assert
import org.testng.annotations.Test

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
                        .field("name")
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
                            .field("name")
                            .field("fullName")
                }
                .with {
                    slice("childs")
                            .field("name")
                            .with {
                                slice("parent")
                                        .field("name")
                            }
                }
    }


    @Test
    fun basicProjectionSlices01() {

        var dp = on("JiraStaffUnit")
                .withRefs()
                .field("name")
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

    @Test
    fun basicFiltersApiMethod() {

        val dp = on("JiraStaffUnit")
                .withRefs()
                .field("name")
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
                                { f("W.name") eq value("nanyr") }
                    } and
                            {
                                f("W.email") eq value("gazman@google.com") or
                                        { f("name") eq value("zzz") }
                            }
                })
                .limit(100).offset(100)
    }

    @Test
    fun basicFiltersBindingMethod() {

        //без алиасов
        var dp = on("JiraStaffUnit")
                .withRefs()
                .field("name")
                .with {
                    slice("worker")
                            .scalars().withRefs()
                }
                .field("gender")
                .where("""
                    (
                         {{worker.gender.id}} = {{gender.id}}
                        or
                         {{worker.name}} = :param0
                    ) and
                    (
                        {{worker.email}} = :param1 or
                        {{name}} = 'zzz'
                    )
                """)
                .param("param0", "nanyr")
                .param("param1", "gazman@google.com")


        //с алиасом
        dp = on("JiraStaffUnit")
                .withRefs()
                .field("name")
                .with {
                    slice("worker").alias("W")
                            .scalars().withRefs()
                }
                .field("gender")
                .where("""
                    (
                         {{W.gender.id}} = {{gender.id}}
                        or
                         {{W.name}} = :param0
                    ) and
                    (
                        {{W.email}} = :param1 or
                        {{name}} = 'zzz'
                    )
                """)
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
                    (select string_agg(t.name, ';') as tasks1, count(*) as qty1
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
                        .where("{{name}} = 'QDP'")
        )

        //2 догружаем коллекции
        dataService.upgrade(projects, projection()
                .with {
                    slice("jiraTasks") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })


        //use complex indexator
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[0].name") == "foo check")
    }


}