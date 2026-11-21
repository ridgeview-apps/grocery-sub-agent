package dev.shilan.grocery_sub_agent

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Runs against H2, seeded from src/test/resources/schema.sql and data.sql.
@SpringBootTest
@Import(FakeChatModelConfig::class)
class SubstitutionHistoryToolsTest(@Autowired private val tools: SubstitutionHistoryTools) {

    @Test
    fun `C001 history is newest first with product names and rejection reasons`() {
        val history = tools.getSubstitutionHistory("C001")

        assertEquals(listOf("H0018", "H0020"), history.map { it.substitutionId })

        val rejected = history[0]
        assertEquals(Outcome.REJECTED, rejected.outcome)
        assertEquals("Not suitable for dietary needs", rejected.rejectionReason)
        assertEquals("Vegetable Lasagne", rejected.originalName)
        assertEquals("Beef Lasagne", rejected.substituteName)
        assertEquals(LocalDate.of(2026, 9, 7), rejected.substitutedOn)

        val accepted = history[1]
        assertEquals(Outcome.ACCEPTED, accepted.outcome)
        // Stored as '' in the seed data: blank means no reason.
        assertNull(accepted.rejectionReason)
    }

    @Test
    fun `a customer with no history gets an empty list`() {
        assertEquals(emptyList(), tools.getSubstitutionHistory("C999"))
    }
}
