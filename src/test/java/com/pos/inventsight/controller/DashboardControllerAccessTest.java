package com.pos.inventsight.controller;

import com.pos.inventsight.security.RoleConstants;
import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that DashboardController uses only @PreAuthorize(RoleConstants.GM_PLUS)
 * for access control — no redundant manual isGMPlusRole() check in the method body.
 *
 * <p>Root cause of the original 403 bug: a secondary manual check read {@code user.getRole()}
 * from the database (global UserRole, e.g. EMPLOYEE) rather than the JWT-derived authority
 * (e.g. MANAGER mapped from CompanyRole.GENERAL_MANAGER). That check has been removed;
 * {@code @PreAuthorize(RoleConstants.GM_PLUS)} is now the single authorization gate.</p>
 *
 * <p>Acceptance criteria verified:
 * <ul>
 *   <li>Every dashboard endpoint carries {@code @PreAuthorize(RoleConstants.GM_PLUS)}.</li>
 *   <li>RoleConstants.GM_PLUS includes GENERAL_MANAGER and MANAGER authorities so users
 *       whose JWT authority is mapped from {@code CompanyRole.GENERAL_MANAGER} are accepted.</li>
 *   <li>The private {@code isGMPlusRole(User)} helper method no longer exists in the
 *       controller, confirming the redundant check was removed.</li>
 * </ul>
 * </p>
 */
public class DashboardControllerAccessTest {

    // ── Verify @PreAuthorize annotations are present and correct ──────────────

    @Test
    void getDashboardSummary_HasGmPlusPreAuthorize() throws NoSuchMethodException {
        Method method = DashboardController.class.getMethod(
                "getDashboardSummary", Authentication.class, String.class);
        assertGmPlusAnnotation(method, "GET /dashboard/summary");
    }

    @Test
    void refreshDashboard_HasGmPlusPreAuthorize() throws NoSuchMethodException {
        Method method = DashboardController.class.getMethod(
                "refreshDashboard", Authentication.class, String.class);
        assertGmPlusAnnotation(method, "POST /dashboard/refresh");
    }

    @Test
    void getDashboardKPIs_HasGmPlusPreAuthorize() throws NoSuchMethodException {
        Method method = DashboardController.class.getMethod(
                "getDashboardKPIs", Authentication.class);
        assertGmPlusAnnotation(method, "GET /dashboard/kpis");
    }

    @Test
    void getDashboardStats_HasGmPlusPreAuthorize() throws NoSuchMethodException {
        Method method = DashboardController.class.getMethod(
                "getDashboardStats", UUID.class, String.class, String.class, Authentication.class);
        assertGmPlusAnnotation(method, "GET /dashboard/stats");
    }

    @Test
    void getRevenue_HasGmPlusPreAuthorize() throws NoSuchMethodException {
        Method method = DashboardController.class.getMethod(
                "getRevenue", String.class, Authentication.class);
        assertGmPlusAnnotation(method, "GET /dashboard/revenue");
    }

    @Test
    void getOrders_HasGmPlusPreAuthorize() throws NoSuchMethodException {
        Method method = DashboardController.class.getMethod(
                "getOrders", String.class, Authentication.class);
        assertGmPlusAnnotation(method, "GET /dashboard/orders");
    }

    @Test
    void getProducts_HasGmPlusPreAuthorize() throws NoSuchMethodException {
        Method method = DashboardController.class.getMethod(
                "getProducts", Authentication.class);
        assertGmPlusAnnotation(method, "GET /dashboard/products (product-stats)");
    }

    @Test
    void getCategories_HasGmPlusPreAuthorize() throws NoSuchMethodException {
        Method method = DashboardController.class.getMethod(
                "getCategories", Authentication.class);
        assertGmPlusAnnotation(method, "GET /dashboard/categories (category-stats)");
    }

    @Test
    void getSalesChart_HasGmPlusPreAuthorize() throws NoSuchMethodException {
        Method method = DashboardController.class.getMethod(
                "getSalesChart", String.class, LocalDate.class, LocalDate.class, Authentication.class);
        assertGmPlusAnnotation(method, "GET /dashboard/sales-chart");
    }

    @Test
    void getBestPerformer_HasGmPlusPreAuthorize() throws NoSuchMethodException {
        Method method = DashboardController.class.getMethod(
                "getBestPerformer", String.class, Authentication.class);
        assertGmPlusAnnotation(method, "GET /dashboard/best-performer");
    }

    @Test
    void getRecentOrders_HasGmPlusPreAuthorize() throws NoSuchMethodException {
        Method method = DashboardController.class.getMethod(
                "getRecentOrders", int.class, Authentication.class);
        assertGmPlusAnnotation(method, "GET /dashboard/recent-orders");
    }

    // ── Verify GM_PLUS includes GENERAL_MANAGER and MANAGER authorities ───────

    @Test
    void gmPlus_IncludesGeneralManagerAuthority() {
        // GM_PLUS explicitly lists both cases as separate entries in hasAnyAuthority()
        // so that JWT authorities in either case are accepted (defensive explicit listing,
        // not relying on Spring Security's own case-sensitivity behaviour).
        assertTrue(RoleConstants.GM_PLUS.contains("'GENERAL_MANAGER'"),
                "GM_PLUS must include 'GENERAL_MANAGER' authority so CompanyRole.GENERAL_MANAGER users are accepted");
        assertTrue(RoleConstants.GM_PLUS.contains("'general_manager'"),
                "GM_PLUS must explicitly list lowercase 'general_manager' as a separate authority entry");
    }

    @Test
    void gmPlus_IncludesManagerAuthority() {
        // UserRole.MANAGER is the value mapped from CompanyRole.GENERAL_MANAGER at login.
        // GM_PLUS explicitly lists both cases as separate entries in hasAnyAuthority().
        assertTrue(RoleConstants.GM_PLUS.contains("'MANAGER'"),
                "GM_PLUS must include 'MANAGER' authority (UserRole.MANAGER mapped from CompanyRole.GENERAL_MANAGER)");
        assertTrue(RoleConstants.GM_PLUS.contains("'manager'"),
                "GM_PLUS must explicitly list lowercase 'manager' as a separate authority entry");
    }

    // ── Verify the redundant isGMPlusRole helper has been removed ─────────────

    @Test
    void dashboardController_HasNoIsGMPlusRoleMethod() {
        boolean hasRedundantHelper = false;
        for (Method m : DashboardController.class.getDeclaredMethods()) {
            if (m.getName().equals("isGMPlusRole")) {
                hasRedundantHelper = true;
                break;
            }
        }
        assertFalse(hasRedundantHelper,
                "DashboardController must not contain the redundant isGMPlusRole(User) helper method. "
                + "@PreAuthorize(RoleConstants.GM_PLUS) is the single authorization gate.");
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private void assertGmPlusAnnotation(Method method, String endpointDescription) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize,
                endpointDescription + " must have a @PreAuthorize annotation");
        assertEquals(RoleConstants.GM_PLUS, preAuthorize.value(),
                endpointDescription + " must use @PreAuthorize(RoleConstants.GM_PLUS)");
    }
}
