/**
 * PART 6 (CAP-7): the final Spring Boot project - Hibernate / JPA and a REST API at http://127.0.0.1:8080/api/v1
 *
 * <ul>
 *   <li>{@code entity}     - JPA entities mapped to the MySQL tables (one-to-many opportunity to legs)</li>
 *   <li>{@code repository} - Spring Data JPA repositories: derived queries, JPQL, paging</li>
 *   <li>{@code service}    - business logic with {@code @Transactional}</li>
 *   <li>{@code dto}        - records sent and received as JSON, with Bean Validation</li>
 *   <li>{@code rest}       - {@code @RestController} classes and the global error handler</li>
 *   <li>{@code config}     - CORS for the React web desk</li>
 * </ul>
 *
 * Layers: REST controller -> service (@Transactional) -> repository (JPA) -> MySQL.
 * The CAP-5 JDBC DAOs still exist; JPA works on the same tables, so both styles can be compared.
 */
package com.arbitrage.cap7;
