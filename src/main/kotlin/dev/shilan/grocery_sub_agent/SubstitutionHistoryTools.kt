package dev.shilan.grocery_sub_agent

import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
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

@Component
class SubstitutionHistoryTools(private val jdbc: JdbcClient) {

    @McpTool(
        name = "get_substitution_history",
        description = "List a customer's past substitutions, newest first: original and substitute product " +
                "(ID and name), whether the customer accepted or rejected it, the rejection reason, and the date. " +
                "Use this to avoid proposing a substitute the customer has rejected before. " +
                "Returns an empty list if the customer has no substitution history.",
        annotations = McpTool.McpAnnotations(readOnlyHint = true),
    )
    fun getSubstitutionHistory(
        @McpToolParam(description = "Customer ID, for example C001", required = true)
        customerId: String,
    ): List<SubstitutionRecord> =
        jdbc.sql(
            """
            SELECT h.SUBSTITUTION_ID, h.ORIGINAL_PRODUCT_ID, op.NAME AS ORIGINAL_NAME,
                   h.SUBSTITUTE_PRODUCT_ID, sp.NAME AS SUBSTITUTE_NAME,
                   h.OUTCOME, h.REJECTION_REASON, h.SUBSTITUTED_ON
            FROM SUBSTITUTION_HISTORY h
            LEFT JOIN PRODUCTS op ON op.PRODUCT_ID = h.ORIGINAL_PRODUCT_ID
            LEFT JOIN PRODUCTS sp ON sp.PRODUCT_ID = h.SUBSTITUTE_PRODUCT_ID
            WHERE h.CUSTOMER_ID = :customerId
            ORDER BY h.SUBSTITUTED_ON DESC, h.SUBSTITUTION_ID DESC
            LIMIT 50
            """.trimIndent()
        )
            .param("customerId", customerId)
            .query { rs, _ ->
                SubstitutionRecord(
                    substitutionId = rs.getString("SUBSTITUTION_ID"),
                    originalProductId = rs.getString("ORIGINAL_PRODUCT_ID"),
                    originalName = rs.getString("ORIGINAL_NAME"),
                    substituteProductId = rs.getString("SUBSTITUTE_PRODUCT_ID"),
                    substituteName = rs.getString("SUBSTITUTE_NAME"),
                    // valueOf throws on anything else, so an unexpected value fails closed instead of being misread.
                    outcome = Outcome.valueOf(rs.getString("OUTCOME")),
                    // The CSV load may store a missing reason as '' or NULL; both mean none.
                    rejectionReason = rs.getString("REJECTION_REASON")?.takeUnless { it.isBlank() },
                    substitutedOn = rs.getDate("SUBSTITUTED_ON").toLocalDate(),
                )
            }
            .list()
}
