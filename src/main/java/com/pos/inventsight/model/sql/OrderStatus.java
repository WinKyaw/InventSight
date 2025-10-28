package com.pos.inventsight.model.sql;

/**
 * Enum representing the lifecycle status of a sales order
 */
public enum OrderStatus {
    DRAFT("Draft"),
    SUBMITTED("Submitted"),
    PENDING_MANAGER_APPROVAL("Pending Manager Approval"),
    CONFIRMED("Confirmed"),
    CANCEL_REQUESTED("Cancel Requested"),
    CANCELLED("Cancelled"),
    FULFILLED("Fulfilled");
    
    private final String displayName;
    
    OrderStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if order can be modified (add/remove items)
     */
    public boolean isModifiable() {
        return this == DRAFT;
    }
    
    /**
     * Check if order can be submitted
     */
    public boolean canSubmit() {
        return this == DRAFT;
    }
    
    /**
     * Check if order can be cancelled directly (without manager approval)
     */
    public boolean canCancelDirectly() {
        return this == DRAFT || this == SUBMITTED;
    }
    
    /**
     * Check if order requires manager action
     */
    public boolean requiresManagerAction() {
        return this == PENDING_MANAGER_APPROVAL || this == CANCEL_REQUESTED;
    }
}
