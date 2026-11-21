package dev.shilan.grocery_sub_agent.guardrails

import dev.shilan.grocery_sub_agent.domain.CustomerConstraints
import dev.shilan.grocery_sub_agent.domain.Diet
import dev.shilan.grocery_sub_agent.domain.StockedProduct
import dev.shilan.grocery_sub_agent.guardrails.RejectionReason.BREAKS_DIET
import dev.shilan.grocery_sub_agent.guardrails.RejectionReason.CONTAINS_ALLERGEN
import dev.shilan.grocery_sub_agent.guardrails.RejectionReason.OUTSIDE_PRICE_BAND
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SubstitutionFilterTest {

    // Seed data: customer C001 and the in-stock sausages at store S01, replacing P001 (£2.75).
    private val c001 = CustomerConstraints(
        allergens = setOf("tree_nuts", "peanuts"),
        diet = Diet.VEGETARIAN,
        priceTolerancePct = 15,
    )
    private val p001Price = BigDecimal("2.75")

    private val p002 = product("P002", "Vegetarian Lincolnshire Sausages", "Greenfield", "3.25", "gluten|soya", veg = true, vegan = true)
    private val p003 = product("P003", "Mushroom & Walnut Sausages", "Grocer Own", "3.00", "gluten|tree_nuts", veg = true, vegan = true)
    private val p004 = product("P004", "Lincolnshire-style Veggie Sausages", "Hearth & Field", "3.50", "gluten|egg", veg = true, vegan = false)
    private val p005 = product("P005", "Plant-based Cumberland Sausages", "Plantwise", "3.75", "soya", veg = true, vegan = true)
    private val p006 = product("P006", "Pork Sausages 6 pack", "Grocer Own", "2.50", "gluten|sulphites", veg = false, vegan = false)
    private val p007 = product("P007", "Outdoor Bred Pork Chipolatas", "Grocer Essentials", "2.20", "gluten|sulphites", veg = false, vegan = false)
    private val p008 = product("P008", "Chicken Sausages", "Grocer Own", "3.10", "gluten", veg = false, vegan = false)

    // Cheapest first, as find_in_stock_products returns them.
    private val s01Sausages = listOf(p007, p006, p003, p008, p002, p004, p005)

    @Test
    fun `C001 replacing P001 at S01 has no allowed candidate`() {
        val verdicts = SubstitutionFilter.evaluate(c001, p001Price, s01Sausages)

        assertEquals(
            mapOf(
                p007 to Verdict.Rejected(listOf(BREAKS_DIET)),
                p006 to Verdict.Rejected(listOf(BREAKS_DIET)),
                p003 to Verdict.Rejected(listOf(CONTAINS_ALLERGEN)),
                p008 to Verdict.Rejected(listOf(BREAKS_DIET)),
                p002 to Verdict.Rejected(listOf(OUTSIDE_PRICE_BAND)),
                p004 to Verdict.Rejected(listOf(OUTSIDE_PRICE_BAND)),
                p005 to Verdict.Rejected(listOf(OUTSIDE_PRICE_BAND)),
            ),
            verdicts,
        )
        assertEquals(s01Sausages, verdicts.keys.toList())
    }

    @Test
    fun `C001 price limit is the original price plus 15 percent`() {
        assertEquals(0, SubstitutionFilter.priceLimit(c001, p001Price).compareTo(BigDecimal("3.1625")))
    }

    @Test
    fun `raising C001 tolerance to 40 percent allows the vegetarian nut-free candidates`() {
        val verdicts = SubstitutionFilter.evaluate(c001.copy(priceTolerancePct = 40), p001Price, s01Sausages)

        assertEquals(setOf(p002, p004, p005), verdicts.filterValues { it is Verdict.Allowed }.keys)
        assertEquals(Verdict.Rejected(listOf(CONTAINS_ALLERGEN)), verdicts[p003])
    }

    @Test
    fun `a cheaper candidate is always inside the price band`() {
        val noRestrictions = CustomerConstraints(emptySet(), Diet.NONE, priceTolerancePct = 0)

        assertEquals(Verdict.Allowed, SubstitutionFilter.verdictFor(noRestrictions, p001Price, p007))
    }

    @Test
    fun `a price exactly at the limit is allowed whatever its scale, one penny over is not`() {
        val zeroTolerance = CustomerConstraints(emptySet(), Diet.NONE, priceTolerancePct = 0)

        assertEquals(
            Verdict.Allowed,
            SubstitutionFilter.verdictFor(zeroTolerance, p001Price, p002.copy(priceGbp = BigDecimal("2.750"))),
        )
        assertEquals(
            Verdict.Rejected(listOf(OUTSIDE_PRICE_BAND)),
            SubstitutionFilter.verdictFor(zeroTolerance, p001Price, p002.copy(priceGbp = BigDecimal("2.76"))),
        )
    }

    @Test
    fun `every broken rule is reported, not just the first`() {
        val strict = CustomerConstraints(setOf("gluten"), Diet.VEGAN, priceTolerancePct = 0)

        assertEquals(
            Verdict.Rejected(listOf(CONTAINS_ALLERGEN, BREAKS_DIET, OUTSIDE_PRICE_BAND)),
            SubstitutionFilter.verdictFor(strict, p001Price, p008),
        )
    }

    @Test
    fun `vegan requires isVegan, so vegetarian-only products are rejected`() {
        val vegan = CustomerConstraints(emptySet(), Diet.VEGAN, priceTolerancePct = 100)

        assertEquals(Verdict.Rejected(listOf(BREAKS_DIET)), SubstitutionFilter.verdictFor(vegan, p001Price, p004))
        assertEquals(Verdict.Allowed, SubstitutionFilter.verdictFor(vegan, p001Price, p005))
    }

    @Test
    fun `diet parses the CUSTOMERS DIET values and rejects anything else`() {
        assertEquals(Diet.NONE, Diet.fromDb("none"))
        assertEquals(Diet.VEGETARIAN, Diet.fromDb(" Vegetarian "))
        assertEquals(Diet.VEGAN, Diet.fromDb("vegan"))
        assertFailsWith<IllegalArgumentException> { Diet.fromDb("pescatarian") }
    }

    private fun product(
        id: String,
        name: String,
        brand: String,
        price: String,
        allergens: String,
        veg: Boolean,
        vegan: Boolean,
    ) = StockedProduct(
        productId = id,
        name = name,
        brand = brand,
        priceGbp = BigDecimal(price),
        allergens = allergens.split('|').filter { it.isNotBlank() },
        isVegetarian = veg,
        isVegan = vegan,
        storage = "chilled",
        quantity = 10,
    )
}
