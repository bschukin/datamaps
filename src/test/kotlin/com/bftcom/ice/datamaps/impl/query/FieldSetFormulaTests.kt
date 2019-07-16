package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.MFS
import com.bftcom.ice.datamaps.f
import com.bftcom.ice.datamaps.value
import com.bftcom.ice.server.*
import com.bftcom.ice.IfSpringProfileActive
import org.junit.Assert
import org.junit.Test


open class FieldSetFormulaTests : BaseSpringTests() {

    object GenderWithFormula : MFS<GenderWithFormula>() {
        init {
            table = "Gender"
        }

        val name = Field.string("name")

        val captionSqlFormula = Field.string("captionSqlFormula") {
            this.oqlFormula = """
                    case when {{id}}=1 then 'Ж'
                         when {{id}}=2 then 'М'
                         else 'О' end
                """
        }
    }


    @Test
    @IfSpringProfileActive("postgresql")
    fun testReadDataMapWithFormulaScalarField() {

        val genders = dataService.findAll(
                GenderWithFormula
                        .slice {
                            withFormulas()
                            filter { f(captionSqlFormula) eq value("Ж") }
                        })


        Assert.assertEquals(genders.size, 1)
        assertEqIgnoreCase(genders[0][{ captionSqlFormula }], "Ж")

    }

    object ProjectWithFormulaOnRef : MFS<ProjectWithFormulaOnRef>() {
        init {
            table = "Project"
        }

        val name = Field.string("name")

        val tasksInfo = Field.reference("taskInfo", "taskInfo") {
            this.oqlFormula = """
                        select string_agg(t.name, ';') as tasks, count(*) as qty
                            from task t
                            where t.project_id= {{id}}
                    """
        }
    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testReadDataMapWithFormulaReference() {

        val dp =
                ProjectWithFormulaOnRef.slice {
                    withFormulas()

                }.where("{{taskInfo.tasks}} like '%001%'")

        val e = dataService.findAll(dp)

        assert(e.size == 1)
        assert(e[0]["taskInfo.tasks"] == "SAUMI-001;SAUMI-002")
        assert(e[0]["taskInfo.qty"] == 2L)

    }

    object ProjectWithFormulaOnList : MFS<ProjectWithFormulaOnList>() {
        init {
            table = "Project"
        }

        val name = Field.string("name")

        val taskList = Field.list("taskList", "taskList") {
            this.oqlFormula = """
                        select id as id, name as name
                            from task t
                            where t.project_id= {{id}}
                    """
        }
    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testReadDataMapWithFormulaList() {

        val dp =
                ProjectWithFormulaOnList.slice {
                    withFormulas()

                }.where("{{id}} = 2 ")

        val e = dataService.find_(dp)

        assertTrue(e[{ taskList }].size == 2)
        assertNotNull(e[{ taskList }].find { it.id == 3 })
        assertNotNull(e[{ taskList }].find { it.id == 4 })

    }


}