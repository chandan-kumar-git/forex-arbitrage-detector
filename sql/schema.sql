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

-- CAP-6: users of the Servlet/JSP portal
CREATE TABLE IF NOT EXISTS app_user (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(40)  NOT NULL,
    password_hash  VARCHAR(200) NOT NULL,
    display_name   VARCHAR(60)  NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at  TIMESTAMP    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_username (username)
);

-- CAP-7: profit alerts (JPA entity ProfitAlert)
CREATE TABLE IF NOT EXISTS profit_alert (
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    owner              VARCHAR(40)    NOT NULL,
    min_profit         DECIMAL(10, 6) NOT NULL,
    currency_code      CHAR(3)        NULL,
    active             BOOLEAN        NOT NULL DEFAULT TRUE,
    trigger_count      INT            NOT NULL DEFAULT 0,
    last_triggered_at  DATETIME(3)    NULL,
    last_path          VARCHAR(120)   NULL,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_alert_profit CHECK (min_profit >= 0)
);
