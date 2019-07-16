package com.bftcom.ice.server.examples

import com.bftcom.ice.datamaps.f
import com.bftcom.ice.datamaps.on
import com.bftcom.ice.datamaps.projection
import com.bftcom.ice.datamaps.slice
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.Gender
import com.bftcom.ice.server.Project
import org.junit.Assert
import org.junit.Test


/***
 * Примеры на работу с датамапсами и проекциями
 * (Fieldset-API)
 */
open class Examples01Advanced : BaseSpringTests() {


    @Test
    //Использование формул в запросах, и отображение полученных результатов в поля датамапа
    fun exampleQueryFormulas() {

        //формула + фильтр по полю формулы
        val gender = dataService.find(Gender.on()
                .formula("caption", """
                    case when {{id}}=1 then 'Ж'
                         when {{id}}=2 then 'М'
                         else 'О' end
                """)
                .filter(f("caption") eq "Ж"))!!

        println(gender["caption"])

        //lateral: получение элементов дочерней коллекции
        // на родительский уровень для отображения в списке
        val dp = Project.on()
                .lateral("tasks", """
                    (select string_agg(t.n, ';') as tasks1, count(*) as qty1
                            from task t
                            where t.project_id= {{id}}
                            ) tasks on true
                    """,
                        "tasks1" to "tasks", "qty1" to "qty")
                .where("{{tasks.tasks1}} like '%001%'")
    }

    @Test
    //апгрейды:  догрузка
    fun exampleUpgrades() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                on("Project")
                        .scalars()
                        .where("{{name}} = 'QDP'")
        )

        //2 догружаем коллекции
        dataService.upgrade(projects, projection()
                .with {
                    slice("Tasks") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })


        //use complex indexator
        Assert.assertTrue(projects[0].nested("Tasks[0].checklists[0].name") == "foo check")
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