package com.datamaps.service

import com.datamaps.maps.DataMap
import org.testng.Assert

import java.util.Collections

/**
 * Created by Щукин on 31.10.2017.
 */
class DataServiceTests {


    internal var dataService: DataService? = null

    //table A (id, name, caption, b_id(not null))
    //table C (id, xxx, a_id(not_null))

    //table AS (id, a_id, s_id)
    //table S (id, qqq)
    fun test1() {
        val a = dataService!!.create("A")
        Assert.assertNotNull(a.id)

        val b = dataService!!["B", 7777L]

        a["name"] = "boris"
        a["caption"] = "schukin"
        a["b"] = b //N-1

        val c = DataMap("C", 666L)
        c["xxx"] = "xxx"

        a.list("slaves").add(c)//1-N

        dataService!!.flush()
        //insert  into A(id, caption, name, b_id) values ...;
        //insert  into C(id, a_id, xxx values) ...;

        val s = dataService!!.create("S")
        a.list("mmList").add(s)//N-N
        dataService!!.flush()

        //insert  into S(id, qqq) values ...;
        //insert into AS(id, a_id, qqq_id) values ....
    }

    fun testLoad() {
        val a = dataService!!["A", 666, " {" +
                "    slaves\n" +
                "    {" +
                "      xxx" +
                "    }\n" +
                "  }"]

        val aas = dataService!!.find(
                " A {" +
                        "    bid: :b" +
                        "    slaves\n" +
                        "  }", Collections.singletonMap<String, Any>("b", 777L))


    }

    internal interface DataService {
        fun create(entity: String): DataMap

        operator fun get(entity: String, id: Any): DataMap

        operator fun get(entity: String, id: Any, graphQl: String): DataMap

        fun find(graphQL: String, params: Map<String, Any>): List<DataMap>


        fun flush()
    }

}
