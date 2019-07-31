package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.core.util.printAsJson
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Created by Щукин on 03.11.2017.
 */
open class QueryBuilderFormulasTests : BaseSpringTests() {


    //базовый тест на
    @Test
    fun testSelectCOlumn01() {
        val dp = Projection("Gender")
                .formula("genderCaption", """
                    case when {{id}}=1 then 'Ж'
                         when {{id}}=2 then 'М'
                         else 'О' end
                """)
                .filter(f("genderCaption") eq "Ж")

        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        val e = dataService.findAll(dp)
        e.forEach {
            print(e)
        }
        Assert.assertEquals(e.size, 1)
        assertEqIgnoreCase(e[0]["genderCaption"], "Ж")
    }


    @Test
    fun testSelectColumn01WithWhere() {
        val dp = Projection("Gender")
                .formula("genderCaption", """
                    case when {{id}}=1 then 'Ж'
                         when {{id}}=2 then 'М'
                         else 'О' end
                """)
                .where("{{genderCaption}} = :param0")
                .param("param0", "Ж")

        val q = queryBuilder.createQueryByDataProjection(dp)

        println(q.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        val e = dataService.findAll(dp)
        e.forEach {
            print(e)
        }
        Assert.assertEquals(e.size, 1)
        assertEqIgnoreCase(e[0]["genderCaption"], "Ж")
    }

    @Test()
    fun testFormula02OnNestedEntity() {
        val dp = projection("Person")
                .with {
                    slice("gender")
                            .formula("genderCaption", """
                    case when {{id}}=1 then 'Ж'
                         when {{id}}=2 then 'М'
                         else 'О' end
                """)
                }
                .filter(f("gender.genderCaption") eq "Ж")

        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        val res = dataService.findAll(dp)
        res.forEach { r -> println(r) }

        assertEquals(res.size, 2)
        assertEquals(res[0]("gender")!!["genderCaption"], "Ж")
        assertEquals(res[1]("gender")!!["genderCaption"], "Ж")
        assertNotEquals(res[0].id, res[1].id)
    }

    @Test
    //тест показывает как затащить из запроса в карту константу
    fun testConstantField() {
        val dp = on(Person)
                .scalars().formula("entityDiscriminator", "'xxx'")

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp)

        dataService.findAll(dp).forEach {
            it.printAsJson()
        }
    }


    @Test
            /**
             * тест показывает как вытащить айдишник не как REF а как число
             * с помощью формулы
             * */
    fun testRefAsId() {
        val dp = on(Person)
                .scalars().formula("genderId", "GENDER_ID")

        //1 тест на  структуру по которой построится запрос
        val qr = QueryBuildContext()
        queryBuilder.buildMainQueryStructure(qr, dp)


        val q = queryBuilder.createQueryByDataProjection(dp)
        println(q.sql)
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t  PERSON1.\"ID\"  AS  ID1,  PERSON1.\"NAME\"  AS  NAME1,  PERSON1.\"EMAIL\"  AS  EMAIL1, " +
                "PERSON1.\"LAST_NAME\"  AS  LAST_NAME1, PERSON1.\"AGE\"  AS  AGE1," +
                " (GENDER_ID) AS  genderId\n" +
                "FROM PERSON  PERSON1 ")

        println(dataService.findAll(dp))
    }

    /***
     * тест на использование функции upper в фильтре
     */
    @Test
    fun testFunctionUpper() {

        val slice = Gender.filter {
            Upper(name) eq Upper("man")
        }

        val q = queryBuilder.createQueryByDataProjection(slice)
        println(q.sql)
        assertBodyEquals(q.sql, "SELECT \n" +
                "\t  GENDER1.\"ID\"  AS  ID1,  GENDER1.\"NAME\"  AS  NAME1,  GENDER1.\"IS_CLASSIC\"  AS  IS_CLASSIC1\n" +
                "FROM GENDER GENDER1 \n" +
                "WHERE UPPER(:param0) = UPPER(:param1) ")
    }
}