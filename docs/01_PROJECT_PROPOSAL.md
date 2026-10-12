# Project Proposal (CAP-1)

**Title:** Bellman-Ford Triangular Arbitrage Detector and Real-time Forex Network Visualizer
**Course:** Advanced Java (Project-Based Learning)
**Technology:** Java 17, Spring Boot (core), Java Swing, JDBC + MySQL, Java Sockets

## 1. Problem statement

Currency exchange rates are quoted by many sources and change every second. Sometimes the rates
are briefly inconsistent: converting money in a loop, for example INR → EUR → GBP → INR, returns
more than you started with, even after fees. This is called **triangular arbitrage**. It disappears
within moments, so it must be detected automatically and immediately.

Finding these loops by hand is impossible, and checking every combination becomes slow as the
number of currencies grows. We need a program that models the market as a graph, finds profitable
loops quickly, shows them visually, and keeps a history for analysis.

## 2. Objectives

1. Model currencies and exchange rates as a **weighted directed graph** using OOP and Java Collections.
2. Detect profitable cycles with the **Bellman-Ford algorithm** (weights = −log(rate × (1 − fee))), and cross-check triangles with a brute-force validator.
3. Build a **Java Swing desktop GUI** that shows the live currency network, highlights profitable cycles, and lists opportunities.
4. Store currencies, settings and every detected opportunity in **MySQL using JDBC** (full CRUD with PreparedStatement).
5. Stream live rates over the network: a **socket Rate Feed Server** broadcasts rates, and the desktop app connects as a **client**.
6. Handle errors gracefully: invalid input, database offline, network disconnects.

## 3. Scope

**In scope (this semester, CAP-1 to CAP-5):** rate simulator, detection engine, Swing GUI, MySQL persistence, socket client-server feed, unit tests, documentation.

**Later (CAP-6 to CAP-8):** web interface (Servlet/JSP, MVC), login and sessions, Spring Boot with Hibernate and REST APIs, final presentation.

**Out of scope:** real money trading, real bank APIs, order execution.

## 4. Users

- **Analyst (main user):** watches the live network, studies detected opportunities, manages the currency list and fee settings.
- **Feed operator:** runs the Rate Feed Server program that broadcasts rates to connected analysts.

## 5. Modules

| Module | Responsibility | Main Java concepts |
|---|---|---|
| Model | Currency, RateSnapshot, Edge, Opportunity | Classes, records, encapsulation |
| Engine | Graph building, Bellman-Ford, profit, validation | Arrays, Collections, algorithms |
| Feed | Simulator and socket client behind one interface | Interface, polymorphism, threads |
| GUI | Dashboard, network graph, history, currencies, feed, settings | Swing, layouts, events, custom painting |
| Database | Tables, DAO classes, CRUD | JDBC, PreparedStatement, transactions |
| Server | Rate Feed Server with its own window | ServerSocket, multithreading |

## 6. Milestones

| Week | CAP | Milestone | Deliverable |
|---|---|---|---|
| 2 | CAP-1 | Proposal + core engine | This document, model + algorithm + tests |
| 4–6 | CAP-2 | LinkedIn course + reflection | Certificate, reflection report |
| 6–7 | CAP-3 | Phase I: Collections & Swing GUI | Working desktop app (in-memory) |
| 8 | CAP-4 | Coding assessment | Exam (no project deliverable) |
| 9–10 | CAP-5 | Phase II: JDBC & Networking | MySQL CRUD, socket feed, integrated GUI |
| 11–12 | CAP-6 | Phase III: Servlet, JSP, MVC | (later) |
| 13–14 | CAP-7 | Final Spring Boot project | (later) |
| 15 | CAP-8 | Demo and viva | (later) |

## 7. Feasibility

- **Technical:** every part uses the JDK, Spring Boot core and the MySQL driver taught in class. No paid tools.
- **Performance:** one detection tick takes microseconds for 5–10 currencies, so updating every second is easy.
- **Risk: MySQL not available during the demo.** The app keeps working in memory and shows "Database offline".
- **Risk: network not available.** The local simulator works without any server.

## 8. Expected outcome

A desktop application that visibly detects arbitrage within one second, stores a searchable history
in MySQL, and receives live rates from a separate server program over TCP sockets. The whole thing
is written in simple, explainable Java.
