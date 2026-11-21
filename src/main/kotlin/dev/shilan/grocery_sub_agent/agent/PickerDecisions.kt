package dev.shilan.grocery_sub_agent.agent

import com.fasterxml.jackson.annotation.JsonTypeInfo
import dev.shilan.grocery_sub_agent.guardrails.RejectionReason
import dev.shilan.grocery_sub_agent.tools.OrderTools
import dev.shilan.grocery_sub_agent.tools.StockTools
import dev.shilan.grocery_sub_agent.tools.SubstitutionHistoryTools
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

enum class PickerAction { APPROVE, OVERRIDE, NO_SUBSTITUTE }

/** What the picker decided for one line. A product is required unless the action is NO_SUBSTITUTE. */
data class PickerDecision(val action: PickerAction, val productId: String? = null)

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "outcome")
sealed interface DecisionOutcome {
    data class Accepted(val orderId: String, val lineNo: Int, val action: PickerAction, val productId: String?) :
        DecisionOutcome

    /** The same reasons the agent would give, so the picker sees why their choice is refused. */
    data class Refused(
        val productId: String,
        val inStockCandidate: Boolean,
        val filterReasons: List<RejectionReason>,
        val previouslyRejected: Boolean,
    ) : DecisionOutcome
}

/**
 * Checks a picker's approval or override against the same hard rules as the agent's proposals: a person
 * can choose a different substitute, but not one the customer can't have. The client's claim that it is
 * approving the proposal isn't trusted, because nothing is stored between calls; every product is re-checked.
 */
@Service
class PickerDecisions(
    private val orderTools: OrderTools,
    private val stockTools: StockTools,
    private val historyTools: SubstitutionHistoryTools,
) {

    fun decide(orderId: String, lineNo: Int, decision: PickerDecision): DecisionOutcome {
        val order = orderTools.getOrder(orderId)
        val line = requireNotNull(order.lines.find { it.lineNo == lineNo }) { "Order $orderId has no line $lineNo" }

        if (decision.action == PickerAction.NO_SUBSTITUTE) {
            return accepted(orderId, lineNo, decision.action, productId = null)
        }
        val productId = requireNotNull(decision.productId) { "productId is required for ${decision.action}" }

        val history = historyTools.getSubstitutionHistory(order.customerId)
        val (allowed, rejected) = assessCandidates(stockTools, order, line, history)
        if (allowed.any { it.productId == productId }) {
            return accepted(orderId, lineNo, decision.action, productId)
        }

        val candidate = rejected.find { it.product.productId == productId }
        log.info("Picker decision refused: order {} line {} {} {}", orderId, lineNo, decision.action, productId)
        return DecisionOutcome.Refused(
            productId = productId,
            inStockCandidate = candidate != null,
            filterReasons = candidate?.filterReasons.orEmpty(),
            previouslyRejected = candidate?.previouslyRejected ?: false,
        )
    }

    // Logged, not stored: the database role is read-only (see README Known limitations).
    private fun accepted(orderId: String, lineNo: Int, action: PickerAction, productId: String?): DecisionOutcome {
        log.info("Picker decision accepted: order {} line {} {} {}", orderId, lineNo, action, productId)
        return DecisionOutcome.Accepted(orderId, lineNo, action, productId)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(PickerDecisions::class.java)
    }
}
