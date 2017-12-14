package com.datamaps.servicedesk

import com.datamaps.maps.DM
import com.datamaps.maps.Field

class TimeZone : DM() {
    companion object {
        val entity = "TimeZone"
        val table = "TimeZone"

        val id = Field.long("id")
        val name = Field.string("name")
    }
}

class OrgUser : DM() {
    companion object {
        val entity = "OrgUser"
        val table = "OrgUser"

        val id = Field.long("id")
        val name = Field.string("name")
        val mobile = Field.string("mobile")
        val workPhone = Field.string("workPhone")
        val skype = Field.string("skype")
        val icq = Field.string("icq")
        val position = Field.string("position")
        val organisation = Field.reference("organisation", Organisation)

    }
}


class Subdivision : DM() {
    companion object {
        val entity = "Subdivision"
        val table = "Subdivision"

        val id = Field.long("id")
        val name = Field.string("name")

    }
}

class Organisation : DM() {
    companion object {
        val entity = "Organisation"
        val table = "Organisation"

        val id = Field.long("id")
        val name = Field.string("name")
        val fullName = Field.string("fullName")
        val legalAddress = Field.string("legalAddress")
        val actualAddress = Field.string("actualAddress")
        val INN = Field.string("INN")
        val KPP = Field.string("KPP")
        val FIAS = Field.string("FIAS")
        val telephone = Field.string("telephone")
        val email = Field.string("email")
        val isActual = Field.boolean("isActual")
        val VIP = Field.boolean("VIP")
        val workTimeStart = Field.int("workTimeStart")
        val workTimeEnd = Field.int("workTimeEnd")
        val dinnerTimeStart = Field.int("dinnerTimeStart")
        val dinnerTimeEnd = Field.int("dinnerTimeEnd")
        val loadedFromPU = Field.boolean("loadedFromPU")
        val description = Field.boolean("description")
        val timeZone = Field.reference("timeZone", TimeZone)
        val subdivision = Field.reference("subdivision", Subdivision)

    }
}

typealias ORG = Organisation