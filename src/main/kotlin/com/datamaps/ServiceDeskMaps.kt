package com.datamaps

import com.datamaps.maps.Field
import com.datamaps.maps.MFS


object BftSubdivision : MFS<BftSubdivision>() {
    val entity = "BftSubdivision"
    val table = "BftSubdivision"

    val id = Field.long("id")
    val name = Field.string("name")
}

object ContractType : MFS<ContractType>() {
    val entity = "ContractType"
    val table = "ContractType"
    val name = Field.string("name")
}

object Contract : MFS<Contract>() {
    val entity = "Contract"
    val table = "Contract"


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
}

typealias CTR = Contract

object ContractOrg : MFS<ContractOrg>() {
    val entity = "ContractOrg"
    val table = "ContractOrg"
    val id = Field.long("id")
    val contract = Field.reference("contract", Contract)
    val organisation = Field.reference("organisation", Organisation)
    val slas = Field.list("slas", SLA)
}

object ContractProduct : MFS<ContractProduct>() {
    val entity = "ContractProduct"
    val table = "ContractProduct"

    val id = Field.long("id")
    val contract = Field.reference("contract", Contract)
    val product = Field.reference("product", Product)
    val modules = Field.list("contractproductmodules", ContractProductModule)
    val slas = Field.list("slas", SLA)
}

object ContractProductModule : MFS<ContractProductModule>() {
    val entity = "ContractProductModule"
    val table = "ContractProductModule"

    val id = Field.long("id")
    val contractProduct = Field.reference("contractProduct", ContractProduct)
    val module = Field.reference("module", Module)
}

typealias CPM = ContractProductModule

object Module : MFS<Module>() {
    val entity = "Module"
    val table = "Module"
    val id = Field.long("id")
    val name = Field.string("name")
    val notActive = Field.boolean("notActive")
    val product = Field.reference("product", Product)

}


object Organisation : MFS<Organisation>() {
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

typealias ORG = Organisation

object OrgUser : MFS<OrgUser>() {
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


object Product : MFS<Product>() {
    val entity = "Product"
    val table = "Product"

    val id = Field.long("id")
    val name = Field.string("name")
    val email = Field.string("email")
    val modules = Field.list("modules", Module)
    val contracts = Field.list("contractproducts", ContractProduct)
}

object Priority : MFS<Priority>() {
    val entity = "Priority"
    val table = "Priority"

    val id = Field.long("id")
    val name = Field.string("name")
    val rang = Field.int("rang")
}

object Service : MFS<Service>() {
    val entity = "Service"
    val table = "Service"

    val id = Field.long("id")
    val name = Field.string("name")
    val default = Field.boolean("default")
}

object SLA : MFS<SLA>() {
    val entity = "SLA"
    val table = "SLA"


    val id = Field.long("id")
    val sla = Field.int("sla")
    val contractOrg = Field.reference("contractOrg", ContractOrg)
    val contractProduct = Field.reference("contractProduct", ContractProduct)
    val service = Field.reference("service", Service)
    val priority = Field.reference("priority", Priority)
}


object TimeZone : MFS<TimeZone>() {
    val entity = "TimeZone"
    val table = "TimeZone"

    val id = Field.long("id")
    val name = Field.string("name")
}
