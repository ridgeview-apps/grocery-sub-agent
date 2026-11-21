package dev.shilan.grocery_sub_agent.agent

import dev.shilan.grocery_sub_agent.guardrails.RejectionReason.CONTAINS_ALLERGEN
import dev.shilan.grocery_sub_agent.guardrails.RejectionReason.OUTSIDE_PRICE_BAND
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

// The picker faces the same hard rules as the model.
class PickerDecisionsTest {

    private val db = TestDatabase()
    private val decisions = db.pickerDecisions()

    @AfterTest
    fun shutDown() = db.close()

    @Test
    fun `approving the only allowed milk is accepted`() {
        // O0025 line 2: Semi-skimmed Milk (P010) out of stock; Whole Milk (P011) is what the agent proposes.
        val outcome = decisions.decide("O0025", 2, PickerDecision(PickerAction.APPROVE, "P011"))

        assertEquals(DecisionOutcome.Accepted("O0025", 2, PickerAction.APPROVE, "P011"), outcome)
    }

    @Test
    fun `overriding with a product the customer can't have is refused with every reason`() {
        // Unsweetened Almond Drink (P014) for C001: tree nuts, and £2.00 against a £1.6675 limit.
        val outcome = assertIs<DecisionOutcome.Refused>(
            decisions.decide("O0025", 2, PickerDecision(PickerAction.OVERRIDE, "P014")),
        )

        assertEquals(listOf(CONTAINS_ALLERGEN, OUTSIDE_PRICE_BAND), outcome.filterReasons)
        assertEquals(true, outcome.inStockCandidate)
    }

    @Test
    fun `for the escalated sausages, no substitute is the only choice the picker can make`() {
        // O0025 line 1: Vegetarian Lincolnshire Sausages (P002) is vegetarian but over the 15% band.
        val override = assertIs<DecisionOutcome.Refused>(
            decisions.decide("O0025", 1, PickerDecision(PickerAction.OVERRIDE, "P002")),
        )
        assertEquals(listOf(OUTSIDE_PRICE_BAND), override.filterReasons)

        assertIs<DecisionOutcome.Accepted>(decisions.decide("O0025", 1, PickerDecision(PickerAction.NO_SUBSTITUTE)))
    }

    @Test
    fun `a substitute the customer rejected before is refused even though it passes the filter`() {
        // O9002: C900 once rejected Whole Milk (P011) for Semi-skimmed Milk (P010).
        val outcome = assertIs<DecisionOutcome.Refused>(
            decisions.decide("O9002", 1, PickerDecision(PickerAction.OVERRIDE, "P011")),
        )

        assertEquals(emptyList(), outcome.filterReasons)
        assertEquals(true, outcome.previouslyRejected)
    }

    @Test
    fun `a product that isn't an in-stock candidate is refused`() {
        val outcome = assertIs<DecisionOutcome.Refused>(
            decisions.decide("O0025", 2, PickerDecision(PickerAction.OVERRIDE, "P999")),
        )

        assertEquals(false, outcome.inStockCandidate)
    }

    @Test
    fun `an override without a product, or for a line that doesn't exist, is a caller error`() {
        assertFailsWith<IllegalArgumentException> { decisions.decide("O0025", 2, PickerDecision(PickerAction.OVERRIDE)) }
        assertFailsWith<IllegalArgumentException> { decisions.decide("O0025", 9, PickerDecision(PickerAction.APPROVE, "P011")) }
    }
}
