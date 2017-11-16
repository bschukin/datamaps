package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.mappings.DataProjection
import com.datamaps.maps.DataMap
import org.springframework.beans.factory.annotation.Autowired
import org.testng.Assert
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class QueryExecutorTests : BaseSpringTests() {

    @Autowired
    lateinit var queryBuilder: QueryBuilder

    @Autowired
    lateinit var queryExecutor: QueryExecutor

    @Test
            //простейшие тесты на квери: на лысую таблицу (без вложенных сущностей)
    fun testExecQuery01() {

        val dp = DataProjection("JiraGender")

        val q = queryBuilder.createQueryByDataProjection(dp)

        val list = queryExecutor.findAll(q)

        list.forEach { e->println(e) }
        Assert.assertEquals(list.size, 4)

        val indeх = list.indexOf(DataMap("JiraGender", 1))
        Assert.assertTrue(indeх >=0)
        val dm =  list[indeх]
        Assert.assertTrue(dm["gender"] as String == "woman")
    }


    @Test(invocationCount = 1)//простейшие тесты на квери: на таблицу c вложенными сущностями M-1
    fun testExecQuery02() {
        var dp = DataProjection("JiraWorker")
                .group("full")
                .field("gender")
                /*  */.inner()
                /*      */.field("gender")
                /*  */.end()

        val q = queryBuilder.createQueryByDataProjection(dp)

        val list = queryExecutor.findAll(q)

        list.forEach { e->println(e) }

        Assert.assertEquals(list.size, 5)

        //всякие проверочки
        val indeх = list.indexOf(DataMap("JiraWorker", 1))
        Assert.assertTrue(indeх >=0)
        val dm =  list[indeх]
        Assert.assertTrue(dm["email"] as String == "madonna@google.com")
        Assert.assertTrue( dm("gender")["gender"] == "woman")

        val indeх2 = list.indexOf(DataMap("JiraWorker", 5))
        val dm2 =  list[indeх2]

        Assert.assertTrue(dm2["email"] as String == "mylene@francetelecom.fr")
        Assert.assertTrue( dm2("gender")["gender"] == "woman")

        //самый интересный ассерт: убеждаемся, что ссылки на гендер - физически это один и тот же инстанс
        Assert.assertTrue(dm2["gender"]  === dm["gender"])
    }


    @Test(invocationCount = 1)//обрежем jiraWorker поосновательней
    fun testExecQuery03() {
        var dp = DataProjection("JiraWorker")
                .field("name")
                .field("gender")
                /*  */.inner()
                /*      */.field("gender")
                /*  */.end()

        val q = queryBuilder.createQueryByDataProjection(dp)

        val list = queryExecutor.findAll(q)

        list.forEach { e->println(e) }

        //всякие проверочки
        val indeх = list.indexOf(DataMap("JiraWorker", 1))
        Assert.assertTrue(indeх >=0)
        val dm =  list[indeх]
        Assert.assertTrue(dm["email"]  == null)
        Assert.assertTrue( dm("gender")["gender"] == "woman")

        val indeх2 = list.indexOf(DataMap("JiraWorker", 5))
        val dm2 =  list[indeх2]

        Assert.assertTrue(dm2["email"]  == null)
        Assert.assertTrue( dm2("gender")["gender"] == "woman")

        //самый интересный ассерт: убеждаемся, что ссылки на гендер - физически это один и тот же инстанс
        Assert.assertTrue(dm2["gender"]  === dm["gender"])
    }

    @Test(invocationCount = 1)//собираем инфо-п
    fun testBuildQuery05() {
        var dp = DataProjection("JiraStaffUnit")
                .gfull()
                .field("name")
                .field("worker")
                /*  */.inner()
                /*      */.gfull()
                /*  */.end()
                .field("gender")

        val q = queryBuilder.createQueryByDataProjection(dp)
        val list = queryExecutor.findAll(q)

        list.forEach { e->println(e) }
        //todo ассерты
    }

    @Test(invocationCount = 1)//JiraDepartment: структура-дерево
    fun testBuildQuery04() {
        var dp = DataProjection("JiraDepartment")
                .field("name")
                .field("parent")
               // /*  */.inner()
                ///*      */.field("name")
                ///*      */.parentLinkField("parent")
                ///*  */.end()

        //1 тест на  структуру по которой построится запрос
        val q = queryBuilder.createQueryByDataProjection(dp)
        val list = queryExecutor.findAll(q)

        list.forEach { e->println(e) }
        //todo ассерты
    }
}