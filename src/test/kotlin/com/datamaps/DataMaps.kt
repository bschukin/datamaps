package com.datamaps

import com.datamaps.mappings.DM
import com.datamaps.mappings.Field

class Gender : DM() {
    companion object {
        val entity = "JiraGender"
        val id = Field.id()
        val gender = Field.string("gender")
    }
}
typealias GDR = Worker

class Worker : DM() {
    companion object {
        val entity = "JiraWorker"
        val id = Field.id()
        val gender = Field.reference("gender", Gender)
    }
}

typealias WRKR = Worker


class StaffUnit : DM() {
    companion object {
        val entity = "JiraStaffUnit"
        val id = Field.long("ID")
        val name = Field.string("name")
        val gender = Field.reference("gender", Gender)
        val worker = Field.reference("worker", Worker)
    }
}

typealias SU = StaffUnit

class Department : DM() {
    companion object {
        val entity = "JiraDepartment"
        val id = Field.id()
        val name = Field.string("name")
        val fullName = Field.string("fullName")
        val parent = Field.reference("parent", Department)
        val childs = Field.list("childs", Department)
    }
}
typealias DTP = Department

class Task : DM() {
    companion object {
        val entity = "JiraTask"
        val id = Field.id()
        val name = Field.string("name")
        val checks = Field.list("jiraChecklists", Check)
    }
}

typealias TSK = Task

class Check : DM() {
    companion object {
        val entity = "JiraChecklist"
        val id = Field.id()
        val name = Field.string("name")
        val task = Field.reference("jiraTask", Task)
    }
}