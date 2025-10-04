package com.pos.inventsight.dto;

public class SubscriptionInfoResponse {
    private String plan;
    private Integer maxCompanies;
    private Long currentUsage;
    private Integer remaining;
    
    public SubscriptionInfoResponse() {
    }
    
    public SubscriptionInfoResponse(String plan, Integer maxCompanies, Long currentUsage, Integer remaining) {
        this.plan = plan;
        this.maxCompanies = maxCompanies;
        this.currentUsage = currentUsage;
        this.remaining = remaining;
    }
    
    // Getters and Setters
    public String getPlan() {
        return plan;
    }
    
    public void setPlan(String plan) {
        this.plan = plan;
    }
    
    public Integer getMaxCompanies() {
        return maxCompanies;
    }
    
    public void setMaxCompanies(Integer maxCompanies) {
        this.maxCompanies = maxCompanies;
    }
    
    public Long getCurrentUsage() {
        return currentUsage;
    }
    
    public void setCurrentUsage(Long currentUsage) {
        this.currentUsage = currentUsage;
    }
    
    public Integer getRemaining() {
        return remaining;
    }
    
    public void setRemaining(Integer remaining) {
        this.remaining = remaining;
    }
}
