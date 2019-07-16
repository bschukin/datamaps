package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.MFS
import com.bftcom.ice.datamaps.Temporal
import com.bftcom.ice.datamaps.f
import com.bftcom.ice.datamaps.utils.Date
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.datamaps.impl.util.printAsJson
import org.junit.Test

/**
 * Created by Щукин on 03.11.2017.
 */
open class TemporalVersionsTests : BaseSpringTests() {

    object Nation : MFS<Nation>("Nation") {

        val id = Field.guid()
        val recordId = Field.guid("recordId") { defaultValue = { it[id] } }
        val name = Field.stringNN("name")
        val startDate = Field.dateNN("startDate")
        val endDate = Field.dateNN("endDate")
        val nationRegistrys = Field.list("nationRegistrys", NationRegistry)
        val capitals = Field.list("capitals", Capital)

        val capitalsRecords = Field.list("capitalsRecords", Capital) {
            synthetic = true
            thisJoinColumn = "record_id"
            thatJoinColumn = "nation_record_id"
        }

        init {
            options.add(Temporal(startDate, endDate))
        }
    }


    object NationRegistry : MFS<NationRegistry>("NationRegistry") {

        val id = Field.guid()
        val recordId = Field.guid("recordId") { defaultValue = { it[id] } }
        val name = Field.stringNN("name")
        val startDate = Field.dateNN("startDate")
        val endDate = Field.dateNN("endDate")

        val nation = Field.reference("nation", Nation) {
            synthetic = true
            thisJoinColumn = "nation_record_id"
            thatJoinColumn = "record_id"
        }

        init {
            options.add(Temporal(Nation.startDate, Nation.endDate))
        }
    }

    object Capital : MFS<Capital>("Capital") {

        val id = Field.guid()
        val recordId = Field.guid("recordId") { defaultValue = { it[id] } }
        val name = Field.stringNN("name")
        val startDate = Field.dateNN("startDate")
        val endDate = Field.dateNN("endDate")

        init {
            options.add(Temporal(Nation.startDate, Nation.endDate))
        }
    }


    /****тест на связь M-1 с темпоральной версионнированной сущностью */
    @Test
    fun testM1TemporalVersionsSelect() {
        insertRecords()

        //получаем реестр наций  на дату  1.1.1800
        val r2 = dataService.find_(
                NationRegistry.slice {
                    onDate(Date(1800, 1, 1))
                    +nation
                })

        //в этот момент Русь была Российской империей
        assertNotNull(r2[{ nation }])
        assertTrue(r2[{ nation().name }] == "Russian Imperia")
        r2.printAsJson()

        //получаем реестр наций  на дату  1.1.0100
        val r = dataService.find_(
                NationRegistry.slice {
                    onDate(Date(1000, 1, 1))
                    +nation
                })

        //россии тогда еще не было.
        assertNull(r[{ nation }])
        r.printAsJson()

    }

    /****тест, который показывает, что если целевой  OnDate не указан
     * в темпорально-версионном референсе окажется выбран случайный объект*/
    @Test
    fun testM1NoTemporalVersionsSelect() {
        insertRecords()

        //поменяем порядок сортировки и увиидим что россиии  будут в референсах разные
        val r = dataService.find_(
                NationRegistry.slice {
                    +name
                    +nation
                }.order(f(NationRegistry.nation().startDate).desc()))
        assertTrue(r[{ NationRegistry.nation().name }] == "Russian Imperia")
        r.printAsJson()

        val r2 = dataService.find_(
                NationRegistry.slice {
                    +name
                    +nation
                }.order(f(NationRegistry.nation().startDate).asc_()))

        r2.printAsJson()

        assertTrue(r2[{ NationRegistry.nation().name }] == "Russian Federation")

    }

    @Test

