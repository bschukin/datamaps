package com.datamaps.mappings

import com.google.gson.Gson
import org.testng.annotations.Test

/**
 * Created by Щукин on 28.10.2017.
 */

class DataMappingReadTests {
    @Test
    fun readMappingsFromString() {

        var mapping = """
       {
            "name" : "AAA",
            "table" :   AAA_,
            "fields":[
                {
                    "field": "caption",
                    group: "default",
                    type:"string"
                },
                {
                    "field": "description",
                    sqlcolumn: "desc_",
                    type:"string"
                }
                ,
                {
                    field: "bbb",
                    m-1:{to:"BBB", join-column:"bbbId"},
                    1-m:{to:"CCCC", their-join-column:"aaaID"}
                }
            ]
        }
        """

        var result = Gson().fromJson(mapping, DataMapping::class.java)
        println(result.name)
        println(result.table)
        result.fields.forEach { df ->
            run {
                println("===")
                println(df.field)
                println(df.group)
                println(df.type)
                println(df.sqlcolumn)
                println(df.manyToOne?.joinColumn)
                println(df.oneToMany?.theirJoinColumn)
            }
        }
    }
}