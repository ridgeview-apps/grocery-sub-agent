"""Generate synthetic grocer data for the substitution agent prototype.

Deterministic (seeded), so the CSVs can be regenerated exactly:
    python3 scripts/generate_data.py data/
Allergen codes follow the UK 14 major allergens; multi-valued fields are pipe-separated.
"""
import csv, random, sys
from datetime import date, timedelta
from pathlib import Path

random.seed(20261120)
out = Path(sys.argv[1] if len(sys.argv) > 1 else "data")
out.mkdir(parents=True, exist_ok=True)

# (name, category, brand, price, size, allergens, vegetarian, vegan, storage)
P = [
    # Sausages: the Session 2 scenario lives here (veg sausages + nut allergy)
    ("Vegetarian Sausages 6 pack", "sausages", "Grocer Own", 2.75, "300g", "gluten|soya", True, True, "chilled"),
    ("Vegetarian Lincolnshire Sausages", "sausages", "Greenfield", 3.25, "300g", "gluten|soya", True, True, "chilled"),
    ("Mushroom & Walnut Sausages", "sausages", "Grocer Own", 3.00, "300g", "gluten|tree_nuts", True, True, "chilled"),
    ("Lincolnshire-style Veggie Sausages", "sausages", "Hearth & Field", 3.50, "276g", "gluten|egg", True, False, "chilled"),
    ("Plant-based Cumberland Sausages", "sausages", "Plantwise", 3.75, "300g", "soya", True, True, "chilled"),
    ("Pork Sausages 6 pack", "sausages", "Grocer Own", 2.50, "400g", "gluten|sulphites", False, False, "chilled"),
    ("Outdoor Bred Pork Chipolatas", "sausages", "Grocer Essentials", 2.20, "340g", "gluten|sulphites", False, False, "chilled"),
    ("Chicken Sausages", "sausages", "Grocer Own", 3.10, "360g", "gluten", False, False, "chilled"),
    ("Frozen Vegetarian Sausages", "sausages", "Fernleaf", 3.00, "336g", "egg|gluten", True, False, "frozen"),
    # Milk & alternatives
    ("Semi-skimmed Milk 2 pint", "milk", "Grocer Essentials", 1.45, "1.136L", "milk", True, False, "chilled"),
    ("Whole Milk 2 pint", "milk", "Grocer Essentials", 1.45, "1.136L", "milk", True, False, "chilled"),
    ("Organic Semi-skimmed Milk 2 pint", "milk", "Grocer Own", 1.90, "1.136L", "milk", True, False, "chilled"),
    ("Oat Drink Barista", "milk", "Oat Valley", 2.10, "1L", "gluten", True, True, "ambient"),
    ("Unsweetened Almond Drink", "milk", "Meadowbloom", 2.00, "1L", "tree_nuts", True, True, "ambient"),
    ("Soya Drink", "milk", "Meadowbloom", 1.85, "1L", "soya", True, True, "ambient"),
    ("Lactose Free Semi-skimmed Milk", "milk", "Dale Farmhouse", 1.95, "1L", "milk", True, False, "chilled"),
    # Bread
    ("White Sliced Bread 800g", "bread", "Grocer Essentials", 1.10, "800g", "gluten|soya", True, True, "ambient"),
    ("Wholemeal Sliced Bread 800g", "bread", "Grocer Own", 1.40, "800g", "gluten|soya", True, True, "ambient"),
    ("Seeded Batch Loaf", "bread", "Grocer Own", 1.85, "800g", "gluten|sesame|soya", True, True, "ambient"),
    ("Gluten Free White Bread", "bread", "Free & Easy", 3.20, "535g", "egg", True, False, "ambient"),
    ("Sourdough Bloomer", "bread", "Grocer Own", 2.50, "600g", "gluten", True, True, "ambient"),
    # Yoghurt
    ("Greek Style Natural Yoghurt", "yoghurt", "Grocer Own", 1.60, "500g", "milk", True, False, "chilled"),
    ("Total 0% Greek Yoghurt", "yoghurt", "Olympus Dairy", 3.00, "500g", "milk", True, False, "chilled"),
    ("Coconut Yoghurt Alternative", "yoghurt", "Coco Grove", 2.75, "350g", "", True, True, "chilled"),
    ("Soya Plain Alternative to Yoghurt", "yoghurt", "Meadowbloom", 1.85, "500g", "soya", True, True, "chilled"),
    # Pesto & sauces
    ("Classic Green Pesto", "pesto", "Grocer Own", 1.80, "190g", "milk|tree_nuts", True, False, "ambient"),
    ("Red Pesto", "pesto", "Bella Tavola", 2.60, "190g", "milk|tree_nuts", True, False, "ambient"),
    ("Free From Green Pesto", "pesto", "Bella Tavola", 2.90, "190g", "", True, True, "ambient"),
    ("Tomato & Basil Pasta Sauce", "pasta_sauce", "Grocer Own", 1.20, "500g", "", True, True, "ambient"),
    ("Arrabbiata Pasta Sauce", "pasta_sauce", "Nonna's Kitchen", 2.20, "500g", "", True, True, "ambient"),
    ("Tomato & Mascarpone Sauce", "pasta_sauce", "Grocer Own", 1.90, "350g", "milk", True, False, "chilled"),
    # Snacks / spreads
    ("Smooth Peanut Butter", "spread", "Grocer Own", 1.75, "340g", "peanuts", True, True, "ambient"),
    ("Crunchy Peanut Butter", "spread", "Nutshed", 3.25, "300g", "peanuts", True, True, "ambient"),
    ("Sunflower Seed Butter", "spread", "Grocer Own", 2.80, "250g", "", True, True, "ambient"),
    ("Hummus Classic", "dip", "Grocer Own", 1.25, "200g", "sesame", True, True, "chilled"),
    ("Reduced Fat Hummus", "dip", "Grocer Essentials", 1.00, "200g", "sesame", True, True, "chilled"),
    ("Beetroot Dip", "dip", "Grocer Own", 1.50, "170g", "milk", True, False, "chilled"),
    # Cheese
    ("Mature Cheddar 400g", "cheese", "Grocer Own", 3.60, "400g", "milk", True, False, "chilled"),
    ("Extra Mature Cheddar 400g", "cheese", "Westcountry Creamery", 4.75, "350g", "milk", True, False, "chilled"),
    ("Vegan Cheddar Style Block", "cheese", "Verdant", 3.00, "200g", "", True, True, "chilled"),
    ("Parmigiano Reggiano", "cheese", "Grocer Own", 3.50, "200g", "milk", False, False, "chilled"),
    ("Italian Hard Cheese (vegetarian)", "cheese", "Grocer Own", 2.90, "200g", "milk", True, False, "chilled"),
    # Ready meals / frozen
    ("Vegetable Lasagne", "ready_meal", "Grocer Own", 3.75, "400g", "gluten|milk|egg", True, False, "chilled"),
    ("Beef Lasagne", "ready_meal", "Grocer Own", 3.75, "400g", "gluten|milk|egg|celery", False, False, "chilled"),
    ("Frozen Garden Peas", "frozen_veg", "Grocer Essentials", 1.30, "1kg", "", True, True, "frozen"),
    ("Frozen Petits Pois", "frozen_veg", "Northsea Frozen", 2.25, "750g", "", True, True, "frozen"),
    ("Chickpeas in Water", "tinned", "Grocer Essentials", 0.65, "400g", "", True, True, "ambient"),
    ("Red Kidney Beans", "tinned", "Grocer Essentials", 0.60, "400g", "", True, True, "ambient"),
    # Fish
    ("Scottish Salmon Fillets x2", "fish", "Grocer Own", 4.50, "240g", "fish", False, False, "chilled"),
    ("Smoked Salmon", "fish", "Grocer Own", 3.75, "100g", "fish", False, False, "chilled"),
]

