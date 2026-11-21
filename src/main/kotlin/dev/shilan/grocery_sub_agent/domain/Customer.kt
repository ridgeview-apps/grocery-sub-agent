package dev.shilan.grocery_sub_agent.domain

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