    fun test1NTemporalVersionsSelect() {
        insertRecords()

        val r = dataService.find_(
                Nation.slice {
                    onDate(Date(1900, 1, 1))
                    +capitalsRecords
                }.order(f(Nation.capitalsRecords().startDate).asc_()))

        r.printAsJson()
        assertTrue(r[{ capitalsRecords }].size == 2)
        assertTrue(r[{ capitalsRecords }][0][{ name }] == "Москва")
        assertTrue(r[{ capitalsRecords }][1][{ name }] == "SaintPeterburg")

        val r2 = dataService.find_(
                Nation.slice {
                    onDate(Date(1960, 1, 1))
                    +capitalsRecords
                }.order(f(Nation.capitalsRecords().startDate).asc_()))

        r2.printAsJson()
        assertTrue(r2[{ capitalsRecords }].size == 2)
        assertTrue(r2[{ capitalsRecords }][0][{ name }] == "Москва")
        assertTrue(r2[{ capitalsRecords }][1][{ name }] == "Ленинград")

    }

    /**Тест на темпоральные апгрейды**/
    @Test
    fun test1NTemporalVersionsAsUpgrade() {
        insertRecords()
        val nation = dataService.find_(
                Nation.slice {
                    onDate(Date(2000, 1, 1))
                })
        nation.printAsJson()
        assertTrue(nation[{ capitalsRecords }].size == 0)

        dataService.upgrade(
                listOf(nation),
                Nation.slice {
                    onDate(Date(2000, 1, 1))
                    +capitalsRecords
                }.order(f(Nation.capitalsRecords().startDate).asc_())
        )

        nation.printAsJson()
        assertTrue(nation[{ capitalsRecords }].size == 2)
        assertTrue(nation[{ capitalsRecords }][0][{ name }] == "Москва")
        assertTrue(nation[{ capitalsRecords }][1][{ name }] == "Питер")

    }

    /*Тест в котором есть оба вида темпорально-версионных связей */
    @Test
    fun testM1and1NTemporalVersionsSelect() {
        insertRecords()

        val r2 = dataService.find_(
                NationRegistry.slice {
                    onDate(Date(1915, 1, 1))
                    +name
                    nation {
                        scalars()
                        +capitalsRecords
                    }
                })

        assertNotNull(r2[{ nation }])
        assertTrue(r2[{ nation().name }] == "Russian Imperia")
        r2.printAsJson()
    }


    private fun insertRecords() {

        //актуальная запись
        val fed = Nation {
            it[name] = "Russian Federation"
            it[startDate] = Date(1991, 12, 25)
            it[endDate] = Date(2420, 1, 1)
        }

        //историческая запись 1
        val rus = Nation {
            it[name] = "Russian Imperia"
            it[recordId] = fed[recordId]
            it[startDate] = Date(1721, 11, 2)
            it[endDate] = Date(1917, 11, 7)
        }

        //историческая запись 2
        val soviet = Nation {
            it[name] = "Soviet Nation"
            it[recordId] = fed[recordId]
            it[startDate] = Date(1917, 11, 7)
            it[endDate] = Date(1991, 12, 25)
        }


        dataService.flush()

        val registry = NationRegistry {
            it[name] = "Регистрация РФ в ООН"
            it[startDate] = Date(1000, 1, 1)
            it[endDate] = Date(2050, 1, 1)
        }
        fed[{ nationRegistrys }].add(registry)
        dataService.flush()


        //единственая запись о москве
        val moscow = Capital {
            it[name] = "Москва"
            it[Nation.startDate] = Date(1100, 1, 2)
            it[Nation.endDate] = Date(2420, 1, 1)
        }

        val piter = Capital {
            it[name] = "SaintPeterburg"
            it[Nation.startDate] = Date(1721, 11, 2)
            it[Nation.endDate] = Date(1917, 11, 7)
        }

        val lening = Capital {
            it[name] = "Ленинград"
            it[Nation.startDate] = Date(1924, 1, 1)
            it[Nation.endDate] = Date(1991, 12, 25)
        }

        val piter2 = Capital {
            it[name] = "Питер"
            it[Nation.startDate] = Date(1991, 12, 25)
            it[Nation.endDate] = Date(2420, 1, 1)
        }

        fed[{ capitals }].addAll(listOf(moscow, piter, lening, piter2))
        dataService.flush()

    }


}