products = []
for i, (name, cat, brand, price, size, allergens, veg, vegan, storage) in enumerate(P, start=1):
    products.append({
        "product_id": f"P{i:03d}", "name": name, "category": cat, "brand": brand,
        "price_gbp": f"{price:.2f}", "pack_size": size, "allergens": allergens,
        "is_vegetarian": str(veg).upper(), "is_vegan": str(vegan).upper(),
        "storage": storage,
    })
pid = {p["name"]: p["product_id"] for p in products}

customers = [
    # (name, allergens, diet, price tolerance %)
    ("Test Customer 01 (vegetarian, nut allergy)", "tree_nuts|peanuts", "vegetarian", 15),  # Session 2 scenario
    ("Test Customer 02 (no restrictions)", "", "none", 20),
    ("Test Customer 03 (vegan, milk allergy)", "milk", "vegan", 10),
    ("Test Customer 04 (gluten allergy)", "gluten", "none", 25),
    ("Test Customer 05 (vegetarian, sesame allergy)", "sesame", "vegetarian", 10),
    ("Test Customer 06 (no restrictions, price-sensitive)", "", "none", 5),
    ("Test Customer 07 (vegetarian, egg and soya allergy)", "egg|soya", "vegetarian", 20),
    ("Test Customer 08 (fish allergy)", "fish", "none", 15),
]
cust = [{"customer_id": f"C{i:03d}", "name": n, "allergens": a, "diet": d,
         "price_tolerance_pct": t} for i, (n, a, d, t) in enumerate(customers, start=1)]

