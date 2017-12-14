package com.datamaps.mappings

import com.datamaps.BaseSpringTests
import com.datamaps.maps.projection
import com.datamaps.maps.slice
import org.testng.Assert
import org.testng.annotations.Test

/**
 * Created by Щукин on 07.11.2017.
 */
class DataProjectionAPITests : BaseSpringTests() {

    @Test
    fun testProjectionsApi() {

        val dp = projection("JiraWorker")
                .group("scalars").group("withCollections")
                .field("gender")
                .with { slice("gender") }

        Assert.assertEquals(dp.entity, "JiraWorker")
        Assert.assertEquals(dp.groups.size, 2)
        Assert.assertEquals(dp.fields.size, 1)

        val dp2 = projection("JiraDepartment")
                .scalars().withRefs().withCollections()
                .with {
                    slice("parent")
                            .field("n")
                            .field("n")
                }
                .with {
                    slice("childs")
                            .field("n")
                            .with {
                                slice("parent")
                                        .field("n")
                            }
                }

        Assert.assertEquals(dp2.entity, "JiraDepartment")
        Assert.assertEquals(dp2.groups.size, 3)
        Assert.assertEquals(dp2.fields.size, 2)
    }


}