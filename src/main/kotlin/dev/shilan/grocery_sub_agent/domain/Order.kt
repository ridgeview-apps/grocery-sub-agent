package dev.shilan.grocery_sub_agent.domain

import java.math.BigDecimal
import java.time.LocalDate

/** One order plus the customer's hard constraints: everything the agent needs to plan substitutions. */
data class OrderDetails(
    val orderId: String,
    val customerId: String,
    val storeId: String,
    val deliveryDate: LocalDate,
    // The domain type, not raw columns, so the agent loop can pass it straight to SubstitutionFilter.
    val constraints: CustomerConstraints,
    val lines: List<OrderLine>,
)

/** One order line, with stock at the order's store. */
data class OrderLine(
    val lineNo: Int,
    val productId: String,
    val name: String,
    val category: String,
    val priceGbp: BigDecimal,
    val quantityOrdered: Int,
    // Null when STOCK has no row for this store and product. That means unknown, which is not the same as zero.
    val stockQuantity: Int?,
    val stockStatus: String?,
)
