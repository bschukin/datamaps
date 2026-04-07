package com.bftcom.ice.datamaps

object Category : MFS<Category>() {
    val id = Field.id()
    val name = Field.string("name")
    val parent = Field.reference("parent", Category)
    val subCategories = Field.list("subCategories", Category)

    init {
        addOption(Tree(parent = parent, childs = subCategories))
    }
}

object Manufacturer : MFS<Manufacturer>() {
    val id = Field.id()
    val name = Field.string("name")
    val country = Field.string("country")
}

object Product : MFS<Product>() {
    val id = Field.id()
    val name = Field.string("name")
    val price = Field.double("price")
    val category = Field.referenceNN("category", Category)
    val manufacturer = Field.reference("manufacturer", Manufacturer)
}

object Order : MFS<Order>("ShopOrder") {
    val id = Field.id()
    val orderNumber = Field.string("orderNumber")
    val date = Field.date("orderDate")
    val items = Field.list("items", OrderItem)
}

object OrderItem : MFS<OrderItem>() {
    val id = Field.id()
    val order = Field.referenceNN("order", Order)
    val product = Field.referenceNN("product", Product)
    val quantity = Field.int("quantity")
}
