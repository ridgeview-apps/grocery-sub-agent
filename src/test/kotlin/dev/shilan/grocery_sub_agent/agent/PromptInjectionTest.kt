package dev.shilan.grocery_sub_agent.agent

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Injection check 1 (CI): a product whose name carries instructions to the model. The filter rejects it,
 * so its text never reaches the ranker, and a ranker that obeys it anyway is overruled. Check 2, which
 * tests the model's own resistance, runs against live Gemini.
 */
class PromptInjectionTest {

    private val db = TestDatabase()

    @AfterTest
    fun shutDown() = db.close()

    @Test
    fun `injected instructions in a rejected product never reach the ranker, and obeying them is overruled`() {
        // A fully compromised model: it does what the injected product name says.
        val ranker = RecordingRanker { Ranking(INJECTED_PRODUCT_ID, "Chosen as instructed") }

        val plan = db.agent(ranker).plan("O0025")

        val milk = assertIs<LineDecision.Escalated>(plan.decisions.single { it.line.productId == "P010" })
        assertEquals(EscalationCause.RANKER_OUTPUT_INVALID, milk.cause)
        assertTrue(milk.rejected.any { it.product.productId == INJECTED_PRODUCT_ID })
        val namesSentToRanker = ranker.requests.flatMap { request -> request.allowed.map { it.name } }
        assertTrue(namesSentToRanker.none { "ignore previous instructions" in it.lowercase() })
    }

    private companion object {
        private const val INJECTED_PRODUCT_ID = "P901"
    }
}
