package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.f
import com.bftcom.ice.datamaps.not
import com.bftcom.ice.datamaps.value
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Person
import com.bftcom.ice.server.assertBodyEquals
import org.junit.Test

/**
 * Created by Щукин on 03.11.2017.
 */

open class QueryBuilderTextFilterTests : BaseSpringTests() {


    @Test
    fun testBuildEsQuery() {

        //SINGLE
        var dp = Person.on().field("name")
                .textFilter { value("Варшавский") }
        var q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1\n" +
                "FROM PERSON PERSON1" +
                "WHERE zdb('PERSON', ${q.qr.rootAlias}.ctid) ==> '\"Варшавский\"'", q.sql)


        //AND
        dp = Person.on().field("name")
                .textFilter { value("Кипелов") and value("Варшавский") }
        q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1\n" +
                "FROM PERSON PERSON1" +
                "WHERE zdb('PERSON', ${q.qr.rootAlias}.ctid) ==> '(\"Кипелов\" and \"Варшавский\")'", q.sql)

        //OR
        dp = Person.on().field("name")
                .textFilter { value("Кипелов") or value("Варшавский") }
        q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1\n" +
                "FROM PERSON PERSON1 " +
                "WHERE zdb('PERSON', ${q.qr.rootAlias}.ctid) ==> '(\"Кипелов\" or \"Варшавский\")'", q.sql)


        //NOT
        dp =Person.on().field("name")
                .textFilter { not(value("Варшавский")) }
        q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1\n" +
                "FROM PERSON PERSON1 " +
                "WHERE zdb('PERSON', ${q.qr.rootAlias}.ctid) ==> ' !\"Варшавский\"'", q.sql)


       //NOT AND
        dp = Person.on().field("name")
                .textFilter { not(value("Кипелов")) and value("Варшавский") }
        q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1\n" +
                "FROM PERSON PERSON1 \n" +
                "WHERE zdb('PERSON', ${q.qr.rootAlias}.ctid) ==> '(!\"Кипелов\" AND \"Варшавский\")'", q.sql)

        //FIELD EQ
        dp = Person.on().field("name")
                .textFilter { f("data.P5") eq value("Варшавский") }
        q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1\n" +
                "FROM PERSON PERSON1 \n" +
                "WHERE zdb('PERSON', ${q.qr.rootAlias}.ctid) ==> 'data.P5 = \"Варшавский\"'", q.sql)

        //FIELD LESS OR EQUAL
        dp = Person.on().field("name")
                .textFilter { f("data.P5") le value("Варшавский") }
        q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1\n" +
                "FROM PERSON PERSON1 \n" +
                "WHERE zdb('PERSON', ${q.qr.rootAlias}.ctid) ==> 'data.P5 <= \"Варшавский\"'", q.sql)

        //FIELD LESS
        dp = Person.on().field("name")
                .textFilter { f("data.P5") lt value("Варшавский") }
        q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1\n" +
                "FROM PERSON PERSON1 \n" +
                "WHERE zdb('PERSON', ${q.qr.rootAlias}.ctid) ==> 'data.P5 < \"Варшавский\"'", q.sql)


        //FIELD GREATER OR EQUAL
        dp = Person.on().field("name")
                .textFilter { f("data.P5") ge value("Варшавский") }
        q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1\n" +
                "FROM PERSON PERSON1 \n" +
                "WHERE zdb('PERSON', ${q.qr.rootAlias}.ctid) ==> 'data.P5 >= \"Варшавский\"'", q.sql)

        //FIELD GREATER
        dp = Person.on().field("name")
                .textFilter { f("data.P5") gt value("Варшавский") }
        q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1\n" +
                "FROM PERSON PERSON1 \n" +
                "WHERE zdb('PERSON', ${q.qr.rootAlias}.ctid) ==> 'data.P5 > \"Варшавский\"'", q.sql)

        //FIELD NOT EQUALS
        dp = Person.on().field("name")
                .textFilter { f("data.P5") neq value("Варшавский") }
        q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1\n" +
                "FROM PERSON PERSON1 \n" +
                "WHERE zdb('PERSON', ${q.qr.rootAlias}.ctid) ==> 'data.P5 <> \"Варшавский\"'", q.sql)


    }

    @Test
    fun testEsAndSqlQueryInOne() {
        val dp = Person.on().field("name")
                .textFilter { value("Кипелов") and value("Варшавский") }
                .filter( f("name") eq "John")

        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)
        assertBodyEquals("SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1\n" +
                "FROM PERSON PERSON1 \n" +
                "WHERE PERSON1.\"NAME\" = :param0 AND zdb('PERSON', ${q.qr.rootAlias}.ctid) ==> '(\"Кипелов\" AND \"Варшавский\")' ", q.sql)
    }

}