# Database Specification (CAP-5)

MySQL 8, database `arbitrage_db`. Access uses **plain JDBC only**, in the same style as the class programs:
`DriverManager.getConnection` → `PreparedStatement` with `?` → `setXxx` → `executeUpdate`/`executeQuery` → `ResultSet`.

## 1. ER diagram

```
┌──────────────────────┐        ┌────────────────────────────┐        ┌──────────────────────────┐
│ currency             │        │ opportunity                │        │ opportunity_leg          │
├──────────────────────┤        ├────────────────────────────┤        ├──────────────────────────┤
│ PK code CHAR(3)      │◄──┐    │ PK id BIGINT AUTO_INCREMENT│◄──────┤ PK,FK opportunity_id     │
│    name VARCHAR(40)  │   │    │    path_text VARCHAR(120)  │  1   N│ PK    leg_no TINYINT     │
│    usd_price DEC     │   ├────┤    path_length TINYINT     │       │ FK    from_code CHAR(3)  │
│    active BOOLEAN    │   │    │    profit_percent DEC      │       │ FK    to_code CHAR(3)    │
│    created_at        │   │    │    fee DECIMAL             │       │       rate DECIMAL       │
│    updated_at        │   │    │    source VARCHAR(10)      │       └──────────────────────────┘
└──────────────────────┘   │    │    detected_at DATETIME(3) │         from_code, to_code → currency
                           │    └────────────────────────────┘
                           └── referenced by opportunity_leg (ON DELETE RESTRICT)

┌──────────────────────────┐
│ app_setting              │   key/value table: fee, spread, inject_chance
├──────────────────────────┤
│ PK setting_key VARCHAR   │
│    setting_value VARCHAR │
│    updated_at            │
└──────────────────────────┘
```

Design notes, for the viva:
- `opportunity_leg` normalises the cycle: each conversion step is one row with foreign keys to `currency`, so it is 1-to-N.
- `path_text` is stored as well, purely for fast listing and search. This is a deliberate, documented denormalisation.
- `ON DELETE CASCADE` from opportunity to legs means deleting an opportunity removes its legs.
- `ON DELETE RESTRICT` from legs to currency stops a currency that appears in history from being deleted (the app shows "deactivate instead").

## 2. Schema (`sql/schema.sql`, also run by `DatabaseInitializer`)

See `../sql/schema.sql`. `DatabaseInitializer.createTables()` executes the same statements with `Statement.executeUpdate` (DDL, like class program 2).
Then `seedIfEmpty()` inserts the 5 default currencies and 3 default settings **only if** the tables are empty.

## 3. DatabaseConnection

```java
@Component
public class DatabaseConnection {
    private final String url, username, password;   // from @Value("${db.url}") etc.
    private volatile boolean online = false;
    private volatile String lastError = "";

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /** Opens and closes one connection; returns the time taken in ms. Throws DatabaseException. */
    public long test() { ... sets online/lastError ... }
}
```

There is no connection pool on purpose (simplicity). Each DAO method opens one connection in try-with-resources.

## 4. DAO methods and exact SQL

Every method catches `SQLException` and throws `DatabaseException(userMessage, e)`, after setting
`databaseConnection.markOffline(e.getMessage())` when it is a connection failure (SQLState starting with `08`).

### CurrencyDao

| Method | SQL | JDBC |
|---|---|---|
| `List<Currency> findAll()` | `SELECT code, name, usd_price, active FROM currency ORDER BY code` | `executeQuery`, loop `rs.next()` |
| `List<Currency> findActive()` | `... WHERE active = TRUE ORDER BY code` | `executeQuery` |
| `boolean exists(String code)` | `SELECT 1 FROM currency WHERE code = ?` | `setString(1, code)` |
| `void insert(Currency c)` | `INSERT INTO currency (code, name, usd_price, active) VALUES (?, ?, ?, ?)` | `setString, setString, setDouble, setBoolean`; `executeUpdate` |
| `void update(Currency c)` | `UPDATE currency SET name = ?, usd_price = ?, active = ? WHERE code = ?` | returns rows; 0 → `DatabaseException("Currency not found")` |
| `void delete(String code)` | `DELETE FROM currency WHERE code = ?` | catch `SQLIntegrityConstraintViolationException` → `DatabaseException("Used in history – deactivate it instead")` |

