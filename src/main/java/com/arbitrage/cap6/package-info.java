/**
 * PART 5 (CAP-6, Phase III): Servlets, JSP and MVC - the web portal at http://127.0.0.1:8080/portal
 *
 * <ul>
 *   <li>{@code servlet}  - controllers: classic {@code HttpServlet} classes with doGet / doPost</li>
 *   <li>{@code filter}   - {@code AuthFilter} (login check) and {@code RequestLogFilter} (timing)</li>
 *   <li>{@code listener} - {@code PortalListener}: ServletContext and HttpSession lifecycle</li>
 *   <li>{@code auth}     - users stored with JDBC, passwords hashed with PBKDF2</li>
 *   <li>{@code view}     - small JavaBeans with getters that the JSP pages read with EL</li>
 * </ul>
 *
 * The views are JSP files in {@code src/main/resources/META-INF/resources/WEB-INF/jsp/}.
 * MVC: a servlet (Controller) asks the services for data (Model), puts it in request
 * attributes, and forwards to a JSP (View) that only displays it with EL and JSTL.
 */
package com.arbitrage.cap6;
