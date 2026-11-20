package dev.shilan.grocery_sub_agent

import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component

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

/** The per-order columns, which ORDERS repeats on every line. */
private data class OrderHeader(
    val orderId: String,
    val customerId: String,
    val storeId: String,
    val deliveryDate: LocalDate,
    val constraints: CustomerConstraints,
)

@Component
class OrderTools(private val jdbc: JdbcClient) {

    @McpTool(
        name = "get_order",
        description = "Get one online order: its customer, store and delivery date, the customer's hard " +
                "constraints (allergens, diet, price tolerance %), and every line with product, category, price, " +
                "quantity ordered and stock quantity and status at the order's store. Stock fields are null when " +
                "the store has no stock record for that product. Fails with an error if the order ID does not exist.",
        annotations = McpTool.McpAnnotations(readOnlyHint = true),
    )
    fun getOrder(
        @McpToolParam(description = "Order ID, for example O0025", required = true)
        orderId: String,
    ): OrderDetails {
        val header = findHeader(orderId)
        return OrderDetails(
            orderId = header.orderId,
            customerId = header.customerId,
            storeId = header.storeId,
            deliveryDate = header.deliveryDate,
            constraints = header.constraints,
            lines = findLines(orderId),
        )
    }

    private fun findHeader(orderId: String): OrderHeader {
        // LIMIT 2, not 1: a second distinct row means the lines disagree, and that must not pass silently.
        val rows = jdbc.sql(
            """
            SELECT DISTINCT o.ORDER_ID, o.CUSTOMER_ID, o.STORE_ID, o.DELIVERY_DATE,
                   c.ALLERGENS, c.DIET, c.PRICE_TOLERANCE_PCT
            FROM ORDERS o
            JOIN CUSTOMERS c ON c.CUSTOMER_ID = o.CUSTOMER_ID
            WHERE o.ORDER_ID = :orderId
            LIMIT 2
            """.trimIndent()
        )
            .param("orderId", orderId)
            .query { rs, _ ->
                OrderHeader(
                    orderId = rs.getString("ORDER_ID"),
                    customerId = rs.getString("CUSTOMER_ID"),
                    storeId = rs.getString("STORE_ID"),
                    // Snowflake's JDBC driver rejects getObject(..., LocalDate); getDate works on both it and H2.
                    deliveryDate = rs.getDate("DELIVERY_DATE").toLocalDate(),
                    constraints = CustomerConstraints(
                        allergens = parseAllergens(rs.getString("ALLERGENS")).toSet(),
                        diet = Diet.fromDb(rs.getString("DIET")),
                        priceTolerancePct = rs.getInt("PRICE_TOLERANCE_PCT"),
                    ),
                )
            }
            .list()
        require(rows.isNotEmpty()) { "Order $orderId not found" }
        check(rows.size == 1) { "Order $orderId has conflicting store, customer or date across its lines" }
        return rows.single()
    }

    private fun findLines(orderId: String): List<OrderLine> =
        jdbc.sql(
            """
            SELECT o.LINE_NO, o.PRODUCT_ID, p.NAME, p.CATEGORY, p.PRICE_GBP,
                   o.QUANTITY AS QUANTITY_ORDERED,
                   s.QUANTITY AS STOCK_QUANTITY, s.STATUS AS STOCK_STATUS
            FROM ORDERS o
            JOIN PRODUCTS p ON p.PRODUCT_ID = o.PRODUCT_ID
            LEFT JOIN STOCK s ON s.STORE_ID = o.STORE_ID AND s.PRODUCT_ID = o.PRODUCT_ID
            WHERE o.ORDER_ID = :orderId
            ORDER BY o.LINE_NO
            LIMIT 100
            """.trimIndent()
        )
            .param("orderId", orderId)
            .query { rs, _ ->
                OrderLine(
                    lineNo = rs.getInt("LINE_NO"),
                    productId = rs.getString("PRODUCT_ID"),
                    name = rs.getString("NAME"),
                    category = rs.getString("CATEGORY"),
                    priceGbp = rs.getBigDecimal("PRICE_GBP"),
                    quantityOrdered = rs.getInt("QUANTITY_ORDERED"),
                    // getInt returns 0 for NULL, and wasNull() checks the last column read,
                    // so nothing may be read between these two calls.
                    stockQuantity = rs.getInt("STOCK_QUANTITY").takeUnless { rs.wasNull() },
                    stockStatus = rs.getString("STOCK_STATUS"),
                )
            }
            .list()
}
