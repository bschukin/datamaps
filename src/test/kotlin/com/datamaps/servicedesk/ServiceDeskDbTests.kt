package com.datamaps.servicedesk

import com.datamaps.*
import com.datamaps.mappings.AsIsNameMappingsStrategy
import com.datamaps.mappings.NameMappingsStrategy
import com.datamaps.maps.*
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
import org.testng.Assert
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

    fun notExists(p: projection): Boolean {
        return dataService.find(p) == null
    }

    fun find_(dp: DataProjection): DataMap {
        return dataService.find_(dp)
    }

    @Test
            //тест на создание SLA и его поиск и выдачу
    fun testSLA1() {

        insertSLA()

        //1) по организации - показать все имеющиеся SLA
        //списком:  организация, контракт, продукт, услуга, SLA
        //способ 1: от организации
        val res = dataService.findAll(on(ORG)
                .with(+ORG.contracts().contract().number,
                       + ORG.contracts().contract().products().product().name,
                       + ORG.contracts().contract().products().slas().service().name,
                       + ORG.contracts().contract().products().slas().sla,
                       + ORG.name)
                .filter {  -ORG.name eq "ЗАО БИС"}
        )

        val s = StringBuilder()
        res.forEach {
            s.append("${it[ORG.name]} " +
                    "| ${it[ORG.contracts[0].contract().number]}"+
                    "| ${it[ORG.contracts[0].contract().products[0].product().name]}"+
                    "| ${it[ORG.contracts[0].contract().products[0].slas[0].service().name]}" +
                    "| ${it[ORG.contracts[0].contract().products[0].slas[0].sla]}")
        }
        Assert.assertEquals("ЗАО БИС | 666| QDP| Consulting| 600", s.trim())
    }

    @Test
            //тест на создание SLA и его поиск и выдачу
    fun testSLA() {

        insertSLA()

        //1) по организации - показать все имеющиеся SLA
        //списком:  организация, контракт, продукт, услуга, SLA
        //способ 1: от организации
        val res = dataService.findAll(on(ORG).full()
                .with {
                    slice(ORG.contracts)
                            .with {
                                slice(ContractOrg.contract)
                                        .fields(Contract.number)
                                        .with {
                                            slice(Contract.products).full()
                                                    .with { slice(ContractProduct.slas).withRefs() }
                                        }
                            }
                }
                .where("{{name}} = 'ЗАО БИС'")
        )


        val s = StringBuilder()
        res.forEach {
            s.append("${it[ORG.name]} " +
                    "| ${it[ORG.contracts][0][ContractOrg.contract][Contract.number]}" +
                    "| ${it[ORG.contracts][0][ContractOrg.contract][Contract.products][0][ContractProduct.product][Product.name]}" +
                    "| ${it[ORG.contracts][0][ContractOrg.contract][Contract.products][0][ContractProduct.slas][0][SLA.service][Service.name]}" +
                    "| ${it[ORG.contracts][0][ContractOrg.contract][Contract.products][0][ContractProduct.slas][0][SLA.sla]}")
        }
        Assert.assertEquals("ЗАО БИС | 666| QDP| Consulting| 600", s.trim())

        //второй вариант: от SLA
        val res2 = dataService
                .findAll(on(SLA).full()
                        .with {
                            slice(SLA.contractProduct)
                                    .with {
                                        slice(ContractProduct.product)
                                                .field("name")
                                    }
                        }
                        .with {
                            slice(SLA.contractOrg).with {
                                slice(ContractOrg.organisation).field(ORG.name)
                            }
                        }
                        .where("{{contractOrg.organisation.name}} = 'ЗАО БИС'")
                )

        println(dataService.toJson(res2[0]))
        val s2 = StringBuilder()
        res2.forEach {
            s2.append("${it[SLA.contractOrg().organisation().name]}  " +
                    "| ${it[SLA.contractProduct().contract().number]}" +
                    "| ${it[SLA.contractProduct().product().name]}" +
                    "| ${it[SLA.service().name]}" +
                    "| ${it[SLA.sla]}")
        }
        println(s2)
        Assert.assertEquals("ЗАО БИС | 666| QDP| Consulting| 600", s.trim())
    }

    fun insertSLA() {
        //справочники
        insertPriorities()
        insertServices()

        insertOrg() //   tz[ORG.name] = "ЗАО БИС",  tz[ORG.bftSubdivision] = on(BftSubdivision)("{{name}} = 'DE'")
        insertContracts()// tz[CTR.number] = "666" tz[CTR.bftSubdivision] = on(BftSubdivision).where("{{name}} = 'DE'"))
        val co = insertContractOrg() // on(Contract).where("{{number}} = '666'"), on(ORG).where("{{name}} = 'ЗАО БИС'"))

        //вставляем продукт и пару модулей
        insertProducts() //p1[name] = "QDP". ,  m[Module.name] = "bpm",  m[Module.name] = "etl"
        //связываем контракт (#666), продукт(QDP) и модуль (etl)
        val contrProduct = insertContractProduct()

        //создаем SLA
        with(SLA)
        {
            val sla = new()
            sla[contractOrg] = co //сопоставляем с организацией-получателеем услуг по контракту
            sla[contractProduct] = contrProduct //сопосатвляем с купленным продуктом
            sla[priority] = find_(Priority.filter { f(Priority.name) eq "High" }) //определяем приоритет
            sla[service] = find_(Service.filter { f(Service.name) eq "Consulting" })//ставим услугу
            sla[this.sla] = 600 //секунды
        }

        dataService.flush()


    }

    @Test
    fun testbftSubdivision() {

        val dm = dataService.getDataMapping("BftSubdivision")
        dm.print()

        insertBftSubdivions()

        val sub1 = find_(on(BftSubdivision).where("{{name}} = 'DE'"))
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

    @Test
    fun testInsertModules() {
        insertProducts()

        val p = find_(on(Product)
                .withCollections()
                .filter { f(Product.name) eq "QDP" })

        assertNotNull(p[Product.modules].find { it[Module.name] == "bpm" })
        assertNotNull(p[Product.modules].find { it[Module.name] == "etl" })
    }

    fun insertModules() {

        val p = find_(on(Product).filter { f(Product.name) eq "QDP" })

        if (p[Product.modules].find { it[Module.name] == "bpm" } == null) {
            val m = Module.new()
            m[Module.name] = "bpm"
            m[Module.notActive] = false

            p[Product.modules].add(m)
        }

        if (p[Product.modules].find { it[Module.name] == "etl" } == null) {
            val m = Module.new()
            m[Module.name] = "etl"
            m[Module.notActive] = false
            p[Product.modules].add(m)
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


    @Test()
    fun testOrganisation() {

        insertOrg()
        val org = find_(on(ORG).where("{{name}} = 'ЗАО БИС'"))
        print(org[ORG.name])
    }

    fun insertOrg(): DataMap {
        insertBftSubdivions()

        val tz = DataMap(ORG.entity)
        tz[ORG.name] = "ЗАО БИС"
        tz[ORG.bftSubdivision] = find_(on(BftSubdivision).where("{{name}} = 'DE'"))

        dataService.flush()
        return tz
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

            val user2 = find_(on(OrgUser).filter { f(name) eq "Boris" })
            println(user2)
        }

    }

    @Test(invocationCount = 1)
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
        val ctr = find_(on(Contract).where("{{number}} = '666'"))
        assertNotNull(ctr)
        println(ctr[CTR.number])
    }

    @Test
    fun testContractOrg() {
        insertContracts()
        insertOrg()
        val dm1 = insertContractOrg()

        //пример сохранения связи через сохранение самой перевязочной сущности
        //кайфон
        val ctr = find_(on(Contract)
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

        val org = find_(on(ORG).full().where("{{name}} = 'ЗАО БИС'"))
        assertEquals(org[ORG.contracts].size, 1)


        println(org)

        //удалим связь через коллекцию
        ctr[CTR.orgs].remove(dm1)
        dataService.flush()
        assertEquals(ctr[CTR.orgs].size, 0)

        //но увы
        assertEquals(org[ORG.contracts].size, 1)
    }

    fun insertContractOrg(): DataMap {
        val ctr = find_(on(Contract).where("{{number}} = '666'"))
        val org = find_(on(ORG).where("{{name}} = 'ЗАО БИС'"))

        val dm1 = DataMap(ContractOrg)
        dm1[ContractOrg.contract] = ctr
        dm1[ContractOrg.organisation] = org

        dataService.flush()
        return dm1
    }

    fun insertContracts() {
        insertBftSubdivions()

        val tz = DataMap(Contract)
        tz[CTR.number] = "666"
        tz[CTR.bftSubdivision] = find_(on(BftSubdivision).where("{{name}} = 'DE'"))

        dataService.flush()
    }

    @Test
    fun testContractProducts() {
        insertContracts()
        insertProducts()

        insertContractProduct()

        dataService.flush()

        //достаем Contract'ы с купленными продуктами и модулями
        val ctr = find_(on(Contract)
                .full()
                .with {
                    slice(Contract.products).full()
                            .with {
                                slice(ContractProduct.modules)
                                        .full()
                            }
                }
                .where("{{number}} = '666'")
        )

        println(ctr)

        assertEquals(ctr[CTR.products].size, 1)
        assertEquals(ctr[CTR.products][0][ContractProduct.modules].size, 1)


    }

    private fun insertContractProduct(): DataMap {
        //берем контракт
        val ctr = find_(on(Contract).where("{{number}} = '666'"))

        //берем продукт с модулями
        val prd = find_(on(Product).full().where("{{name}} = 'QDP'"))

        //связываем контракт с продуктом
        val contractProduct = ContractProduct.new()
        contractProduct[ContractProduct.contract] = ctr
        contractProduct[ContractProduct.product] = prd

        val contractProductModule = ContractProductModule.new()
        contractProductModule[CPM.contractProduct] = contractProduct
        contractProductModule[CPM.module] = prd[Product.modules].first { it[Module.name] == "etl" }

        return contractProduct
    }

    @Test
    fun testProduct() {
        insertProducts()

        val p = find_(Product.on()
                .full()
                .filter { f(Product.name) eq "QDP" }
        )
        assertNotNull(p)
    }

    fun insertProducts() {
        with(Product)
        {
            if (notExists(filter { f(name) eq "QDP" })) {
                val p1 = new()
                p1[name] = "QDP"
            }

            if (notExists(filter { f(name) eq "AZK" })) {
                val p1 = new()
                p1[name] = "AZK"
            }
        }
        dataService.flush()

        insertModules()
    }

    @Test
    fun testPriorities() {
        insertPriorities()

        val p = find_(Priority.filter { f(Priority.name) eq "High" }.full())
        assertNotNull(p)

    }

    fun insertPriorities() {
        with(Priority)
        {
            if (notExists(filter { f(name) eq "High" })) {
                val s1 = new()
                s1[name] = "High"
            }

            if (notExists(filter { f(name) eq "Low" })) {
                val s1 = new()
                s1[name] = "Low"
            }
        }
        dataService.flush()

    }

    @Test
    fun testService() {
        insertServices()

        val p = find_(Service.filter { f(Service.name) eq "Consulting" }.full())
        assertNotNull(p)

    }

    fun insertServices() {
        with(Service)
        {
            if (notExists(filter { f(name) eq "Consulting" })) {
                val s1 = new()
                s1[name] = "Consulting"
            }

            if (notExists(filter { f(name) eq "Management" })) {
                val s1 = new()
                s1[name] = "Management"
            }
        }
        dataService.flush()
    }


}

