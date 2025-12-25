package com.pos.inventsight.model.sql;

/**
 * Permission types for role-based access control
 */
public enum PermissionType {
    /** Permission to add new items to inventory */
    ADD_ITEM("Add Item Permission"),
    
    /** Permission to edit existing items */
    EDIT_ITEM("Edit Item Permission"),
    
    /** Permission to delete items */
    DELETE_ITEM("Delete Item Permission"),
    
    /** 
     * Permission to manage supply and predefined items
     * Allows access to:
     * - Predefined items catalog
     * - CSV import/export
     * - Bulk operations
     * - Item templates
     * 
     * Typically granted to GM+ roles and Supply Management Specialists
     */
    MANAGE_SUPPLY("Manage Supply Permission");
    
    private final String displayName;
    
    PermissionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
