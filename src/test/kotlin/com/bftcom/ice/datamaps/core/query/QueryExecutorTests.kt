package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.core.util.toJson
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Created by Щукин on 03.11.2017.
 */
open class QueryExecutorTests : BaseSpringTests() {


    @Test
            //простейшие тесты на квери: на лысую таблицу (без вложенных сущностей)
    fun testExecQuery01() {

        val dp = Gender.slice {  }

        val q = queryBuilder.createQueryByDataProjection(dp)

        val list = queryExecutor.findAll<Gender>(q)


        list.forEach { e ->
            println(e[{ isClassic}])
            println(e)
        }
        Assert.assertEquals(list.size, 4)

        val index = list.indexOf(DataMap("Gender", 1))
        Assert.assertTrue(index >= 0)
        val dm = list[index]
        Assert.assertTrue(dm["name"] as String == "woman")
    }


    @Test()//простейшие тесты на квери: на таблицу c вложенными сущностями M-1
    fun testExecQuery02() {
        val dp = Projection("Person")
                .group("full")
                .with {
                    slice("gender")
                            .field("name")
                }

        val q = queryBuilder.createQueryByDataProjection(dp)

        val list = queryExecutor.findAll<UndefinedFieldSet>(q)

        list.forEach { e -> println(e) }

        Assert.assertEquals(list.size, 5)

        //всякие проверочки
        val index = list.indexOf(DataMap("Person", 1))
        Assert.assertTrue(index >= 0)
        val dm = list[index]
        Assert.assertTrue(dm["email"] as String == "madonna@google.com")
        Assert.assertTrue(dm("gender")!!["name"] == "woman")

        val index2 = list.indexOf(DataMap("Person", 5))
        val dm2 = list[index2]

        Assert.assertTrue(dm2["email"] as String == "mylene@francetelecom.fr")
        Assert.assertTrue(dm2("gender")!!["name"] == "woman")

        //самый интересный ассерт: убеждаемся, что ссылки на гендер - физически это один и тот же инстанс
        Assert.assertTrue(dm2["gender"] === dm["gender"])
    }


    @Test()//обрежем person поосновательней
    fun testExecQuery03() {
        val dp = Projection("Person")
                .field("name")
                .with {
                    slice("gender")
                            .field("name")
                }

        val q = queryBuilder.createQueryByDataProjection(dp)

        val list = queryExecutor.findAll<UndefinedFieldSet>(q)

        list.forEach { e -> println(e) }

        //всякие проверочки
        val index = list.indexOf(DataMap("Person", 1))
        Assert.assertTrue(index >= 0)
        val dm = list[index]
        Assert.assertTrue(dm["email"] == null)
        Assert.assertTrue(dm("gender")!!["name"] == "woman")

        val index2 = list.indexOf(DataMap("Person", 5))
        val dm2 = list[index2]

        Assert.assertTrue(dm2["email"] == null)
        Assert.assertTrue(dm2("gender")!!["name"] == "woman")

        //самый интересный ассерт: убеждаемся, что ссылки на гендер - физически это один и тот же инстанс
        Assert.assertTrue(dm2["gender"] === dm["gender"])
    }

    @Test()//собираем инфо-п
    fun testBuildQuery05() {
        val dp = Projection("Department")
                .full()
                .field("name")
                .with {
                    slice("boss")
                            .full()
                }
                .field("city")

        val q = queryBuilder.createQueryByDataProjection(dp)
        val list = queryExecutor.findAll<UndefinedFieldSet>(q)

        list.forEach { e -> println(e) }
        //todo ассерты
    }

    @Test()//Department: структура-дерево
    fun testBuildQuery04() {
        val dp = Projection("Department")
                .field("name")
                .field("parent")
        // /*  */.inner()
        ///*      */.parentField("name")
        ///*      */.parentLinkField("parent")
        ///*  */.end()

        //1 тест на  структуру по которой построится запрос
        val q = queryBuilder.createQueryByDataProjection(dp)
        val list = queryExecutor.findAll<UndefinedFieldSet>(q)

        list.forEach { e -> println(e) }
        //todo ассерты
    }

    @Test()//Коллеция 1-N
    fun testExecQuery06() {
        val dp = Projection("Project")
                .scalars().withRefs()
                .with {
                    slice("Tasks")
                            .scalars().withRefs()
                }
        //1 тест на  структуру по которой построится запрос
        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        val list = queryExecutor.findAll<UndefinedFieldSet>(q)

        val res = StringBuilder()
        list.forEach { e -> res.append(e.toJson()) }
        list.forEach { e -> println(e.toJson()) }

        assertBodyEquals("""
{
  "entity": "Project",
  "id": 1,
  "Tasks": [
    {
      "entity": "Task",
      "id": 1,
      "Project": {
        "entity": "Project",
        "id": 1,
        "isBackRef": "true"
      },
      "name": "SAUMI-001"
    },
    {
      "entity": "Task",
      "id": 2,
      "Project": {
        "entity": "Project",
        "id": 1,
        "isBackRef": "true"
      },
      "name": "SAUMI-002"
    }
  ],
  "name": "SAUMI"
}
{
  "entity": "Project",
  "id": 2,
  "Tasks": [
    {
      "entity": "Task",
      "id": 3,
      "Project": {
        "entity": "Project",
        "id": 2,
        "isBackRef": "true"
      },
      "name": "QDP-003"
    },
    {
      "entity": "Task",
      "id": 4,
      "Project": {
        "entity": "Project",
        "id": 2,
        "isBackRef": "true"
      },
      "name": "QDP-004"
    }
  ],
  "name": "QDP"
}
        """.trimIndent(), res.toString())

    }

    @Test
    fun testExecQuery07WithId() {
        val dp = Projection("Project", 1L)
                .full()
                .with {
                    slice("tasks")
                            .full()
                }
        //1 тест на  структуру по которой построится запрос
        val q = queryBuilder.createQueryByDataProjection(dp)

        val e = queryExecutor.executeSingle(q)

        println(e)

        with(e!!)
        {
            assertTrue(id == 1)
            assertTrue(this["name"] == "SAUMI")
            assertTrue(list("tasks").size == 2)
        }
    }

    @Test
    fun testExecQueryDeleteAll() {
        //создаем проект  а при нем два таска
        val p = Project {
            it.id = 1000
            it[name] = "Project XXX"
            it[tasks].add(Task {
                it[name] = "task01"
            })
            it[tasks].add(Task {
                it[name] = "task02"
            })
        }
        dataService.flush()
        val p1  = dataService.find_(Project.on().id(p.id).withCollections())
        assertTrue(p1[{ tasks}].size==2)

        //удаляем таски проекцией
        dataService.deleteAll(Task.on().where ( "project_id = 1000" ))

        //проверяем что таски в проекте исчезли
        val p2  = dataService.find_(Project.on().id(p.id).withCollections())
        assertTrue(p2[{ tasks}].size==0)
    }

    @Test
    fun testDeleteInListOfLong() {
        //создаем 10 персон
        val personIds = (0..9).map {
            val person = Person { pt ->
                pt[name] = it.toString()
            }
            dataService.flush()
            person.id
        }.toList()

        //проверяем что все создались
        val count = dataService.count(Person.slice { filter { id IN personIds } })
        assertEquals(10,count)

        //удаляем всех
        dataService.deleteAll(Person.slice { filter { id IN personIds } })
        dataService.flush()

        //проверяем что все удалились
        val count2 = dataService.count(Person.slice { filter { id IN personIds } })
        assertEquals(0,count2)
    }
}