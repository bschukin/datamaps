package com.datamaps.mappings

import com.google.gson.Gson
import org.testng.annotations.Test

/**
 * Created by Щукин on 28.10.2017.
 */

class DataMappingReadTests {
    @Test(invocationCount = 0)
    fun readMappingsFromString() {

        var mapping = """
       {
            "name" : "AAA",
            "table" :   AAA_,
            "id-column": "name";
            "fields":[
                {
                    "name": "caption",
                    group: "default",
                    type:"string"
                },
                {
                    "name": "description",
                    sqlcolumn: "desc_",
                    type:"string"
                }
                ,
                {
                    name: "bbb",
                    m-1:{to:"BBB", join-column:"bbbId"},
                    1-m:{to:"CCCC", their-join-column:"aaaID"},
                    m-m:{to:"ZZZZ", join-table:"AAA_ZZZ", our-join-column:"AAA_ID", their-join-column:"aaaID"}
                }
            ],
            groups:
            [   {
                    name:"specific",
                    fields:
                    [
                        {
                        name: "ccc"
                        }
                    ]
                }
            ]
        }
        """

        var result = Gson().fromJson(mapping, DataMapping::class.java)
        println(result.name)
        println(result.table)
        result.fields.forEach { df ->
            run {
               /* println("===")
                println(df.parentField)
                //println(df.group)
                println(df.type)
                println(df.sqlcolumn)
                println(df.manyToOne?.joinColumn)
                println(df.oneToMany?.theirJoinColumn)
                println(df.manyToMany?.joinTable)*/
            }
        }
    }
}