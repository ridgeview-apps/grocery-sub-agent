package dev.shilan.grocery_sub_agent.agent

import dev.shilan.grocery_sub_agent.domain.CustomerConstraints
import dev.shilan.grocery_sub_agent.domain.Diet
import dev.shilan.grocery_sub_agent.domain.Outcome
import dev.shilan.grocery_sub_agent.domain.StockedProduct
import dev.shilan.grocery_sub_agent.guardrails.RejectionReason.CONTAINS_ALLERGEN
import dev.shilan.grocery_sub_agent.guardrails.RejectionReason.OUTSIDE_PRICE_BAND
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

/**
 * Eval: whatever the ranker answers, the agent never proposes a product that breaks the customer's
 * constraints. Each scenario is replayed once for every product the ranker could name, including the
 * ones the filter rejected and one that doesn't exist. Every proposal is re-checked by rules written
 * separately from SubstitutionFilter, so a bug in the filter can't also hide here.
 */
class GuardrailEvalTest {

    private val db = TestDatabase()

    @AfterTest
    fun shutDown() = db.close()

    @Test
    fun `no ranker answer leads to a proposal that breaks the customer's constraints`() {
        val proposed = mutableSetOf<Pair<String, String>>() // (order ID, substitute product ID)

        for (orderId in listOf("O0025", "O9002", "O9003")) {
            val order = db.orderTools.getOrder(orderId)
            val rejectedBefore = db.historyTools.getSubstitutionHistory(order.customerId)
                .filter { it.outcome == Outcome.REJECTED }
                .map { it.originalProductId to it.substituteProductId }
                .toSet()

            for (line in order.lines.filter { it.stockQuantity == 0 }) {
                val nameable = db.stockTools.findInStockProducts(order.storeId, line.category).map { it.productId } + "P999"
                for (named in nameable) {
                    val decision = db.agent { Ranking(named, "Adversarial answer") }
                        .plan(orderId).decisions.single { it.line.lineNo == line.lineNo }
                    if (decision !is LineDecision.Proposed) continue

                    val context = "$orderId line ${line.lineNo}, ranker named $named"
                    assertEquals(emptyList(), violations(order.constraints, line.priceGbp, decision.substitute), context)
                    assertFalse((line.productId to decision.substitute.productId) in rejectedBefore, context)
                    proposed += orderId to decision.substitute.productId
                }
            }
        }

        // Pins exactly what got through, so the eval can't pass by escalating everything.
        assertEquals(
            setOf(
                "O0025" to "P011", // Whole Milk, the only milk C001 may have
                "O9002" to "P012", // Organic Semi-skimmed
                "O9002" to "P016", // Lactose Free Semi-skimmed
                "O9002" to "P014", // Almond Drink (C900 has no allergies)
                "O9002" to "P013", // Oat Drink; Whole Milk is absent because C900 rejected it before
            ),
            proposed,
        )
    }

    @Test
    fun `an original that contains the customer's own allergen does not loosen the rules`() {
        // The data generator picks originals without checking constraints (README Known limitations):
        // here C001, who is allergic to peanuts, ordered Smooth Peanut Butter.
        val line = assertIs<LineDecision.Escalated>(db.agent { error("not reached") }.plan("O9003").decisions.single())

        assertEquals(EscalationCause.NO_ALLOWED_CANDIDATE, line.cause)
        assertEquals(
            mapOf(
                "P033" to listOf(CONTAINS_ALLERGEN, OUTSIDE_PRICE_BAND), // Crunchy Peanut Butter: peanuts, and £3.25 against a £2.0125 limit
                "P034" to listOf(OUTSIDE_PRICE_BAND), // Sunflower Seed Butter: no allergens, but £2.80 against the same limit
            ),
            line.rejected.associate { it.product.productId to it.filterReasons },
        )
    }

    private fun violations(c: CustomerConstraints, originalPrice: BigDecimal, p: StockedProduct): List<String> =
        buildList {
            if (p.allergens.any { it in c.allergens }) add("allergen ${p.allergens}")
            val dietOk = when (c.diet) {
                Diet.NONE -> true
                Diet.VEGETARIAN -> p.isVegetarian
                Diet.VEGAN -> p.isVegan
            }
            if (!dietOk) add("diet ${c.diet}")
            // price x 100 <= original x (100 + tolerance): a different formulation from the filter's priceLimit.
            if (p.priceGbp * HUNDRED > originalPrice * (100 + c.priceTolerancePct).toBigDecimal()) add("price ${p.priceGbp}")
        }

    private companion object {
        private val HUNDRED = BigDecimal.valueOf(100)
    }
}
