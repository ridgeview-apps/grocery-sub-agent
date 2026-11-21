package dev.shilan.grocery_sub_agent.domain

import java.math.BigDecimal

/** One product that is in stock at one store: a substitution candidate. */
data class StockedProduct(
    val productId: String,
    val name: String,
    val brand: String,
    val priceGbp: BigDecimal,
    val allergens: List<String>,
    val isVegetarian: Boolean,
    val isVegan: Boolean,
    val storage: String,
    val quantity: Int,
)
