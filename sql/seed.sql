-- Default data. The app inserts the same rows at startup only when the tables are empty.
USE arbitrage_db;

INSERT IGNORE INTO currency (code, name, usd_price, active) VALUES
    ('USD', 'US Dollar',      1.00000000, TRUE),
    ('EUR', 'Euro',           1.08000000, TRUE),
    ('GBP', 'British Pound',  1.27000000, TRUE),
    ('INR', 'Indian Rupee',   0.01200000, TRUE),
    ('JPY', 'Japanese Yen',   0.00670000, TRUE);

INSERT IGNORE INTO app_setting (setting_key, setting_value) VALUES
    ('fee',           '0.0005'),
    ('spread',        '0.001'),
    ('inject_chance', '0.30');

-- Useful queries for the demo / viva
-- SELECT * FROM opportunity ORDER BY detected_at DESC LIMIT 10;
-- SELECT o.id, o.path_text, l.leg_no, l.from_code, l.to_code, l.rate
--   FROM opportunity o JOIN opportunity_leg l ON l.opportunity_id = o.id
--   ORDER BY o.id DESC, l.leg_no LIMIT 30;
-- SELECT DATE(detected_at) AS day, COUNT(*), ROUND(AVG(profit_percent), 3)
--   FROM opportunity GROUP BY DATE(detected_at);
