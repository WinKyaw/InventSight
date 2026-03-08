package com.pos.inventsight.dto;

import java.util.List;

/**
 * Response DTO describing the current user's capabilities and allowed API paths.
 * Used by the frontend to skip API calls that the user is not permitted to make,
 * preventing unnecessary 403 errors.
 */
public class UserCapabilitiesResponse {
    private String role;
    private boolean isGMPlus;
    private boolean canViewDashboard;
    private boolean canViewReports;
    private boolean canManageTeam;
    private boolean canManageWarehouse;
    private boolean canManageInventory;
    private boolean canViewReceipts;
    private boolean canCreateReceipts;
    private List<String> allowedApiPaths;

    public UserCapabilitiesResponse() {}

    // Getters and Setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isGMPlus() { return isGMPlus; }
    public void setGMPlus(boolean isGMPlus) { this.isGMPlus = isGMPlus; }

    public boolean isCanViewDashboard() { return canViewDashboard; }
    public void setCanViewDashboard(boolean canViewDashboard) { this.canViewDashboard = canViewDashboard; }

    public boolean isCanViewReports() { return canViewReports; }
    public void setCanViewReports(boolean canViewReports) { this.canViewReports = canViewReports; }

    public boolean isCanManageTeam() { return canManageTeam; }
    public void setCanManageTeam(boolean canManageTeam) { this.canManageTeam = canManageTeam; }

    public boolean isCanManageWarehouse() { return canManageWarehouse; }
    public void setCanManageWarehouse(boolean canManageWarehouse) { this.canManageWarehouse = canManageWarehouse; }

    public boolean isCanManageInventory() { return canManageInventory; }
    public void setCanManageInventory(boolean canManageInventory) { this.canManageInventory = canManageInventory; }

    public boolean isCanViewReceipts() { return canViewReceipts; }
    public void setCanViewReceipts(boolean canViewReceipts) { this.canViewReceipts = canViewReceipts; }

    public boolean isCanCreateReceipts() { return canCreateReceipts; }
    public void setCanCreateReceipts(boolean canCreateReceipts) { this.canCreateReceipts = canCreateReceipts; }

    public List<String> getAllowedApiPaths() { return allowedApiPaths; }
    public void setAllowedApiPaths(List<String> allowedApiPaths) { this.allowedApiPaths = allowedApiPaths; }
}
