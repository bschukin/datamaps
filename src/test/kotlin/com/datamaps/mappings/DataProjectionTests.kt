package com.datamaps.mappings

import com.datamaps.BaseSpringTests
import org.testng.Assert
import org.testng.annotations.Test

/**
 * Created by Щукин on 07.11.2017.
 */
class DataProjectionTests : BaseSpringTests() {

    @Test
    fun testProjectionsApi() {

        var dp = DataProjection("JiraWorker")
                .group("default").group("collections")
                .field("gender")
                .inner().field("gender").end()

        dp = DataProjection("JiraDepartment")
                .group("default").group("collections")
                .field("parent")
                /*  */.inner()
                /*      */.field("name")
                /*      */.field("fullName")
                /*  */.end()
                .field("childs")
                /*  */.inner()
                /*      */.field("name")
                /*      */.field("parent")
                /*          */.inner()
                /*              */.field("name")
                /*          */.end()
                /*  */.end()
        Assert.assertEquals(dp.entity, "JiraDepartment")
        Assert.assertEquals(dp.groups.size, 2)
        Assert.assertEquals(dp.fields.size, 2)
    }


}