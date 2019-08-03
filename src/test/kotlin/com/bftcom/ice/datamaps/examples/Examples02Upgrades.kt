package com.bftcom.ice.datamaps.examples

import com.bftcom.ice.datamaps.f
import com.bftcom.ice.datamaps.on
import com.bftcom.ice.datamaps.projection
import com.bftcom.ice.datamaps.slice
import com.bftcom.ice.datamaps.BaseSpringTests
import com.bftcom.ice.datamaps.Gender
import com.bftcom.ice.datamaps.Project
import org.junit.Assert
import org.junit.Test


/***
 * Примеры на работу с датамапсами и проекциями
 * (Fieldset-API)
 */
open class Examples01Advanced : BaseSpringTests() {

    @Test
    //апгрейды:  догрузка данных карты
    fun exampleUpgrades() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                Project.slice {
                    scalars()
                    where("{{name}} = 'QDP'")
                }
        )

        //2 догружаем коллекцию тасков (с вложенной коллекцией чеклистов)
        dataService.upgrade(projects,
                Project.slice {
                    tasks {
                        full()
                    }
                })


        //use complex indexator
        Assert.assertTrue(projects[0].nested("Tasks[0].checklists[0].name") == "foo check")
    }

    @Test
    //Апгрейдится можно на любом уровне.
    //Загрузим сначала Проекты и Таски, но без чеклистов.
    // А поотм догрузим и чеклисты
    //Project(1)-->Tasks(2)-->Checklist(3) - загрузим чеклисты
    fun testUpgradesOnSecondLevel() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                Project.on().scalars()
                        .withCollections()
                        .where("{{name}} = 'QDP'")
                        .order(f("Tasks.id"))
        )

        Assert.assertTrue(projects.size == 1)
        Assert.assertTrue(projects[0].list("Tasks").size == 2)
        Assert.assertTrue(projects[0].nested("Tasks[0].checklists") == null)

        dataService.upgrade(projects,
                Project.slice {
                    tasks {
                        checks {
                            scalars()
                        }
                    }
                })

        println(projects)

        Assert.assertTrue(projects[0].nestedl("Tasks[0].checklists").size == 2)
    }


    /***
     * Стратегия выгрузки  данных по проекции.
     * Обычно - все выгружается одним запросом , через жойны.
     *
     * Если мы считаем, что  эффективней выгрузить колекции не джойнами, а отдельными селектами,
     * то используется конструкция .asSelect() на на нужном уровне проекции
     */
    @Test

    fun testSelectCollectionWithSeparateSelect() {

        //1 грузим  проекты без коллекций
        val projects = dataService
                .findAll(on("Project")
                        .scalars()
                        .with {
                            slice("tasks") //загружаем коллекуию тасков
                                    .scalars().asSelect()
                                    .with {
                                        slice("checklists") //загружаем коллекуию чеклистов
                                                .scalars().asSelect()
                                    }
                        }
                        .where("{{name}} = :qdp").param("qdp", "QDP")
                )


        println(projects)
    }
}