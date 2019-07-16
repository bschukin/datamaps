package com.bftcom.ice.server.datamaps

import com.bftcom.ice.common.maps.DataMap
import com.google.gson.Gson
import org.junit.Test

/**
 * Created by Щукин on 28.10.2017.
 */
open class GsonTests {
    @Test
    fun workWithGson() {

        val json = """
        {
    "header" : {
        "alerts" : [
            {
                "AlertID" : "2",
                "TSExpires" : null,
                "Target" : "1",
                "Text" : "woot",
                "Type" : "1"
            },
            {
                "AlertID" : "3",
                "TSExpires" : null,
                "Target" : "1",
                "Text" : "woot",
                "Type" : "1"
            }
        ],
        "session" : "0bc8d0835f93ac3ebbf11560b2c5be9a"
    },
    "result" : "4be26bc400d3c"
}

    """.trimIndent()

        val result = Gson().fromJson(json, Map::class.java)
        println(result)
    }

    @Test
    fun workWithGson2() {

        val json = """
        {
            n : "name1",
             map: {
                "key1":{
                    "n":"xxx"
                },
                "key2":{
                    "n":"xxx"
                }

            }
        }
    """.trimIndent()

        val result = Gson().fromJson(json, Foo::class.java);
        println(result.name)
        println(result.map)

        val dm = DataMap("Test", 1L)
        dm["bbbb"] = "zzzz"
        dm["xxx"] = DataMap("Person", 2L)
        dm("xxx")!!["сссс"]  = "ага"
       /// dm("xxx")["parent"]  = dm //todo: а это не умеет gson сериализовать
        val s =  Gson().toJson(dm)

        println(s)
    }


    class Foo {
        var name: String = "";
        val map: Map<String, Bar> = mutableMapOf();
    }

    class Bar {
        var name: String = "";
        override fun toString(): String {
            return "Bar(n='$name')"
        }


    }
}