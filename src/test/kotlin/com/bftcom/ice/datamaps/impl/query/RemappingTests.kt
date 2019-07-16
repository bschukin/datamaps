package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.DataMap
import com.bftcom.ice.datamaps.DynamicEntity
import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.MFS
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.assertBodyEquals
import com.bftcom.ice.IfSpringProfileActive
import com.bftcom.ice.datamaps.impl.util.printAsJson
import org.junit.Ignore
import org.junit.Test

/**
 * Created by Щукин on 03.11.2017.
 */
open class RemappingTests : BaseSpringTests() {

    object Remap01 : MFS<Remap01>("Remap01") {
        val name = Field.string("name")
        val firsts = Field.list("firsts", Remap01Child).apply { thatJoinColumn = "first_id" }
        val seconds = Field.list("seconds", Remap01Child).apply { thatJoinColumn = "second_id" }
    }

    object Remap01Child : MFS<Remap01>("Remap01Child"){
        val name = Field.string("name")
    }

    object Remap02 : MFS<Remap02>("Remap02") {
        val name = Field.string("name")
        val firsts = Field.list("firsts", Remap02Child).apply { thatJoinColumn = "first_id" }
        val seconds = Field.list("seconds", Remap02Child).apply { thatJoinColumn = "second_id" }
    }

    object Remap02Child : MFS<Remap01>("Remap02Child"){
        val name = Field.string("name")
    }


    object RemapStringToJson : MFS<RemapStringToJson>("RemapStringToJson") {
        val data = Field.jsonbd("data")
    }

    @Test
    fun testRemapCollectionsNames() {
        sqlStatistics.restart()

        with(Remap01)
        {
            val res = find_(Remap01.slice {
                withId(1)
                +name
                withCollections() //так, чтобы ниже убедиться что старых коллекций не осталось
            })
            assertTrue(res[firsts].size == 2)
            assertTrue(res[seconds].size == 1)
        }

        assertBodyEquals("""
            SELECT
	        REMAP011."ID"  AS  ID1,  REMAP01_CHILD1."ID"  AS  ID2,  REMAP01_CHILD1."NAME"  AS  NAME1,  REMAP01_CHILD2."ID"  AS  ID3,  REMAP01_CHILD2."NAME"  AS  NAME2,  REMAP011."NAME"  AS  NAME3
            FROM REMAP01 REMAP011
            LEFT JOIN REMAP01_CHILD  REMAP01_CHILD1 ON REMAP011."ID"=REMAP01_CHILD1."FIRST_ID"
            LEFT JOIN REMAP01_CHILD  REMAP01_CHILD2 ON REMAP011."ID"=REMAP01_CHILD2."SECOND_ID"
            WHERE REMAP011.ID = :_id1
        """,
                sqlStatistics.lastQuery())

    }


    @Test
    fun testMapCollectionWithNoCascadeInConstraint() {
        sqlStatistics.restart()

        with(Remap02)
        {
            val res = find_(Remap02.slice {
                withId(1)
                +name
                withCollections() //так, чтобы ниже убедиться что старых коллекций не осталось
            })
            assertTrue(res[firsts].size == 2)
            assertTrue(res[seconds].size == 1)

            res.printAsJson()
        }
        println(sqlStatistics.lastQuery())

        assertBodyEquals("""
           SELECT
	           REMAP021."ID"  AS  ID1,  REMAP02_CHILD1."ID"  AS  ID2,  REMAP02_CHILD1."NAME"  AS  NAME1,
            REMAP02_CHILD2."ID"  AS  ID3,
            REMAP02_CHILD2."NAME"  AS  NAME2,
            REMAP021."NAME"  AS  NAME3
              FROM REMAP02 REMAP021
             LEFT JOIN REMAP02_CHILD  REMAP02_CHILD1 ON REMAP021."ID"=REMAP02_CHILD1."FIRST_ID"
             LEFT JOIN REMAP02_CHILD  REMAP02_CHILD2 ON REMAP021."ID"=REMAP02_CHILD2."SECOND_ID"
            WHERE REMAP021.ID = :_id1
        """,
                sqlStatistics.lastQuery())


    }

    @Test
    fun testMapStringToJsonDataMap() {
        sqlStatistics.restart()

        val map = RemapStringToJson {
            it[data] = DynamicEntity.create {
                it["foo"] = "bar"
                it["hello"] = "world"
            }
        }
        dataService.flush()

        val map2 = dataService.find_(RemapStringToJson
                .slice { withId(map.id).withBlobs() }
        )
        assertTrue(map2[{ data }] is DataMap)
        assertTrue(map2[{ data }]!!["foo"] == "bar")
        assertTrue(map2[{ data }]!!["hello"] == "world")

        println(sqlStatistics.lastInsert())


    }

    @Test
    @Ignore//TODO
    fun testRemapTableName() {


    }