### OpportunityDao

| Method | SQL / behaviour |
|---|---|
| `long insertWithLegs(Opportunity o, List<OpportunityLeg> legs)` | **Transaction:** `con.setAutoCommit(false)`; `INSERT INTO opportunity (path_text, path_length, profit_percent, fee, source, detected_at) VALUES (?, ?, ?, ?, ?, ?)` with `Statement.RETURN_GENERATED_KEYS`; read the id from `getGeneratedKeys()`; then for each leg `INSERT INTO opportunity_leg (opportunity_id, leg_no, from_code, to_code, rate) VALUES (?, ?, ?, ?, ?)` using `addBatch()` + `executeBatch()`; `commit()`. On any `SQLException`, `rollback()` then throw. Return the id. |
| `List<Opportunity> search(LocalDateTime since, double minProfit, String codeLike)` | `SELECT id, path_text, profit_percent, fee, source, detected_at FROM opportunity WHERE detected_at >= ? AND profit_percent >= ? AND path_text LIKE ? ORDER BY detected_at DESC LIMIT 500`. `since` = `1970-01-01` for "All"; `codeLike` = `"%" + code + "%"` (empty → `"%"`). Use `setTimestamp(1, Timestamp.valueOf(since))`. |
| `List<OpportunityLeg> findLegs(long id)` | `SELECT leg_no, from_code, to_code, rate FROM opportunity_leg WHERE opportunity_id = ? ORDER BY leg_no` |
| `Summary summary(same filters)` | `SELECT COUNT(*), AVG(profit_percent), MAX(profit_percent) FROM opportunity WHERE ...same...`. `Summary` is a small record. |
| `int delete(long id)` | `DELETE FROM opportunity WHERE id = ?` (legs cascade) |
| `int deleteAll()` | `DELETE FROM opportunity` |

`detected_at` is written with `setTimestamp(…, Timestamp.from(o.detectedAt()))`.
`path_length` = number of currencies in the cycle (path size − 1).

### SettingsDao

| Method | SQL |
|---|---|
| `Map<String, String> loadAll()` | `SELECT setting_key, setting_value FROM app_setting` → `HashMap` |
| `void save(String key, String value)` | `INSERT INTO app_setting (setting_key, setting_value) VALUES (?, ?) ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)` |
| `void saveAll(Map<String,String> values)` | the statement above, in one transaction with `addBatch` |

Setting keys: `fee`, `spread`, `inject_chance` (values stored as plain decimal strings, e.g. `0.0005`).

## 5. Where JDBC is called (threads)

| Caller | Thread | Methods |
|---|---|---|
| `StartupService` | main | `test`, `createTables`, `seedIfEmpty`, `loadAll`, `findActive` |
| `ArbitrageService` | scheduling-1 | `insertWithLegs` (each opportunity) |
| Screens | SwingWorker via `BackgroundTask` | search, summary, findLegs, delete, currency CRUD, settings save, test |

Never call a DAO from the EDT directly.

## 6. Manual test checklist (MySQL needed)

1. Start the app with MySQL running: the `DB connected` chip is green, and the tables exist (`SHOW TABLES`).
2. Wait for 3 opportunities: `SELECT * FROM opportunity` has 3 rows, and `opportunity_leg` has 9 rows.
3. History → double-click → the legs match the database.
4. Currencies: add `AUD / Australian Dollar / 0.66`, and it appears in the graph within 1 s. Update its price, then deactivate it. Delete it (allowed while unused).
5. Try to delete `EUR` after it appears in history → friendly message. Try to delete `USD` → blocked by the validator.
6. Settings → change the fee → Save → restart the app → the value persists.
7. Stop MySQL → the app keeps detecting, the chip turns red, History shows offline + Retry. Start MySQL → Retry works.
