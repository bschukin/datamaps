package com.datamaps

import com.datamaps.maps.DataMap
import org.testng.Assert
import org.testng.annotations.Test
import java.lang.RuntimeException

/**
 * Created by Щукин on 28.10.2017.
 */

class DataMapTests {
    @Test
    fun workWithMaps() {

        val dt = DataMap("xxx", 700L);
        dt["prop1"] = 500;

        println(dt["prop1"])
        println(dt.name)
        println(dt.id)

        dt.list("myList").add(DataMap("bbb", 800))
    }

    @Test(expectedExceptions = arrayOf(RuntimeException::class))
    fun throwExceptionOnIdChange() {

        val dm = DataMap("xxx");
        dm.id = 10000;
        Assert.assertNotNull(dm.id)

        dm.id = 2000;
    }
}