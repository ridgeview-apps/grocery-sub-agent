package dev.shilan.grocery_sub_agent.agent

import dev.shilan.grocery_sub_agent.domain.OrderDetails
import dev.shilan.grocery_sub_agent.domain.OrderLine
import dev.shilan.grocery_sub_agent.domain.Outcome
import dev.shilan.grocery_sub_agent.domain.StockedProduct
import dev.shilan.grocery_sub_agent.domain.SubstitutionRecord
import dev.shilan.grocery_sub_agent.guardrails.SubstitutionFilter
import dev.shilan.grocery_sub_agent.guardrails.Verdict
import dev.shilan.grocery_sub_agent.tools.StockTools

/** In-stock candidates for one line, split by the hard rules. */
data class CandidateAssessment(val allowed: List<StockedProduct>, val rejected: List<RejectedCandidate>)

/**
 * The one place the hard rules are applied: the filter's verdicts and the customer's past rejections.
 * Both the agent (before ranking) and picker decisions (before accepting one) use it, so the model
 * and a person face exactly the same rules.
 */
internal fun assessCandidates(
    stockTools: StockTools,
    order: OrderDetails,
    line: OrderLine,
    history: List<SubstitutionRecord>,
): CandidateAssessment {
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
    return CandidateAssessment(candidates.filter { it.productId !in rejectedIds }, rejected)
}
