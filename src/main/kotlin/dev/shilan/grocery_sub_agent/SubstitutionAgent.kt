package dev.shilan.grocery_sub_agent

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/** What the ranker sees for one out-of-stock line. Every candidate in `allowed` has already passed the guardrails. */
data class RankRequest(
    val original: OrderLine,
    val allowed: List<StockedProduct>,
    // A preference hint only; never treated as a constraint.
    val acceptedHistory: List<SubstitutionRecord>,
)

/** The ranker's answer: a product ID that should come from the request's allowed list, and a one-line reason for the picker. */
data class Ranking(val productId: String, val reason: String)

/** Chooses among pre-filtered candidates. The model sits behind this, so the agent re-checks every answer. */
fun interface SubstituteRanker {
    fun rank(request: RankRequest): Ranking
}

enum class EscalationCause { STOCK_UNKNOWN, NO_ALLOWED_CANDIDATE, RANKER_FAILED, RANKER_OUTPUT_INVALID }

/** A candidate that was in stock but ruled out, with every reason, so the picker can see why. */
data class RejectedCandidate(
    val product: StockedProduct,
    val filterReasons: List<RejectionReason>,
    val previouslyRejected: Boolean,
)

sealed interface LineDecision {
    val line: OrderLine

    data class Proposed(
        override val line: OrderLine,
        val substitute: StockedProduct,
        val reason: String,
    ) : LineDecision

    /** The agent will not choose; the picker decides. */
    data class Escalated(
        override val line: OrderLine,
        val cause: EscalationCause,
        val rejected: List<RejectedCandidate>,
    ) : LineDecision
}

/** One decision per line that needs a substitute; lines in stock are left out. */
data class SubstitutionPlan(val orderId: String, val decisions: List<LineDecision>)

/**
 * Plans substitutions for one order. Code calls the tools in a fixed order and applies every
 * hard rule; the ranker only chooses among candidates that already passed, and its answer is
 * checked again before anything is proposed. When nothing passes, the line goes to the picker.
 */
@Service
class SubstitutionAgent(
    private val orderTools: OrderTools,
    private val stockTools: StockTools,
    private val historyTools: SubstitutionHistoryTools,
    private val ranker: SubstituteRanker,
) {

    fun plan(orderId: String): SubstitutionPlan {
        val order = orderTools.getOrder(orderId)
        val history = historyTools.getSubstitutionHistory(order.customerId)
        val decisions = order.lines.mapNotNull { line ->
            when (line.stockQuantity) {
                // No stock row means unknown, and guessing either way could be wrong.
                null -> LineDecision.Escalated(line, EscalationCause.STOCK_UNKNOWN, emptyList())
                0 -> decide(order, line, history)
                // Partial stock (fewer than ordered) counts as in stock: see README Known limitations.
                else -> null
            }
        }
        return SubstitutionPlan(order.orderId, decisions)
    }

    private fun decide(order: OrderDetails, line: OrderLine, history: List<SubstitutionRecord>): LineDecision {
        val candidates = stockTools.findInStockProducts(order.storeId, line.category)
        val verdicts = SubstitutionFilter.evaluate(order.constraints, line.priceGbp, candidates)
        // Only rejections of this same swap count; a substitute refused for a different original may still suit.
        val previouslyRejectedIds = history
            .filter { it.originalProductId == line.productId && it.outcome == Outcome.REJECTED }
            .map { it.substituteProductId }
            .toSet()

        val rejected = verdicts.mapNotNull { (product, verdict) ->
            val filterReasons = (verdict as? Verdict.Rejected)?.reasons.orEmpty()
            val previouslyRejected = product.productId in previouslyRejectedIds
            if (filterReasons.isEmpty() && !previouslyRejected) null
            else RejectedCandidate(product, filterReasons, previouslyRejected)
        }
        val rejectedIds = rejected.map { it.product.productId }.toSet()
        val allowed = candidates.filter { it.productId !in rejectedIds }
        if (allowed.isEmpty()) {
            return LineDecision.Escalated(line, EscalationCause.NO_ALLOWED_CANDIDATE, rejected)
        }

        val ranking = try {
            ranker.rank(RankRequest(line, allowed, history.filter { it.outcome == Outcome.ACCEPTED }))
        } catch (e: Exception) {
            // A model outage should hand one line to the picker, not fail the whole order.
            log.warn("Ranker failed for order {} line {}", order.orderId, line.lineNo, e)
            return LineDecision.Escalated(line, EscalationCause.RANKER_FAILED, rejected)
        }

        // The guardrail on the model's answer: only a product from `allowed` can ever be proposed.
        val substitute = allowed.find { it.productId == ranking.productId }
        val reason = ranking.reason.trim()
        if (substitute == null || reason.isEmpty() || reason.length > MAX_REASON_LENGTH) {
            log.warn("Invalid ranker output for order {} line {}: {}", order.orderId, line.lineNo, ranking)
            return LineDecision.Escalated(line, EscalationCause.RANKER_OUTPUT_INVALID, rejected)
        }
        return LineDecision.Proposed(line, substitute, reason)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(SubstitutionAgent::class.java)
        // "One line" for a picker's screen; anything longer suggests the model went off task.
        private const val MAX_REASON_LENGTH = 280
    }
}