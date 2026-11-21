package dev.shilan.grocery_sub_agent.api

import dev.shilan.grocery_sub_agent.agent.DecisionOutcome
import dev.shilan.grocery_sub_agent.agent.PickerDecision
import dev.shilan.grocery_sub_agent.agent.PickerDecisions
import dev.shilan.grocery_sub_agent.agent.SubstitutionAgent
import dev.shilan.grocery_sub_agent.agent.SubstitutionPlan
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** The picker's side of the loop: get the agent's plan for an order, then approve, override or skip each line. */
@RestController
@RequestMapping("/orders/{orderId}")
class PickerController(
    private val agent: SubstitutionAgent,
    private val decisions: PickerDecisions,
) {

    // POST, not GET: each call asks the model again, so it is neither free nor guaranteed to repeat.
    @PostMapping("/plan")
    fun plan(@PathVariable orderId: String): SubstitutionPlan = agent.plan(orderId)

    @PostMapping("/lines/{lineNo}/decision")
    fun decide(
        @PathVariable orderId: String,
        @PathVariable lineNo: Int,
        @RequestBody decision: PickerDecision,
    ): ResponseEntity<DecisionOutcome> =
        when (val outcome = decisions.decide(orderId, lineNo, decision)) {
            is DecisionOutcome.Accepted -> ResponseEntity.ok(outcome)
            // 422: the request is well formed, but the choice breaks the customer's rules.
            is DecisionOutcome.Refused -> ResponseEntity.status(422).body(outcome)
        }

    // Unknown order or line, or a missing product: the caller's mistake, not a server error.
    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.badRequest().body(mapOf("error" to e.message))
}
