package com.datamaps.servicedesk

import com.datamaps.BaseSpringTests
import com.datamaps.KotlinDemoApplication
import com.datamaps.mappings.f
import com.datamaps.mappings.on
import com.datamaps.maps.DataMap
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testng.annotations.Test

@SpringBootTest(
        classes = arrayOf(KotlinDemoApplication::class),
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration("classpath:servicedesk-app-context.xml", inheritLocations = false)
@ActiveProfiles("postgresql")
class ServiceDeskDbTests : BaseSpringTests() {

    @Test
    fun testOrgInfo() {

        val dm =dataService.getDataMapping("Organisation")
        dm.print()

        val org = DataMap("Organisation")
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

