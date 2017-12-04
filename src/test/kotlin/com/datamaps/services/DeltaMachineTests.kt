package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.assertBodyEquals
import com.datamaps.mappings.on
import com.datamaps.maps.DataMap
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
        Assert.assertEquals(worker2("gender")["gender"], "woman")
    }


    @Test(invocationCount = 1)
    fun test1MAddExisting() {

        val task001 = dataService.find(
                on("JiraTask")
                        .full()
                        .where("{{name}}='SAUMI-001'"))!!

        Assert.assertTrue(task001.list("jiraChecklists").size==0)

        val checl01 = dataService.find(
                on("JiraChecklist")
                        .where("{{id}}=1"))!!

        Assert.assertNotNull(checl01)

        Assert.assertNull(checl01["jiraTask"])

        task001.list("jiraChecklists").add(checl01)


        Assert.assertEquals(task001, checl01["jiraTask"])

        dataService.flush()

        val task0011 = dataService.find(
                on("JiraTask")
                        .full()
                        .where("{{name}}='SAUMI-001'"))!!

        println(task0011)
        Assert.assertTrue(task0011.list("jiraChecklists").size==1)
        Assert.assertEquals(task0011.list("jiraChecklists")[0], checl01)

    }

    @Test(invocationCount = 1)
    fun testInsertSimpleNoIdentityId() {

        val gender = DataMap("JiraGender", 100L, true)

        Assert.assertTrue(gender.isNew())

        dataService.flush()

        Assert.assertFalse(gender.isNew())

        val gender_ = dataService.get("JiraGender", 100L)
        assertNotNull(gender_)
    }


    @Test(invocationCount = 1)
    fun testInsertSimpleIdentityId() {

        val worker = dataService.create("JiraWorker")

        Assert.assertTrue(worker.isNew())

        dataService.flush()

        Assert.assertFalse(worker.isNew())

        val worker_ = dataService.get("JiraWorker", worker.id as Long)
        assertNotNull(worker_)
    }



}