package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.Gender
import com.datamaps.StaffUnit
import com.datamaps.maps.on
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class DataServiceAsyncTests : BaseSpringTests() {


    @Test
    fun testAsyncCase01() {

        dataService
                .async()
                .find_(on(Gender)
                        .filter(-Gender.gender eq "woman")
                )
                .doWithResult { g ->
                    println(g[Gender.isClassic])
                    assertTrue(g[Gender.isClassic])
                }

        Thread.sleep(500)
    }

    @Test
    fun testAsyncCase02() {

        dataService
                .async()
                .findAll(on(StaffUnit)
                        .with(+StaffUnit.gender().gender, +StaffUnit.worker().name)
                )
                .doWithResult { list ->
                    list.forEach {
                        print(it)
                    }
                }

        Thread.sleep(500)
    }
}