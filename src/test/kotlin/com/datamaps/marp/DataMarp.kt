package com.datamaps.marp

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.maps.*
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


    @Test(invocationCount = 1)//Коллеция 1-N
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

        assertBodyEquals("""
            {
  "entity": "JiraProject",
  "id": 1,
  "n": "SAUMI",
  "jiraTasks": [
    {
      "entity": "JiraTask",
      "id": 1,
      "jiraProject": {
        "entity": "JiraProject",
        "id": 1,
        "isBackRef": "true"
      },
      "n": "SAUMI-001"
    },
    {
      "entity": "JiraTask",
      "id": 2,
      "jiraProject": {
        "entity": "JiraProject",
        "id": 1,
        "isBackRef": "true"
      },
      "n": "SAUMI-002"
    }
  ]
}
{
  "entity": "JiraProject",
  "id": 2,
  "n": "QDP",
  "jiraTasks": [
    {
      "entity": "JiraTask",
      "id": 3,
      "jiraProject": {
        "entity": "JiraProject",
        "id": 2,
        "isBackRef": "true"
      },
      "n": "QDP-003"
    },
    {
      "entity": "JiraTask",
      "id": 4,
      "jiraProject": {
        "entity": "JiraProject",
        "id": 2,
        "isBackRef": "true"
      },
      "n": "QDP-004"
    }
  ]
}
        """.trimIndent(), res.toString())
    }


}