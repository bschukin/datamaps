package com.datamaps.services

import com.datamaps.BaseSpringTests
import com.datamaps.mappings.projection
import com.datamaps.mappings.slice
import org.testng.annotations.Test

/**
 * Created by Щукин on 03.11.2017.
 */
class UpgradeTests : BaseSpringTests() {


    @Test
    fun testUpgradesIdea() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                projection("JiraProject")
                        .scalars())

        println(projects)

        val q = queryBuilder.createUpgradeQueryByMapsAndSlices(projects,
                projection().withCollections())

        println(q.sql)

        val list = queryExecutor.findAll(q)
        println(list)

    }

    @Test
    fun testUpgrades01() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                projection("JiraProject")
                        .scalars()
                        .where("{{name}} = 'QDP'")
        )

        println(projects)

        //2 догружаем коллекции
        dataService.upgrade(projects, projection()
                .with {
                    slice("jiraTasks")
                            .full()
                })

        println(projects)


    }


}