package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.datamaps.DataMap
import com.bftcom.ice.datamaps.core.delta.DeltaStore
import org.junit.Assert
import org.junit.Test
import org.springframework.transaction.annotation.Transactional


/**
 * Created by Щукин on 03.11.2017.
 */

@Transactional
open class DeltaStoreTests : BaseSpringTests() {


    @Test
    fun testRegisterSimpleProperties() {
        val dm = DataMap("XXX", 1L)
        dm["foo", true] = "foo0"
        dm["bar", true] = "bar0"
        dm["foo"] = "foo1"
        dm["bar"] = "bar1"
        dm["bar"] = "bar2"  //второе изменение не должно учитываться в бакете

        val context = DeltaStore.context.get()
        Assert.assertTrue(context.current().deltas.size == 3)


        val dm2 = DataMap("XXX", 2L)
        dm2["foo", true] = "foo00"
        dm2["bar", true] = "bar00"
        dm2["foo"] = "foo2"
        dm2["bar"] = "bar2"
        dm2["qqq"] = "qqq2"

        Assert.assertTrue(context.current().deltas.size == 6)


        val buckets = DeltaStore.collectBuckets()
        Assert.assertTrue(buckets.size == 2)

        Assert.assertTrue(buckets[0].dm.id == 1L)
        Assert.assertTrue(buckets[1].dm.id == 2L)

        Assert.assertTrue(buckets[0].deltas.size == 2)
        Assert.assertTrue(buckets[1].deltas.size == 3)

    }


}