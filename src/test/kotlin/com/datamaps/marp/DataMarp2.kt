package com.datamaps.marp

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.mappings.*
import com.datamaps.maps.DataMap
import org.testng.Assert
import org.testng.annotations.Test

class DataMarp2 : BaseSpringTests() {

    class Gender(var gender: String) : DataMap()

    class Worker(var name: String, var gender: Gender) : DataMap()

    class Department(var name: String, var fullName: String,
                     var parent: Department, var childs: MutableList<Department>): DataMap()

    class StaffUnit(var name: String, var worker: Worker,
                    var gender: Gender): DataMap()

    class Project(var name: String,  var tasks: MutableList<Task>): DataMap()

    class Task(var name: String)  : DataMap()



    @Test
    fun basicProjectionUses() {

        //простой пример
        val gender = dataService.find(
                on(Gender::class)
                        .id(2L)) as Gender
        gender.gender = "men"

        dataService.flush()

        //более сложный пример
        val worker = dataService.find(
                on(Worker::class)
                        .id(1L)
                        .with {
                            slice(Worker::gender)
                                    .field(Gender::gender)
                        }
        ) as Worker


        if (worker.gender.gender != gender.gender)
            worker.gender = gender

        //но и так будет работать
        if (worker("gender")["gender"] != gender["gender"])
            worker["gender"] = gender

        //пример с деревом
        val dp2 = on(Department::class)
                .with {
                    slice(Department::parent)
                            .field(Department::name)
                            .field(Department::fullName)
                }
                .with {
                    slice(Department::childs)
                            .field(Department::name)
                            .with {
                                slice(Department::parent)
                                        .field(Department::name)
                            }
                }
    }


    @Test
    fun basicProjectionSlices01() {

        var dp = on(StaffUnit::class)
                .withRefs()
                .field(StaffUnit::name)
                .with {
                    slice(StaffUnit::worker)
                            .scalars().withRefs()
                }
                .field(StaffUnit::gender)

        //коллекции
        dp = on(Project::class)
                .withCollections()
                .with {
                    slice(Project::tasks)
                            .scalars().withRefs()
                }

        //алиасы
        dp = on(Project::class)
                .full()
                .alias("JP")
                .with {
                    slice(Project::tasks)
                            .alias("JT")
                            .scalars().withRefs()
                }
    }

    @Test
    fun basicFiltersApiMethod() {

        val dp = on(StaffUnit::class)
                .withRefs()
                .field(StaffUnit::name)
                .with {
                    slice(StaffUnit::worker).alias("W")
                            .scalars().withRefs()
                }
                .field(StaffUnit::gender)
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
        var dp = on(StaffUnit::class)
                .withRefs()
                .field(StaffUnit::name)
                .with {
                    slice(StaffUnit::worker)
                            .scalars().withRefs()
                }
                .field(StaffUnit::gender)
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
        dp =  on(StaffUnit::class)
                .withRefs()
                .field(StaffUnit::name)
                .with {
                    slice(StaffUnit::worker).alias("W")
                            .scalars().withRefs()
                }
                .field(StaffUnit::gender)
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
        val gender = dataService.find(on(Gender::class)
                .formula("caption", """
                    case when {{id}}=1 then 'Ж'
                         when {{id}}=2 then 'М'
                         else 'О' end
                """)
                .filter(f("caption") eq 'Ж'))!!

        println(gender["caption"])

        //lateral
        val dp = on(Project::class)
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
                on(Project::class)
                        .scalars()
                        .where("{{n}} = 'QDP'")
        )

        //2 догружаем коллекции
        dataService.upgrade(projects, projection()
                .with {
                    slice(Project::tasks) //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })


        //use complex indexator
        Assert.assertTrue(projects[0].nested("jiraTasks[0].jiraChecklists[0].n") == "foo check")
    }


    @Test(invocationCount = 1)//Коллеция 1-N
    fun testPrintToJson() {
        var list = dataService.findAll(on(Project::class)
                .full()
                .with {
                    slice(Project::tasks)
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