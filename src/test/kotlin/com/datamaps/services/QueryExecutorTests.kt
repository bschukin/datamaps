package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.mappings.DataProjection
import com.datamaps.mappings.slice
import com.datamaps.maps.DataMap
import org.testng.Assert
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class QueryExecutorTests : BaseSpringTests() {


    @Test
            //простейшие тесты на квери: на лысую таблицу (без вложенных сущностей)
    fun testExecQuery01() {

        val dp = DataProjection("JiraGender")

        val q = queryBuilder.createQueryByDataProjection(dp)

        val list = queryExecutor.findAll(q)

        list.forEach { e -> println(e) }
        Assert.assertEquals(list.size, 4)

        val indeх = list.indexOf(DataMap("JiraGender", 1L))
        Assert.assertTrue(indeх >= 0)
        val dm = list[indeх]
        Assert.assertTrue(dm["gender"] as String == "woman")
    }


    @Test(invocationCount = 1)//простейшие тесты на квери: на таблицу c вложенными сущностями M-1
    fun testExecQuery02() {
        var dp = DataProjection("JiraWorker")
                .group("full")
                .with {
                    slice("gender")
                            .field("gender")
                }

        val q = queryBuilder.createQueryByDataProjection(dp)

        val list = queryExecutor.findAll(q)

        list.forEach { e -> println(e) }

        Assert.assertEquals(list.size, 5)

        //всякие проверочки
        val indeх = list.indexOf(DataMap("JiraWorker", 1L))
        Assert.assertTrue(indeх >= 0)
        val dm = list[indeх]
        Assert.assertTrue(dm["email"] as String == "madonna@google.com")
        Assert.assertTrue(dm("gender")["gender"] == "woman")

        val indeх2 = list.indexOf(DataMap("JiraWorker", 5L))
        val dm2 = list[indeх2]

        Assert.assertTrue(dm2["email"] as String == "mylene@francetelecom.fr")
        Assert.assertTrue(dm2("gender")["gender"] == "woman")

        //самый интересный ассерт: убеждаемся, что ссылки на гендер - физически это один и тот же инстанс
        Assert.assertTrue(dm2["gender"] === dm["gender"])
    }


    @Test(invocationCount = 1)//обрежем jiraWorker поосновательней
    fun testExecQuery03() {
        var dp = DataProjection("JiraWorker")
                .field("name")
                .with {
                    slice("gender")
                            .field("gender")
                }

        val q = queryBuilder.createQueryByDataProjection(dp)

        val list = queryExecutor.findAll(q)

        list.forEach { e -> println(e) }

        //всякие проверочки
        val indeх = list.indexOf(DataMap("JiraWorker", 1L))
        Assert.assertTrue(indeх >= 0)
        val dm = list[indeх]
        Assert.assertTrue(dm["email"] == null)
        Assert.assertTrue(dm("gender")["gender"] == "woman")

        val indeх2 = list.indexOf(DataMap("JiraWorker", 5L))
        val dm2 = list[indeх2]

        Assert.assertTrue(dm2["email"] == null)
        Assert.assertTrue(dm2("gender")["gender"] == "woman")

        //самый интересный ассерт: убеждаемся, что ссылки на гендер - физически это один и тот же инстанс
        Assert.assertTrue(dm2["gender"] === dm["gender"])
    }

    @Test(invocationCount = 1)//собираем инфо-п
    fun testBuildQuery05() {
        var dp = DataProjection("JiraStaffUnit")
                .full()
                .field("name")
                .with {
                    slice("worker")
                            .full()
                }
                .field("gender")

        val q = queryBuilder.createQueryByDataProjection(dp)
        val list = queryExecutor.findAll(q)

        list.forEach { e -> println(e) }
        //todo ассерты
    }

    @Test(invocationCount = 1)//JiraDepartment: структура-дерево
    fun testBuildQuery04() {
        var dp = DataProjection("JiraDepartment")
                .field("name")
                .field("parent")
        // /*  */.inner()
        ///*      */.parentField("name")
        ///*      */.parentLinkField("parent")
        ///*  */.end()

        //1 тест на  структуру по которой построится запрос
        val q = queryBuilder.createQueryByDataProjection(dp)
        val list = queryExecutor.findAll(q)

        list.forEach { e -> println(e) }
        //todo ассерты
    }

    @Test(invocationCount = 1)//Коллеция 1-N
    fun testExecQuery06() {
        var dp = DataProjection("JiraProject")
                .full()
                .with {
                    slice("jiraTasks")
                            .scalars().withRefs()
                }
        //1 тест на  структуру по которой построится запрос
        val q = queryBuilder.createQueryByDataProjection(dp)

        val list = queryExecutor.findAll(q)

        val res = StringBuilder()
        list.forEach { e -> res.append(e.toString()) }
        list.forEach { e -> println(e) }

        assertBodyEquals("""
            {
  "entity": "JiraProject",
  "id": 1,
  "name": "SAUMI",
  "jiraTasks": [
    {
      "entity": "JiraTask",
      "id": 1,
      "jiraProject": {
        "entity": "JiraProject",
        "id": 1,
        "isBackRef": "true"
      },
      "name": "SAUMI-001"
    },
    {
      "entity": "JiraTask",
      "id": 2,
      "jiraProject": {
        "entity": "JiraProject",
        "id": 1,
        "isBackRef": "true"
      },
      "name": "SAUMI-002"
    }
  ]
}
{
  "entity": "JiraProject",
  "id": 2,
  "name": "QDP",
  "jiraTasks": [
    {
      "entity": "JiraTask",
      "id": 3,
      "jiraProject": {
        "entity": "JiraProject",
        "id": 2,
        "isBackRef": "true"
      },
      "name": "QDP-003"
    },
    {
      "entity": "JiraTask",
      "id": 4,
      "jiraProject": {
        "entity": "JiraProject",
        "id": 2,
        "isBackRef": "true"
      },
      "name": "QDP-004"
    }
  ]
}
        """.trimIndent(), res.toString())
    }

    @Test
    fun testExecQuery07WithId() {
        val dp = DataProjection("JiraProject", 1L)
                .full()
                .with {
                    slice("jiraTasks")
                            .full()
                }
        //1 тест на  структуру по которой построится запрос
        val q = queryBuilder.createQueryByDataProjection(dp)

        val e = queryExecutor.executeSingle(q)

        println(e)

        with(e!!)
        {
            assertTrue(id == 1L)
            assertTrue(this["name"] == "SAUMI")
            assertTrue(list("jiraTasks").size == 2)
        }
    }
}