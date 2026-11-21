package dev.shilan.grocery_sub_agent.domain

import java.time.LocalDate

enum class Outcome { ACCEPTED, REJECTED }

/** One past substitution for a customer, and whether they accepted it. */
data class SubstitutionRecord(
    val substitutionId: String,
    val originalProductId: String,
    // Names are null if the product is missing from PRODUCTS; the record is still returned.
    val originalName: String?,
    val substituteProductId: String,
    val substituteName: String?,
    val outcome: Outcome,
    // Null for accepted substitutions.
    val rejectionReason: String?,
    val substitutedOn: LocalDate,
)
