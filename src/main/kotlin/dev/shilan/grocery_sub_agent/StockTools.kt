package dev.shilan.grocery_sub_agent

import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.math.BigDecimal

/** One product that is in stock at one store: a substitution candidate. */
data class StockedProduct(
    val productId: String,
    val name: String,
    val brand: String,
    val priceGbp: BigDecimal,
    val allergens: List<String>,
    val isVegetarian: Boolean,
    val isVegan: Boolean,
    val storage: String,
    val quantity: Int,
)

@Component
class StockTools(private val jdbc: JdbcClient) {

    @McpTool(
        name = "find_in_stock_products",
        description = "List the products in a category that are in stock at a store, cheapest first, " +
            "with price, allergens, vegetarian/vegan flags, storage type and quantity. " +
            "Use this to find substitution candidates for an out-of-stock item.",
        annotations = McpTool.McpAnnotations(readOnlyHint = true),
    )
    fun findInStockProducts(
        @McpToolParam(description = "Store ID, for example S01", required = true)
        storeId: String,
        @McpToolParam(description = "Product category in lower case, for example sausages", required = true)
        category: String,
    ): List<StockedProduct> =
        jdbc.sql(
            """
            SELECT p.PRODUCT_ID, p.NAME, p.BRAND, p.PRICE_GBP, p.ALLERGENS,
                   p.IS_VEGETARIAN, p.IS_VEGAN, p.STORAGE, s.QUANTITY
            FROM STOCK s
            JOIN PRODUCTS p ON p.PRODUCT_ID = s.PRODUCT_ID
            WHERE s.STORE_ID = :storeId
              AND p.CATEGORY = :category
              AND s.QUANTITY > 0
            ORDER BY p.PRICE_GBP
            LIMIT 50
            """.trimIndent()
        )
            .param("storeId", storeId)
            .param("category", category)
            .query { rs, _ ->
                StockedProduct(
                    productId = rs.getString("PRODUCT_ID"),
                    name = rs.getString("NAME"),
                    brand = rs.getString("BRAND"),
                    priceGbp = rs.getBigDecimal("PRICE_GBP"),
                    allergens = rs.getString("ALLERGENS")
                        ?.split('|')
                        ?.filter { it.isNotBlank() }
                        .orEmpty(),
                    isVegetarian = rs.getBoolean("IS_VEGETARIAN"),
                    isVegan = rs.getBoolean("IS_VEGAN"),
                    storage = rs.getString("STORAGE"),
                    quantity = rs.getInt("QUANTITY"),
                )
            }
            .list()
}
