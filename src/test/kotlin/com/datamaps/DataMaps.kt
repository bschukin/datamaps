package com.datamaps

import com.datamaps.maps.Field
import com.datamaps.maps.MFS

object Gender : MFS<Gender>() {
    val entity = "JiraGender"
    val id = Field.id()
    val gender = Field.string("gender")
    val isClassic = Field.boolean("isClassic")
}
typealias GDR = Gender

object Worker : MFS<Worker>() {
        val entity = "JiraWorker"
        val id = Field.id()
        val gender = Field.reference("gender", Gender)
        val name = Field.string("name")
        val email = Field.string("email")

}

typealias WRKR = Worker


object StaffUnit : MFS<StaffUnit>() {
        val entity = "JiraStaffUnit"
        val id = Field.long("ID")
        val name = Field.string("name")
        val gender = Field.reference("gender", Gender)
        val worker = Field.reference("worker", Worker)


}

typealias SU = StaffUnit

object Department : MFS<Department>() {
        val entity = "JiraDepartment"
        val id = Field.id()
        val name = Field.string("name")
        val fullName = Field.string("fullName")
        val parent = Field.reference("parent", Department)
        val childs = Field.list("childs", Department)

}
typealias DTP = Department


object Project : MFS<Project>() {

    val entity = "JiraProject"
    val name = Field.string("name")
    val tasks = Field.list("jiraTasks", Task)
}

object Task : MFS<Task>() {

    val entity = "JiraTask"
    val name = Field.string("name")
    val checks = Field.list("jiraChecklists", Check)
}

typealias TSK = Task

object Check : MFS<Check>() {
        val entity = "JiraChecklist"
        val id = Field.id()
        val name = Field.string("name")
        val task = Field.reference("jiraTask", Task)

}