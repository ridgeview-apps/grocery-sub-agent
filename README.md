# grocery-sub-agent

A prototype online grocery substitution agent. Full README to follow.

## Known limitations

- **Missing allergen data counts as "no allergens".** An empty value means "none", so the data has no way to say "unknown", and `find_in_stock_products` also maps a SQL `NULL` to an empty list. A product with missing allergen data would pass the allergen rule. A production version needs an explicit unknown state and a fail-closed rule for it.
