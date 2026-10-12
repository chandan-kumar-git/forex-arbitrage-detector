-- Forex Arbitrage Detector — MySQL 8 schema (CAP-5)
-- Run manually with:  mysql -u root -p < sql/schema.sql
-- The app also runs these CREATE TABLE statements at startup (IF NOT EXISTS).

CREATE DATABASE IF NOT EXISTS arbitrage_db;
USE arbitrage_db;

CREATE TABLE IF NOT EXISTS currency (
    code        CHAR(3)        NOT NULL,
    name        VARCHAR(40)    NOT NULL,
    usd_price   DECIMAL(18, 8) NOT NULL,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (code),
    CONSTRAINT chk_currency_price CHECK (usd_price > 0)
);

CREATE TABLE IF NOT EXISTS opportunity (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    path_text       VARCHAR(120)   NOT NULL,
    path_length     TINYINT        NOT NULL,
    profit_percent  DECIMAL(10, 6) NOT NULL,
    fee             DECIMAL(8, 6)  NOT NULL,
    source          VARCHAR(10)    NOT NULL,
    detected_at     DATETIME(3)    NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_opportunity_detected (detected_at),
    CONSTRAINT chk_opportunity_source CHECK (source IN ('LOCAL', 'SOCKET'))
);

CREATE TABLE IF NOT EXISTS opportunity_leg (
    opportunity_id  BIGINT         NOT NULL,
    leg_no          TINYINT        NOT NULL,
    from_code       CHAR(3)        NOT NULL,
    to_code         CHAR(3)        NOT NULL,
    rate            DECIMAL(18, 8) NOT NULL,
    PRIMARY KEY (opportunity_id, leg_no),
    CONSTRAINT fk_leg_opportunity FOREIGN KEY (opportunity_id)
        REFERENCES opportunity (id) ON DELETE CASCADE,
    CONSTRAINT fk_leg_from FOREIGN KEY (from_code)
        REFERENCES currency (code) ON DELETE RESTRICT,
    CONSTRAINT fk_leg_to FOREIGN KEY (to_code)
        REFERENCES currency (code) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS app_setting (
    setting_key    VARCHAR(40)  NOT NULL,
    setting_value  VARCHAR(100) NOT NULL,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key)
);
