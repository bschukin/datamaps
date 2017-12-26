package com.datamaps

import com.datamaps.maps.*


class BftSubdivision : DM() {
    companion object {
        val entity = "BftSubdivision"
        val table = "BftSubdivision"

        val id = Field.long("id")
        val name = Field.string("name")
    }
}

object ContractType : DM() {
    val entity = "ContractType"
    val table = "ContractType"

    val name = Field.string("name")
    val name3 = Field.string("nameeasdasaddasdas")
    val name5 = Field.string("5asdasddas")


    fun filter(e: (m: Unit) -> exp) = on(ContractType).filter(e)
}

class Contract : DM() {
    companion object {
        val entity = "Contract"
        val table = "Contract"

        fun new() = DataMap(Contract)
        fun on() = on(Contract)
        fun filter(e: (m: Unit) -> exp) = on(Contract).filter(e)

        val id = Field.long("id")
        val number = Field.string("number")
        val conclusionDate = Field.date("conclusionDate")
        val numberPU = Field.string("numberPU")
        val state = Field.string("state")
        val projectPU = Field.string("projectPU")
        val startDate = Field.date("startDate")
        val finishDate = Field.date("finishDate")
        val active = Field.boolean("active")
        val organisation = Field.reference("organisation", Organisation)
        val contractType = Field.reference("contractType", ContractType)
        val bftSubdivision = Field.reference("bftSubdivision", BftSubdivision)
        val orgs = Field.list("contractorgs", ContractOrg)
        val products = Field.list("contractproducts", ContractProduct)
        val products2 = Field.list("contractproducts", ContractProduct)
        val products4 = Field.reference("ContractType", ContractType)
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
        val slas = Field.list("slas", SLA)
        val slas2 = Field.list("slas", SLA)
    }
}

class ContractProduct : DM() {
    companion object {
        val entity = "ContractProduct"
        val table = "ContractProduct"

        fun new() = DataMap(ContractProduct)
        fun on() = on(ContractProduct)
        fun filter(e: (m: Unit) -> exp) = on(ContractProduct).filter(e)

        val id = Field.long("id")
        val contract = Field.reference("contract", Contract)
        val product = Field.reference("product", Product)
        val modules = Field.list("contractproductmodules", ContractProductModule)
        val slas = Field.list("slas", SLA)
    }
}

class ContractProductModule : DM() {
    companion object {
        val entity = "ContractProductModule"
        val table = "ContractProductModule"

        fun new() = DataMap(ContractProductModule)
        fun on() = on(ContractProductModule)
        fun filter(e: (m: Unit) -> exp) = on(ContractProductModule).filter(e)

        val id = Field.long("id")
        val contractProduct = Field.reference("contractProduct", ContractProduct)
        val module = Field.reference("module", Module)

    }
}

typealias CPM = ContractProductModule

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


class Product : DM() {
    companion object {
        val entity = "Product"
        val table = "Product"

        fun new() = DataMap(Product)
        fun on() = on(Product)
        fun filter(e: (m: Unit) -> exp) = on(Product).filter(e)
        fun where(w: String) = on(Product).where(w)

        val id = Field.long("id")
        val name = Field.string("name")
        val email = Field.string("email")
        val modules = Field.list("modules", Module)
        val contracts = Field.list("contractproducts", ContractProduct)
    }
}

class Priority : DM() {
    companion object {
        val entity = "Priority"
        val table = "Priority"

        fun new() = DataMap(Priority)
        fun on() = on(Priority)
        fun filter(e: (m: Unit) -> exp) = on(Priority).filter(e)

        val id = Field.long("id")
        val name = Field.string("name")
        val rang = Field.int("rang")

    }
}

class Service : DM() {
    companion object {
        val entity = "Service"
        val table = "Service"

        fun new() = DataMap(Service)
        fun on() = on(Service)
        fun filter(e: (m: Unit) -> exp) = on(Service).filter(e)

        val id = Field.long("id")
        val name = Field.string("name")
        val default = Field.boolean("default")

    }
}


class SLA : DM() {
    companion object {
        val entity = "SLA"
        val table = "SLA"

        fun new() = DataMap(SLA)
        fun on() = on(SLA)
        fun filter(e: (m: Unit) -> exp) = on(SLA).filter(e)

        val id = Field.long("id")
        val sla = Field.int("sla")
        val contractOrg = Field.reference("contractOrg", ContractOrg)
        val contractProduct = Field.reference("contractProduct", ContractProduct)
        val service = Field.reference("service", Service)
        val priority = Field.reference("priority", Priority)

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