stores = ["S01", "S02", "S03"]
stock = []
for s in stores:
    for p in products:
        r = random.random()
        qty = 0 if r < 0.12 else (random.randint(1, 4) if r < 0.25 else random.randint(5, 60))
        stock.append({"store_id": s, "product_id": p["product_id"], "quantity": qty,
                      "status": "OUT_OF_STOCK" if qty == 0 else ("LOW" if qty < 5 else "IN_STOCK")})
# Guarantee the Session 2 scenario: veg sausages out of stock at S01
for row in stock:
    if row["store_id"] == "S01" and row["product_id"] == pid["Vegetarian Sausages 6 pack"]:
        row["quantity"], row["status"] = 0, "OUT_OF_STOCK"

today = date(2026, 11, 20)
orders, oid = [], 0
for c in cust:
    for _ in range(3):
        oid += 1
        store = random.choice(stores)
        delivery = (today + timedelta(days=random.randint(0, 3))).isoformat()
        chosen = random.sample(products, random.randint(4, 8))
        for line, p in enumerate(chosen, start=1):
            orders.append({"order_id": f"O{oid:04d}", "customer_id": c["customer_id"],
                           "store_id": store, "delivery_date": delivery,
                           "line_no": line, "product_id": p["product_id"], "quantity": random.randint(1, 3)})
# Scenario order: Test Customer 01 (C001), S01, includes the out-of-stock veg sausages
oid += 1
for line, name in enumerate(["Vegetarian Sausages 6 pack", "Semi-skimmed Milk 2 pint",
                             "Wholemeal Sliced Bread 800g", "Classic Green Pesto"], start=1):
    orders.append({"order_id": f"O{oid:04d}", "customer_id": "C001", "store_id": "S01",
                   "delivery_date": today.isoformat(), "line_no": line,
                   "product_id": pid[name], "quantity": 1})

# Substitution history: same-category swaps with plausible outcomes
by_cat = {}
for p in products:
    by_cat.setdefault(p["category"], []).append(p)
reasons_rej = ["Not suitable for dietary needs", "Contains an allergen", "Too expensive",
               "Prefer the original brand", "Wrong pack size", "Don't like this product"]
history = []
for h in range(1, 161):
    c = random.choice(cust)
    cat = random.choice([k for k, v in by_cat.items() if len(v) > 1])
    c_all = set(filter(None, c["allergens"].split("|")))
    def ok(p):
        bad_diet = (c["diet"] == "vegetarian" and p["is_vegetarian"] == "FALSE") or \
                   (c["diet"] == "vegan" and p["is_vegan"] == "FALSE")
        return not bad_diet and not (c_all & set(filter(None, p["allergens"].split("|"))))
    orig = random.choice(by_cat[cat])
    others = [p for p in by_cat[cat] if p is not orig]
    safe = [p for p in others if ok(p)]
    sub = random.choice(safe) if safe and random.random() < 0.8 else random.choice(others)
    s_all = set(filter(None, sub["allergens"].split("|")))
    diet_bad = (c["diet"] == "vegetarian" and sub["is_vegetarian"] == "FALSE") or \
               (c["diet"] == "vegan" and sub["is_vegan"] == "FALSE")
    pricey = float(sub["price_gbp"]) > float(orig["price_gbp"]) * (1 + c["price_tolerance_pct"] / 100)
    if c_all & s_all:
        outcome, reason = "REJECTED", "Contains an allergen"
    elif diet_bad:
        outcome, reason = "REJECTED", "Not suitable for dietary needs"
    elif pricey and random.random() < 0.35:
        outcome, reason = "REJECTED", "Too expensive"
    elif sub["brand"] != orig["brand"] and random.random() < 0.15:
        outcome, reason = "REJECTED", "Prefer the original brand"
    elif random.random() < 0.1:
        outcome, reason = "REJECTED", random.choice(reasons_rej[3:])
    else:
        outcome, reason = "ACCEPTED", ""
    history.append({"substitution_id": f"H{h:04d}", "customer_id": c["customer_id"],
                    "original_product_id": orig["product_id"], "substitute_product_id": sub["product_id"],
                    "outcome": outcome, "rejection_reason": reason,
                    "substituted_on": (today - timedelta(days=random.randint(1, 180))).isoformat()})

def write(name, rows):
    with open(out / name, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader(); w.writerows(rows)
    print(f"{name}: {len(rows)} rows")

write("products.csv", products)
write("customers.csv", cust)
write("stock.csv", stock)
write("orders.csv", orders)
write("substitution_history.csv", history)
