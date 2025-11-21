package com.pos.inventsight.model.sql;

/**
 * Permission types for one-time permissions
 */
public enum PermissionType {
    ADD_ITEM("Add Item Permission"),
    EDIT_ITEM("Edit Item Permission"),
    DELETE_ITEM("Delete Item Permission");
    
    private final String displayName;
    
    PermissionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
