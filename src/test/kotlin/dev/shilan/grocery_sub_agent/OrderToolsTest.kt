package dev.shilan.grocery_sub_agent

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

// Runs against H2, seeded from src/test/resources/schema.sql and data.sql.
@SpringBootTest
class OrderToolsTest(@Autowired private val orderTools: OrderTools) {

    @Test
    fun `O0025 returns C001's constraints and four lines with stock at S01`() {
        val order = orderTools.getOrder("O0025")

        assertEquals("O0025", order.orderId)
        assertEquals("C001", order.customerId)
        assertEquals("S01", order.storeId)
        assertEquals(LocalDate.of(2026, 11, 20), order.deliveryDate)
        assertEquals(
            CustomerConstraints(allergens = setOf("tree_nuts", "peanuts"), diet = Diet.VEGETARIAN, priceTolerancePct = 15),
            order.constraints,
        )

        assertEquals(listOf(1, 2, 3, 4), order.lines.map { it.lineNo })
        assertEquals(listOf("P001", "P010", "P018", "P026"), order.lines.map { it.productId })
        // 0, not null: P001 has a stock row that says it's out of stock.
        assertEquals(listOf(0, 0, 51, 34), order.lines.map { it.stockQuantity })
        assertEquals(
            listOf("OUT_OF_STOCK", "OUT_OF_STOCK", "IN_STOCK", "IN_STOCK"),
            order.lines.map { it.stockStatus },
        )

        val p001 = order.lines.first()
        assertEquals("Vegetarian Sausages 6 pack", p001.name)
        assertEquals("sausages", p001.category)
        assertEquals(BigDecimal("2.75"), p001.priceGbp)
        assertEquals(1, p001.quantityOrdered)
    }

    @Test
    fun `a line with no stock row at the order's store has null stock fields`() {
        val line = orderTools.getOrder("O9001").lines.single()

        assertEquals("P018", line.productId)
        assertEquals(2, line.quantityOrdered)
        assertNull(line.stockQuantity)
        assertNull(line.stockStatus)
    }

    @Test
    fun `an unknown order ID fails`() {
        assertFailsWith<IllegalArgumentException> { orderTools.getOrder("O9999") }
    }
}
