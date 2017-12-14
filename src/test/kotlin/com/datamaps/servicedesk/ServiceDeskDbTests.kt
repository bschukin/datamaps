package com.datamaps.servicedesk

import com.datamaps.mappings.AsIsNameMappingsStrategy
import com.datamaps.mappings.NameMappingsStrategy
import com.datamaps.maps.f
import com.datamaps.maps.on
import com.datamaps.maps.DataMap
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

    @Test(invocationCount = 1)
    fun workTimeZone() {

        val dm =dataService.getDataMapping("TimeZone")
        dm.print()

        val tz = DataMap("TimeZone")
        tz["name"] = "Moscow"

        dataService.flush()
        println(tz.id)
    }


    @Test
    fun testOrgInfo() {

        val dm =dataService.getDataMapping("Organisation")
        dm.print()

        val org = DataMap("Organisation")
        org["name"] = "БИС"
        org["fullName"] = "ЗАО БИС"
        org["inn"] = "123456789101"

        dataService.flush()

        val org2 = dataService.find(on("Organisation")
                .full().filter { f("inn") eq "123456789101" })!!

        org2["legalAddress"]  = "Москва, Лефортово, все дела"
        org2["workTimeStart"]  = 12
        dataService.flush()

        val org3 = dataService.find(on("Organisation")
                .full().filter { f("inn") eq "123456789101" })!!

        println(org3)
    }



}

