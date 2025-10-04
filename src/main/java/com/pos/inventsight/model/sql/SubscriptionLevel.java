package com.pos.inventsight.model.sql;

public enum SubscriptionLevel {
    FREE(1, "Free"),
    PRO(3, "Pro"),
    BUSINESS(10, "Business"),
    ENTERPRISE(-1, "Enterprise"); // -1 represents unlimited
    
    private final int maxCompanies;
    private final String displayName;
    
    SubscriptionLevel(int maxCompanies, String displayName) {
        this.maxCompanies = maxCompanies;
        this.displayName = displayName;
    }
    
    public int getMaxCompanies() {
        return maxCompanies;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isUnlimited() {
        return maxCompanies == -1;
    }
}
