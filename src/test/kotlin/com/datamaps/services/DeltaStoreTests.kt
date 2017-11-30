package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.maps.DataMap
import org.springframework.transaction.annotation.Transactional
import org.testng.Assert
import org.testng.annotations.Test


/**
 * Created by Щукин on 03.11.2017.
 */
@Transactional
class DeltaStoreTests : BaseSpringTests() {


    @Test
    fun testRegisterSimpleProperties()
    {
        val dm = DataMap("XXX", 1L)
        dm.silentSet("foo","foo0")
        dm.silentSet("bar", "bar0")
        dm["foo"] = "foo1"
        dm["bar"] = "bar1"
        dm["bar"] = "bar2"  //второе изменение не должно учитываться в бакете

        val context = DeltaStore.context.get()
        Assert.assertTrue(context.deltas.size==3)


        val dm2 = DataMap("XXX", 2L)
        dm2.silentSet("foo","foo00")
        dm2.silentSet("bar", "bar00")
        dm2["foo"] = "foo2"
        dm2["bar"] = "bar2"
        dm2["qqq"] = "qqq2"

        Assert.assertTrue(context.deltas.size==6)


        val buckets = DeltaStore.collectBuckets()
        Assert.assertTrue(buckets.size==2)

        Assert.assertTrue(buckets[0].dm.id == 1L)
        Assert.assertTrue(buckets[1].dm.id == 2L)

        Assert.assertTrue(buckets[0].deltas.size==2)
        Assert.assertTrue(buckets[1].deltas.size==3)

    }




}