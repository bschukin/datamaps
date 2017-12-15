package com.datamaps.servicedesk

import com.datamaps.mappings.AsIsNameMappingsStrategy
import com.datamaps.mappings.NameMappingsStrategy
import com.datamaps.maps.DataMap
import com.datamaps.maps.f
import com.datamaps.maps.on
import com.datamaps.maps.slice
import com.datamaps.services.DataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest(
        classes = arrayOf(ServiceDeskDbTests.TestConfig::class),
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration("classpath:servicedesk-app-context.xml", inheritLocations = false)
@ActiveProfiles("postgresql")
@Profile("postgresql")
@EnableAutoConfiguration
class ServiceDeskDbTests : AbstractTransactionalTestNGSpringContextTests() {

    @Autowired
    lateinit var dataService: DataService

    @Configuration
    @EnableAutoConfiguration
    @SpringBootApplication(scanBasePackages = ["com.datamaps"])
    class TestConfig {
        @Bean
        fun nameMappingsStrategy(): NameMappingsStrategy {
            return AsIsNameMappingsStrategy()
        }
    }

    @Test
    fun testbftSubdivision() {

        val dm = dataService.getDataMapping("BftSubdivision")
        dm.print()

        insertBftSubdivions()

        val sub1 = dataService.find_(on(BftSubdivision).where("{{name}} = 'DE'"))
        assertNotNull(sub1)
    }

    fun insertBftSubdivions() {

        if (dataService.find(on(BftSubdivision).where("{{name}} = 'DVP' ")) == null) {
            val tz = DataMap(BftSubdivision)
            tz[BftSubdivision.name] = "DVP" //департамент выскотехологичного производства
        }

        if (dataService.find(on(BftSubdivision).where("{{name}} = 'DE' ")) == null) {
            val tz2 = DataMap(BftSubdivision)
            tz2[BftSubdivision.name] = "DE"//департамент эксплуатации
        }

        dataService.flush()
    }

    @Test(invocationCount = 1)
    fun workTimeZone() {

        val dm = dataService.getDataMapping("TimeZone")
        dm.print()

        val tz = DataMap("TimeZone")
        tz["name"] = "Moscow"

        dataService.flush()
        println(tz.id)
    }

    fun insertTimeZones() {
        val tz = DataMap(TimeZone.entity)
        tz[TimeZone.name] = "Moscow"

        val tz2 = DataMap(TimeZone.entity)
        tz2[TimeZone.name] = "NY"

        dataService.flush()
    }


    @Test
    fun testOrganisation() {

        insertOrgs()
        val org = dataService.find_(on(ORG).where("{{name}} = 'ЗАО БИС'"))
        print(org[ORG.name])
    }

    fun insertOrgs() {
        insertBftSubdivions()

        val tz = DataMap(ORG.entity)
        tz[ORG.name] = "ЗАО БИС"
        tz[ORG.bftSubdivision] = dataService.find_(on(BftSubdivision).where("{{name}} = 'DE'"))

        dataService.flush()
    }

    @Test
    fun testOrgTree() {
        val dm2 = dataService.getDataMapping("Organisation")
        dm2.print()
    }

    @Test
    fun testOrgUser() {

        val dm = dataService.getDataMapping("OrgUser")
        dm.print()

        with(OrgUser)
        {
            val user = DataMap(entity)
            user[name] = "Boris"
            user[organisation] = dataService.find(on(ORG).where("{{name}} = 'ЗАО БИС'"))

            dataService.flush()

            val user2 = dataService.find_(on(OrgUser).filter { f(name) eq "Boris" })
            println(user2)
        }

    }

    @Test(invocationCount = 0)
    fun testOrgInfo() {

        insertTimeZones()

        val dm = dataService.getDataMapping("Organisation")
        dm.print()

        val org = DataMap(ORG.entity)
        org[ORG.name] = "БИС"
        org[ORG.fullName] = "ЗАО БИС"
        org[ORG.INN] = "123456789101"

        dataService.flush()

        val org2 = dataService.find(on(ORG)
                .full().filter { f("inn") eq "123456789101" })!!

        org2["legalAddress"] = "Москва, Лефортово, все дела"
        org2["workTimeStart"] = 12
        org2[ORG.timeZone] = dataService.find(on(TimeZone).where("{{name}}='NY'"))
        dataService.flush()

        val org3 = dataService.find(on("Organisation")
                .full().filter { f("inn") eq "123456789101" })!!

        println(org3)
    }


    @Test
    fun testContract() {

        val dm = dataService.getDataMapping("Contract")
        dm.print()

        insertContracts()
        val ctr = dataService.find_(on(Contract).where("{{number}} = '666'"))
        assertNotNull(ctr)
        println(ctr[CTR.number])
    }

    @Test
    fun testContractOrg() {
        insertContracts()
        insertOrgs()

        var ctr = dataService.find_(on(Contract).where("{{number}} = '666'"))
        var org = dataService.find_(on(ORG).where("{{name}} = 'ЗАО БИС'"))

        val dm1 = DataMap(ContractOrg)
        dm1[ContractOrg.contract] = ctr
        dm1[ContractOrg.organisation] = org

        dataService.flush()

        //пример сохранения связи через сохранение самой перевязочной сущности
        //кайфон
        ctr = dataService.find_(on(Contract)
                .full()
                .with {
                    slice(Contract.orgs).withRefs()
                            .with {
                                slice(ContractOrg.organisation)
                                        .fields(ORG.name, ORG.INN)
                            }
                }
                .where("{{number}} = '666'")
        )
        assertEquals(ctr[CTR.orgs].size, 1)
        println(ctr)

        org = dataService.find_(on(ORG).full().where("{{name}} = 'ЗАО БИС'"))
        assertEquals(org[ORG.contracts].size, 1)


        println(org)

        //удалим связь
        ctr[CTR.orgs].remove(dm1)
        dataService.flush()
        assertEquals(ctr[CTR.orgs].size, 0)
        //но увы
        assertEquals(org[ORG.contracts].size, 1)
    }

    fun insertContracts() {
        insertBftSubdivions()

        val tz = DataMap(Contract)
        tz[CTR.number] = "666"
        tz[CTR.bftSubdivision] = dataService.find_(on(BftSubdivision).where("{{name}} = 'DE'"))

        dataService.flush()
    }


}

