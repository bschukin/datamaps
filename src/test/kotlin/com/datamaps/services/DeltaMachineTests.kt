package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.mappings.on
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.testng.Assert
import org.testng.annotations.Test


/**
 * Created by Щукин on 03.11.2017.
 */
@Transactional
class DeltaMachineTests : BaseSpringTests() {

    @Autowired
    lateinit var deltaMachine: DeltaMachine

    @Test
    fun testSimpleUpdateQueryies() {
        val gender = dataService.get("JiraGender", 2L)!!
        println(gender)

        gender["gender"] = "men"
        gender["gender"] = "men2"

        val list = deltaMachine.createUpdateStatements(DeltaStore.collectBuckets())

        list.forEach {
            println(it.first)
        }
        assertBodyEquals(list[0].first,
                "UPDATE JIRA_GENDER SET GENDER = :_gender \n" +
                        " WHERE ID = :_ID")
    }

    @Test
    fun testSimpleUpdates() {
        val gender = dataService.get("JiraGender", 2L)!!
        println(gender)

        gender["gender"] = "men"
        gender["gender"] = "men2"

        dataService.flush()

        val gender2 = dataService.get("JiraGender", 2L)!!
        println(gender2)
        Assert.assertTrue(gender["gender"] == "men2")
    }

    @Test
    fun testM1Updates() {

        val gender = dataService.find(
                on("JiraGender")
                        .where("{{gender}}='woman'"))!!

        Assert.assertNotNull(gender)

        val worker = dataService.find(
                on("JiraWorker").withRefs()
                        .where("{{name}} = 'Fillip Bedrosovich'"))!!

        println(worker)
        Assert.assertNotNull(worker)

        //получай филлипп бедросыч
        worker["gender"] = gender

        dataService.flush()

        val worker2 = dataService.find(
                on("JiraWorker").withRefs()
                        .where("{{name}} = 'Fillip Bedrosovich'"))!!
        Assert.assertEquals(worker2("gender")["gender"] , "woman")
    }


}