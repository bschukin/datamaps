package com.bftcom.ice.datamaps

import org.junit.Test
import kotlin.test.assertNotNull

class ShopProjectionTest : BaseSpringTests() {

    @Test
    fun testComplexShopProjection() {
        // 1. Projection with multiple levels, joins and aggregate functions
        val dp = Order.slice {
            +orderNumber
            +date
            items {
                +quantity
                product {
                    +name
                    +price
                    category {
                        +name
                    }
                    manufacturer {
                        +name
                    }
                }
            }
        }

        // Adding a lateral join for complex calculation
        dp.lateral("totals",
            "SELECT sum(i.quantity * p.price) as total_sum, count(i.id) as items_count " +
            "FROM ORDER_ITEM i JOIN PRODUCT p ON i.product_id = p.id " +
            "WHERE i.order_id = {{id}}",
            "total_sum" to "grandTotal",
            "items_count" to "totalItems"
        )

        println("Defined complex Order projection with lateral join")

        // 2. Recursive projection for categories (e.g., for building a menu)
        val categoryDp = Category.slice {
            +name
            subCategories {
                +name
                // In a real scenario we might want recursive loading
                // option().recursive(3) // currently not fully implemented in this version based on TODOs
            }
        }.filter { Category.parent IS NULL }

        println("Defined Category tree projection")

        // 3. Flat projection using the "on" API with filters and ordering
        val productDp = Product.on().with(
            Product.name,
            Product.price,
            Product.category().name,
            Product.manufacturer().name
        ).filter {
            { Product.price gt 100.0 } and { Product.manufacturer().country eq "Germany" }
        }.order(f(Product.price).desc())

        println("Defined flat Product projection with multi-level filters")

        // 4. Using slice with filters on collections (similar to the previous example)
        val filteredOrderDp = Order.slice {
            +orderNumber
            items {
                +quantity
                product {
                    +name
                }
                withJoinFilter(f("items.quantity") gt 5)
            }
        }

        println("Defined Order projection with filtered collection")
    }
}