    /**
     * Тест на то, что при испрользовании фиелдсета,
     * в группах полей (withRefs(), withCollections(), full() ) будут использоваться только поля,
     * явно описанные в фиелдсете.
     * Если же поле не описано в фиелдсете, то в запросе его можно будет использовать
     * только при явном указании этого поля в слайсе
     **
     * в сущности Imp01 неуказаны поля ссылки и поля имеющиеся в БД:
     *      скаляр bar, ссылка imp05, колллекция imp03s.
     *
     *   Задача теста проверить, что данные поля не будут участвовать в запросе.
     *   (до тех пор пока мы их явно в нем не укажем)
     */
    object Imp01 : MFS<Imp01>("Imp01") {
        val name = Field.string("name")
        val foo = Field.string("foo")
        val imp04 = Field.string("imp04")
        val imp02s = Field.list("imp02s", Imp02)
    }

    object Imp02 : MFS<Imp02>("Imp02") {
        val name = Field.string("name")
    }

    object Imp04 : MFS<Imp02>("Imp04") {
        val name = Field.string("name")
    }
    @Test
    fun testUseOnlyImplicitlySpecifiedFieldsInSliceGroups() {
        sqlStatistics.restart()



        ///часть 1. если используются группы, то выбирать только то что в филедсете

        //1.1 full group
        dataService.findAll(Imp01.on().full())

        var sql  = sqlStatistics.lastQuery()
        println(sql)

        assertBodyEquals(sql, """
                    SELECT
	                    imp011."id"  AS  id1,  imp011."name"  AS  name1,
                        imp011."foo"  AS  foo1,
                        imp041."id"  AS  id2,  imp041."name"  AS  name2,
                        imp021."id"  AS  id3,  imp021."name"  AS  name3
                    FROM imp01 imp011
                        LEFT JOIN imp04  imp041 ON imp011."imp04_id"=imp041."id"
                        LEFT JOIN imp02  imp021 ON imp011."id"=imp021."imp01_id"
            """)

        //1.2 collection group
        dataService.findAll(Imp01.on().scalars().withRefs().withCollections())

        sql  = sqlStatistics.lastQuery()
        println(sql)

        assertBodyEquals(sql, """
                    SELECT
	                    imp011."id"  AS  id1,  imp011."name"  AS  name1,
                        imp011."foo"  AS  foo1,
                        imp041."id"  AS  id2,  imp041."name"  AS  name2,
                        imp021."id"  AS  id3,  imp021."name"  AS  name3
                    FROM imp01 imp011
                        LEFT JOIN imp04  imp041 ON imp011."imp04_id"=imp041."id"
                        LEFT JOIN imp02  imp021 ON imp011."id"=imp021."imp01_id"
            """)

        ///часть 2. если в запросе явно указано поле не из фиелдсета - ОК
        dataService.findAll(Imp01.slice {
            full()
            field("bar") //ссылка не неуказанный в фиелдсете скаляр
            field("imp05") //ссылка не неуказанное в фиелдсете ссылку
            field("imp03s") //ссылка не неуказанную в фиелдсете  коллекцию
        })

        sql  = sqlStatistics.lastQuery()
        println(sql)

        assertBodyEquals(sql, """
                    SELECT
                         imp011."id"  AS  id1,  imp011."name"  AS  name1,
                         imp011."foo"  AS  foo1,
                         imp041."id"  AS  id2,  imp041."name"  AS  name2,
                         imp021."id"  AS  id3,  imp021."name"  AS  name3,
                         imp011."bar"  AS  bar1,
                         imp051."id"  AS  id4,  imp051."name"  AS  name4,
                         imp031."id"  AS  id5,  imp031."name"  AS  name5
                    FROM imp01 imp011
                    LEFT JOIN imp04  imp041 ON imp011."imp04_id"=imp041."id"
                    LEFT JOIN imp02  imp021 ON imp011."id"=imp021."imp01_id"
                    LEFT JOIN imp05  imp051 ON imp011."imp05_id"=imp051."id"
                    LEFT JOIN imp03  imp031 ON imp011."id"=imp031."imp01_id"
            """)
    }


    object Plant : MFS<Plant>("Plant") {
        val id = Field.guid()
        val name = Field.stringNN("name")
        val type = Field.reference("type", PlantType)
    }

    object PlantType : MFS<PlantType>("PlantType") {
        val id = Field.guid()
        val name = Field.stringNN("name")
        val plants = Field.list("plants", Plant)
    }

    /**
     * Тестируем связь "многий к одному", при которой мы ссылаемся на поле таблицы, отличающееся от ID
     * (в частности, Plant.type ссылается на PlantType по полю name)
     */
    @Test//TODO: проверить возможность адаптиции для oracle
    @IfSpringProfileActive("postgresql", "firebird")
    fun testManyToOneReferenceLinkedOnOtherThanIdField() {

        val roza = dataService.find_(
                Plant.filter { type().name eq "flower"}.withRefs())
        assertTrue(roza[{ name}] == "roza")
        assertTrue(roza[{ type().name}] == "flower")

    }

    /**
     * Тестируем связь "один ко многим", при которой дочерняя таблица ссылаемся на родителя таблицы по полю, отличаюмущеся от ID
     * (в частности, Plant.type ссылается на PlantType по полю name)
     */
    @Test//TODO: проверить возможность адаптиции для oracle
    @IfSpringProfileActive("postgresql", "firebird")
    fun testOneToManyReferenceLinkedOnOtherThanIdField() {

        val treeType = dataService.find_(
                PlantType.filter { name eq "tree"}.withCollections())

        assertTrue(treeType[{ plants}].size == 2)

    }
}



