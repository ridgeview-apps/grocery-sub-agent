package dev.shilan.grocery_sub_agent

/**
 * Parses a pipe-separated allergen column. NULL and empty both mean "none listed",
 * which fails open (see README Known limitations).
 */
internal fun parseAllergens(raw: String?): List<String> =
    raw?.split('|')?.filter { it.isNotBlank() }.orEmpty()