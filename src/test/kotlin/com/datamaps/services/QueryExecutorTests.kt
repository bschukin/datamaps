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
    fun testBuildQuery01() {

        var dp = DataProjection("JiraGender")

        var q = queryBuilder.createQueryByDataProjection(dp)

        var list = queryExecutor.findAll(q)

        list.forEach { e->println(e) }
        Assert.assertEquals(list.size, 4)

        val indeх = list.indexOf(DataMap("JiraGender", 1))
        Assert.assertTrue(indeх >=0)
        val dm =  list[indeх]
        Assert.assertTrue(dm["gender"] as String == "woman")
    }


}