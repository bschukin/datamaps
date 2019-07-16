package com.bftcom.ice.server.util

import com.bftcom.ice.common.maps.Projection
import com.bftcom.ice.common.maps.slice
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Project
import com.bftcom.ice.server.assertBodyEquals
import org.junit.Test
import kotlin.test.assertEquals

open class DataMapsJsonTests : BaseSpringTests() {

    @Test
    fun serializeDMListTest() {

        val dp = Projection("Project")
                .scalars().withRefs()
                .with {
                    slice("Tasks")
                            .scalars().withRefs()
                }
        val q = queryBuilder.createQueryByDataProjection(dp)

        val list = queryExecutor.findAll<Project>(q)

        val res = StringBuilder()

        res.append(list.toJson())
        assertBodyEquals(res.toString(), """
            [
  {
    "entity": "Project",
    "id": 1,
    "tasks": [
      {
        "entity": "Task",
        "id": 1,
        "project": {
          "entity": "Project",
          "id": 1,
          "isBackRef": true
        },
        "name": "SAUMI-001"
      },
      {
        "entity": "Task",
        "id": 2,
        "project": {
          "entity": "Project",
          "id": 1,
          "isBackRef": true
        },
        "name": "SAUMI-002"
      }
    ],
    "name": "SAUMI"
  },
  {
    "entity": "Project",
    "id": 2,
    "tasks": [
      {
        "entity": "Task",
        "id": 3,
        "project": {
          "entity": "Project",
          "id": 2,
          "isBackRef": true
        },
        "name": "QDP-003"
      },
      {
        "entity": "Task",
        "id": 4,
        "project": {
          "entity": "Project",
          "id": 2,
          "isBackRef": true
        },
        "name": "QDP-004"
      }
    ],
    "name": "QDP"
  }
]
        """.trimIndent())
    }

    @Test
    fun deserializeDMListTest() {
        val dataMapListFromJson = dataMapsFromJson("""
            [
  {
    "entity": "Project",
    "id": 1,
    "tasks": [
      {
        "entity": "Task",
        "id": 1,
        "project": {
          "entity": "Project",
          "id": 1,
          "isBackRef": true
        },
        "name": "SAUMI-001"
      },
      {
        "entity": "Task",
        "id": 2,
        "project": {
          "entity": "Project",
          "id": 1,
          "isBackRef": true
        },
        "name": "SAUMI-002"
      }
    ],
    "name": "SAUMI"
  },
  {
    "entity": "Project",
    "id": 2,
    "tasks": [
      {
        "entity": "Task",
        "id": 3,
        "project": {
          "entity": "Project",
          "id": 2,
          "isBackRef": true
        },
        "name": "QDP-003"
      },
      {
        "entity": "Task",
        "id": 4,
        "project": {
          "entity": "Project",
          "id": 2,
          "isBackRef": true
        },
        "name": "QDP-004"
      }
    ],
    "name": "QDP"
  }
]
        """)
        assertEquals(2, dataMapListFromJson!!.size)
    }
}