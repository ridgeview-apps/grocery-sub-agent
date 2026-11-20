package dev.shilan.grocery_sub_agent

import java.math.BigDecimal

/** A customer's dietary rule, parsed from CUSTOMERS.DIET. */
enum class Diet {
    NONE, VEGETARIAN, VEGAN;

    companion object {
        /**
         * Parses "none" | "vegetarian" | "vegan" (case and surrounding spaces ignored).
         * Throws on anything else: defaulting to NONE would accept every product (fail open).
         */
        fun fromDb(value: String): Diet = when (value.trim().lowercase()) {
            "none" -> NONE
            "vegetarian" -> VEGETARIAN
            "vegan" -> VEGAN
            else -> throw IllegalArgumentException("Unknown diet: '$value'")
        }
    }
}

/** The hard constraints a substitute must satisfy for one customer. */
data class CustomerConstraints(
    val allergens: Set<String>,
    val diet: Diet,
    val priceTolerancePct: Int,
) {
    init {
        require(priceTolerancePct >= 0) { "priceTolerancePct must be >= 0, was $priceTolerancePct" }
    }
}

enum class RejectionReason { CONTAINS_ALLERGEN, BREAKS_DIET, OUTSIDE_PRICE_BAND }

/** The filter's result for one candidate. */
sealed interface Verdict {
    data object Allowed : Verdict

    data class Rejected(val reasons: List<RejectionReason>) : Verdict {
        init {
            require(reasons.isNotEmpty()) { "Rejected needs at least one reason" }
        }
    }
}

/**
 * Deterministic guardrail: decides whether each in-stock candidate may replace an
 * out-of-stock product for one customer. Reports facts only; what to do when nothing
 * is allowed is the agent loop's decision.
 */
object SubstitutionFilter {

    /** All rejection reasons for one candidate, or Allowed if there are none. */
    fun verdictFor(
        constraints: CustomerConstraints,
        originalPriceGbp: BigDecimal,
        candidate: StockedProduct,
    ): Verdict {
        val reasons = buildList {
            if (containsAllergen(constraints, candidate)) add(RejectionReason.CONTAINS_ALLERGEN)
            if (breaksDiet(constraints.diet, candidate)) add(RejectionReason.BREAKS_DIET)
            if (outsidePriceBand(constraints, originalPriceGbp, candidate)) add(RejectionReason.OUTSIDE_PRICE_BAND)
        }
        return if (reasons.isEmpty()) Verdict.Allowed else Verdict.Rejected(reasons)
    }

    /** Verdicts for every candidate, in the order given (the tool returns cheapest first). */
    fun evaluate(
        constraints: CustomerConstraints,
        originalPriceGbp: BigDecimal,
        candidates: List<StockedProduct>,
    ): Map<StockedProduct, Verdict> =
        candidates.associateWith { verdictFor(constraints, originalPriceGbp, it) }

    /** The highest price allowed: original x (1 + tolerance/100). Multiplying is exact; no rounding needed. */
    fun priceLimit(constraints: CustomerConstraints, originalPriceGbp: BigDecimal): BigDecimal =
        originalPriceGbp * (BigDecimal.ONE + constraints.priceTolerancePct.toBigDecimal().movePointLeft(2))

    private fun containsAllergen(constraints: CustomerConstraints, candidate: StockedProduct): Boolean =
        candidate.allergens.any { it in constraints.allergens }

    private fun breaksDiet(diet: Diet, candidate: StockedProduct): Boolean =
        when (diet) {
            Diet.NONE -> false
            Diet.VEGETARIAN -> !candidate.isVegetarian
            Diet.VEGAN -> !candidate.isVegan
        }

    // Uses compareTo. Don't switch to ==: BigDecimal.equals also compares scale (3.0 != 3.00).
    private fun outsidePriceBand(
        constraints: CustomerConstraints,
        originalPriceGbp: BigDecimal,
        candidate: StockedProduct,
    ): Boolean = candidate.priceGbp > priceLimit(constraints, originalPriceGbp)
}
