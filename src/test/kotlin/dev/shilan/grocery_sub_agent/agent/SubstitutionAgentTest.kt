package dev.shilan.grocery_sub_agent.agent

import dev.shilan.grocery_sub_agent.guardrails.RejectionReason.BREAKS_DIET
import dev.shilan.grocery_sub_agent.guardrails.RejectionReason.CONTAINS_ALLERGEN
import dev.shilan.grocery_sub_agent.guardrails.RejectionReason.OUTSIDE_PRICE_BAND
import dev.shilan.grocery_sub_agent.tools.OrderTools
import dev.shilan.grocery_sub_agent.tools.StockTools
import dev.shilan.grocery_sub_agent.tools.SubstitutionHistoryTools
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Real tools on a fresh H2 database per test, without a Spring context, so the ranker can be a plain fake.
class SubstitutionAgentTest {

    private val db = EmbeddedDatabaseBuilder()
        .generateUniqueName(true)
        .setType(EmbeddedDatabaseType.H2)
        .addScripts("schema.sql", "data.sql")
        .build()
    private val jdbc = JdbcClient.create(db)

    @AfterTest
    fun shutDown() = db.shutdown()

    private fun agent(ranker: SubstituteRanker) =
        SubstitutionAgent(OrderTools(jdbc), StockTools(jdbc), SubstitutionHistoryTools(jdbc), ranker)

    private class RecordingRanker(private val answer: (RankRequest) -> Ranking) : SubstituteRanker {
        val requests = mutableListOf<RankRequest>()

        override fun rank(request: RankRequest): Ranking {
            requests += request
            return answer(request)
        }
    }

    @Test
    fun `O0025 escalates P001 with every rejection reason and proposes the only allowed milk`() {
        val ranker = RecordingRanker { Ranking(it.allowed.first().productId, "Same brand and price; whole milk") }

        val plan = agent(ranker).plan("O0025")

        // P018 and P026 are in stock, so they get no decision.
        assertEquals(listOf("P001", "P010"), plan.decisions.map { it.line.productId })

        val sausages = assertIs<LineDecision.Escalated>(plan.decisions[0])
        assertEquals(EscalationCause.NO_ALLOWED_CANDIDATE, sausages.cause)
        assertEquals(
            mapOf(
                "P007" to listOf(BREAKS_DIET),
                "P006" to listOf(BREAKS_DIET),
                "P003" to listOf(CONTAINS_ALLERGEN),
                "P008" to listOf(BREAKS_DIET),
                "P002" to listOf(OUTSIDE_PRICE_BAND),
                "P004" to listOf(OUTSIDE_PRICE_BAND),
                "P005" to listOf(OUTSIDE_PRICE_BAND),
            ),
            sausages.rejected.associate { it.product.productId to it.filterReasons },
        )

        val milk = assertIs<LineDecision.Proposed>(plan.decisions[1])
        assertEquals("P011", milk.substitute.productId)
        assertEquals("Same brand and price; whole milk", milk.reason)

        // The ranker was asked once (milk only) and only ever saw what the filter allowed.
        val request = ranker.requests.single()
        assertEquals(listOf("P011"), request.allowed.map { it.productId })
        assertEquals(listOf("H0020"), request.acceptedHistory.map { it.substitutionId })
    }

    @Test
    fun `a substitute the customer rejected before for the same product is excluded before ranking`() {
        val ranker = RecordingRanker { Ranking(it.allowed.first().productId, "Closest to semi-skimmed") }

        val decision = assertIs<LineDecision.Proposed>(agent(ranker).plan("O9002").decisions.single())

        assertEquals(listOf("P012", "P016", "P014", "P013"), ranker.requests.single().allowed.map { it.productId })
        assertEquals("P012", decision.substitute.productId)
    }

    @Test
    fun `invalid ranker output is escalated, never proposed`() {
        val invalid = listOf(
            Ranking("P014", "Almond drink"), // in stock, but contains tree_nuts
            Ranking("P999", "Made up"), // not a product
            Ranking("P011", "   "), // eligible, but no reason for the picker
            Ranking("P011", "x".repeat(281)), // eligible, but not one line
        )
        for (ranking in invalid) {
            val milk = agent(RecordingRanker { ranking }).plan("O0025").decisions[1]

            val escalated = assertIs<LineDecision.Escalated>(milk, "for $ranking")
            assertEquals(EscalationCause.RANKER_OUTPUT_INVALID, escalated.cause, "for $ranking")
        }
    }

    @Test
    fun `a ranker failure escalates that line instead of failing the order`() {
        val plan = agent(RecordingRanker { error("model unavailable") }).plan("O0025")

        val milk = assertIs<LineDecision.Escalated>(plan.decisions[1])
        assertEquals(EscalationCause.RANKER_FAILED, milk.cause)
    }

    @Test
    fun `a line with no stock row is escalated as stock unknown without asking the ranker`() {
        val ranker = RecordingRanker { error("should not be called") }

        val line = assertIs<LineDecision.Escalated>(agent(ranker).plan("O9001").decisions.single())

        assertEquals(EscalationCause.STOCK_UNKNOWN, line.cause)
        assertTrue(ranker.requests.isEmpty())
    }
}