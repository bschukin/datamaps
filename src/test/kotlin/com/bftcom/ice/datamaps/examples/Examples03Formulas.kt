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
open class FormulasExamples : BaseSpringTests() {


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


}