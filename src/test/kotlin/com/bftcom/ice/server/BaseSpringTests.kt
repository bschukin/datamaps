package com.bftcom.ice.server

import com.bftcom.ice.common.maps.*
import com.bftcom.ice.server.datamaps.QueryBuilder
import com.bftcom.ice.server.datamaps.QueryExecutor
import com.bftcom.ice.server.services.DataServiceExtd
import com.bftcom.ice.server.services.ShadowService
import com.bftcom.ice.server.services.SqlStatistics
import com.bftcom.ice.server.test.SpringProfileRule
import org.junit.Assert
import org.junit.Rule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests
import javax.annotation.Resource

/**
 * Created by Щукин on 01.11.2017.
 */


@SpringBootTest(classes = [TestApplication::class])
@ContextConfiguration("classpath:test-app-context.xml")
abstract class BaseSpringTests : AbstractTransactionalJUnit4SpringContextTests() {

    @Rule
    @JvmField
    final val rule =  SpringProfileRule.forSpringJunitClassRunner()


    @Autowired
    lateinit var namedParameterJdbcTemplate: NamedParameterJdbcOperations

    @Autowired
    lateinit var env: Environment

    @Autowired
    lateinit var queryBuilder: QueryBuilder

    @Autowired
    open lateinit var dataService: DataService


    val dataServiceExtd: DataServiceExtd
        get() = dataService as DataServiceExtd

    @Resource
    lateinit var queryExecutor: QueryExecutor

    @Autowired
    lateinit var sqlStatistics: SqlStatistics

    @Autowired
    lateinit var shadowService: ShadowService

    fun notExists(p: projection): Boolean {
        return dataService.find(p) == null
    }


    fun isProstgress(): Boolean = env.activeProfiles.contains("postgresql")
    fun isOracle(): Boolean = env.activeProfiles.contains("oracle")
    fun isFireBird(): Boolean = env.activeProfiles.contains("firebird")

    fun assertNull(res: Any?) {
        Assert.assertNull(res)
    }

    fun assertNotNull(obj: Any?) {
        Assert.assertNotNull(obj)
    }

    fun assertTrue(res: Boolean) {
        Assert.assertTrue(res)
    }

    fun assertFalse(res: Boolean) {
        Assert.assertFalse(res)
    }

    fun <T : FieldSet> find_(dp: DataProjectionF<T>): DataMapF<T> {
        return dataService.find_(dp)
    }

}

fun eraseAllWs(string: String): String = string.replace("\\s".toRegex(), "").replace("\"", "")


fun assertBodyEquals(string1: String, string2: String?) {
    if (string2 == null)
        return
    Assert.assertEquals(eraseAllWs(string1).toLowerCase(), eraseAllWs(string2).toLowerCase())
}

fun assertEqIgnoreCase(string1: String, string2: String) {
    Assert.assertEquals(string1.toLowerCase(), string2.toLowerCase())
}

fun assertEqIgnoreCase(obj: Any?, string2: String) {
    Assert.assertEquals(obj.toString().toLowerCase(), string2.toLowerCase())
}

fun assertEqIgnoreCase(list1: List<*>, list2: List<*>) {
    if (list1.size != list2.size)
        Assert.fail("lists have different size: ${list1.size} vs ${list2.size}")
    for (item: Int in list2.indices) {
        val i1 = list1[item].toString()
        val i2 = list2[item].toString()

        Assert.assertTrue("items №${item} of lists are different:  ${i1} vs ${i2} ", i1.equals(i2, true))
    }
}