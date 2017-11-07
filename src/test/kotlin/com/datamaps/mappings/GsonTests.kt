package com.datamaps.mappings

import com.google.gson.Gson
import org.testng.annotations.Test

/**
 * Created by Щукин on 28.10.2017.
 */
class GsonTests {
    @Test
    fun workWithGson() {

        var json = """
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

        var result = Gson().fromJson(json, Map::class.java)
        println(result)
    }

    @Test
    fun workWithGson2() {

        var json = """
        {
            name : "name1",
             map: {
                "key1":{
                    "name":"xxx"
                },
                "key2":{
                    "name":"xxx"
                }

            }
        }
    """.trimIndent()

        var result = Gson().fromJson(json, Foo::class.java);
        println(result.name)
        println(result.map)
    }


    class Foo {
        var name: String = "";
        val map: Map<String, Bar> = mutableMapOf();
    }

    class Bar {
        var name: String = "";
        override fun toString(): String {
            return "Bar(name='$name')"
        }


    }
}