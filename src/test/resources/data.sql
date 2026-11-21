-- Rows copied from data/*.csv for order O0025 (C001 at S01).
INSERT INTO CUSTOMERS VALUES
    ('C001', 'Test Customer 01 (vegetarian, nut allergy)', 'tree_nuts|peanuts', 'vegetarian', 15);

INSERT INTO PRODUCTS VALUES
    ('P001', 'Vegetarian Sausages 6 pack',  'sausages', 'Grocer Own',        2.75, '300g',   'gluten|soya',    TRUE, TRUE,  'chilled'),
    ('P010', 'Semi-skimmed Milk 2 pint',    'milk',     'Grocer Essentials', 1.45, '1.136L', 'milk',           TRUE, FALSE, 'chilled'),
    ('P018', 'Wholemeal Sliced Bread 800g', 'bread',    'Grocer Own',        1.40, '800g',   'gluten|soya',    TRUE, TRUE,  'ambient'),
    ('P026', 'Classic Green Pesto',         'pesto',    'Grocer Own',        1.80, '190g',   'milk|tree_nuts', TRUE, FALSE, 'ambient');

INSERT INTO ORDERS VALUES
    ('O0025', 'C001', 'S01', DATE '2026-11-20', 1, 'P001', 1),
    ('O0025', 'C001', 'S01', DATE '2026-11-20', 2, 'P010', 1),
    ('O0025', 'C001', 'S01', DATE '2026-11-20', 3, 'P018', 1),
    ('O0025', 'C001', 'S01', DATE '2026-11-20', 4, 'P026', 1);

INSERT INTO STOCK VALUES
    ('S01', 'P001',  0, 'OUT_OF_STOCK'),
    ('S01', 'P010',  0, 'OUT_OF_STOCK'),
    ('S01', 'P018', 51, 'IN_STOCK'),
    ('S01', 'P026', 34, 'IN_STOCK');

-- What's on the shelf at S01 to replace O0025's out-of-stock lines (P001 sausages, P010 milk),
-- copied from data/*.csv. Also used by O9002 below.
INSERT INTO PRODUCTS VALUES
    ('P002', 'Vegetarian Lincolnshire Sausages',   'sausages', 'Greenfield',        3.25, '300g',   'gluten|soya',      TRUE,  TRUE,  'chilled'),
    ('P003', 'Mushroom & Walnut Sausages',         'sausages', 'Grocer Own',        3.00, '300g',   'gluten|tree_nuts', TRUE,  TRUE,  'chilled'),
    ('P004', 'Lincolnshire-style Veggie Sausages', 'sausages', 'Hearth & Field',    3.50, '276g',   'gluten|egg',       TRUE,  FALSE, 'chilled'),
    ('P005', 'Plant-based Cumberland Sausages',    'sausages', 'Plantwise',         3.75, '300g',   'soya',             TRUE,  TRUE,  'chilled'),
    ('P006', 'Pork Sausages 6 pack',               'sausages', 'Grocer Own',        2.50, '400g',   'gluten|sulphites', FALSE, FALSE, 'chilled'),
    ('P007', 'Outdoor Bred Pork Chipolatas',       'sausages', 'Grocer Essentials', 2.20, '340g',   'gluten|sulphites', FALSE, FALSE, 'chilled'),
    ('P008', 'Chicken Sausages',                   'sausages', 'Grocer Own',        3.10, '360g',   'gluten',           FALSE, FALSE, 'chilled'),
    ('P011', 'Whole Milk 2 pint',                  'milk',     'Grocer Essentials', 1.45, '1.136L', 'milk',             TRUE,  FALSE, 'chilled'),
    ('P012', 'Organic Semi-skimmed Milk 2 pint',   'milk',     'Grocer Own',        1.90, '1.136L', 'milk',             TRUE,  FALSE, 'chilled'),
    ('P013', 'Oat Drink Barista',                  'milk',     'Oat Valley',        2.10, '1L',     'gluten',           TRUE,  TRUE,  'ambient'),
    ('P014', 'Unsweetened Almond Drink',           'milk',     'Meadowbloom',       2.00, '1L',     'tree_nuts',        TRUE,  TRUE,  'ambient'),
    ('P016', 'Lactose Free Semi-skimmed Milk',     'milk',     'Dale Farmhouse',    1.95, '1L',     'milk',             TRUE,  FALSE, 'chilled');

INSERT INTO STOCK VALUES
    ('S01', 'P002', 51, 'IN_STOCK'),
    ('S01', 'P003',  2, 'LOW'),
    ('S01', 'P004', 32, 'IN_STOCK'),
    ('S01', 'P005',  2, 'LOW'),
    ('S01', 'P006', 34, 'IN_STOCK'),
    ('S01', 'P007', 26, 'IN_STOCK'),
    ('S01', 'P008',  3, 'LOW'),
    ('S01', 'P011', 41, 'IN_STOCK'),
    ('S01', 'P012', 30, 'IN_STOCK'),
    ('S01', 'P013', 29, 'IN_STOCK'),
    ('S01', 'P014', 28, 'IN_STOCK'),
    ('S01', 'P016', 20, 'IN_STOCK');

-- Test-only order, not in Snowflake: store S99 has no STOCK rows, to exercise the LEFT JOIN.
INSERT INTO ORDERS VALUES
    ('O9001', 'C001', 'S99', DATE '2026-11-20', 1, 'P018', 2);


-- Substitution history: two C001 rows from the CSV, plus one C008 row to prove the customer filter.
INSERT INTO PRODUCTS VALUES
    ('P019', 'Seeded Batch Loaf', 'bread',      'Grocer Own', 1.85, '800g', 'gluten|sesame|soya',    TRUE,  TRUE,  'ambient'),
    ('P021', 'Sourdough Bloomer', 'bread',      'Grocer Own', 2.50, '600g', 'gluten',                TRUE,  TRUE,  'ambient'),
    ('P043', 'Vegetable Lasagne', 'ready_meal', 'Grocer Own', 3.75, '400g', 'gluten|milk|egg',        TRUE,  FALSE, 'chilled'),
    ('P044', 'Beef Lasagne',      'ready_meal', 'Grocer Own', 3.75, '400g', 'gluten|milk|egg|celery', FALSE, FALSE, 'chilled');

-- H0020 stores its empty reason as '' and H0001 as NULL, so both paths are exercised.
INSERT INTO SUBSTITUTION_HISTORY VALUES
    ('H0001', 'C008', 'P026', 'P028', 'ACCEPTED', NULL,                             DATE '2026-06-17'),
    ('H0018', 'C001', 'P043', 'P044', 'REJECTED', 'Not suitable for dietary needs', DATE '2026-09-07'),
    ('H0020', 'C001', 'P021', 'P019', 'ACCEPTED', '',                               DATE '2026-07-01');

-- Test-only customer, not in Snowflake: no allergens or diet and a wide band, so only their
-- history can rule out a candidate. They once rejected P011 as a substitute for P010.
INSERT INTO CUSTOMERS VALUES ('C900', 'Test-only customer (no constraints)', NULL, 'none', 50);
INSERT INTO ORDERS VALUES ('O9002', 'C900', 'S01', DATE '2026-11-20', 1, 'P010', 1);
INSERT INTO SUBSTITUTION_HISTORY VALUES
    ('H9001', 'C900', 'P010', 'P011', 'REJECTED', 'Prefer semi-skimmed', DATE '2026-10-01');