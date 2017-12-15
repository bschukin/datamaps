package com.datamaps.servicedesk

import com.datamaps.maps.*


class BftSubdivision : DM() {
    companion object {
        val entity = "BftSubdivision"
        val table = "BftSubdivision"

        val id = Field.long("id")
        val name = Field.string("name")

    }
}

class ContractType : DM() {
    companion object {
        val entity = "ContractType"
        val table = "ContractType"

        val id = Field.long("id")
        val name = Field.string("name")

    }
}

class Contract : DM() {
    companion object {
        val entity = "Contract"
        val table = "Contract"

        val id = Field.long("id")
        val number = Field.string("number")

        val numberPU = Field.string("numberPU")
        val state = Field.string("state")
        val projectPU = Field.string("projectPU")


        val active = Field.boolean("active")
        val organisation = Field.reference("organisation", Organisation)
        val contractType = Field.reference("contractType", ContractType)
        val bftSubdivision = Field.reference("bftSubdivision", BftSubdivision)
        val orgs = Field.list("contractorgs", ContractOrg)

    }
}

typealias CTR = Contract

class ContractOrg : DM() {
    companion object {
        val entity = "ContractOrg"
        val table = "ContractOrg"

        val id = Field.long("id")
        val contract = Field.reference("contract", Contract)
        val organisation = Field.reference("organisation", Organisation)

    }
}

class Module : DM() {
    companion object {
        val entity = "Module"
        val table = "Module"

        fun new() = DataMap(Module)
        fun on() = on(Module)
        fun filter(e: (m: Unit) -> exp) = on(Module).filter(e)

        val id = Field.long("id")
        val name = Field.string("name")
        val notActive = Field.boolean("notActive")
        val product = Field.reference("product", Product)

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
        val bftSubdivision = Field.reference("bftSubdivision", BftSubdivision)
        val contracts = Field.list("contractorgs", ContractOrg)

    }
}

typealias ORG = Organisation

class Product : DM() {
    companion object {
        val entity = "Product"
        val table = "Product"

        fun new() = DataMap(Product)
        fun on() = on(Product)
        fun filter(e: (m: Unit) -> exp) = on(Product).filter(e)

        val id = Field.long("id")
        val name = Field.string("name")
        val email = Field.string("email")
        val modules = Field.list("modules", Module)
    }
}


class TimeZone : DM() {
    companion object {
        val entity = "TimeZone"
        val table = "TimeZone"

        val id = Field.long("id")
        val name = Field.string("name")
    }
}