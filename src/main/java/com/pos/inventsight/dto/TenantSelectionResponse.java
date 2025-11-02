package com.pos.inventsight.dto;

import java.time.LocalDateTime;
import java.util.List;

public class TenantSelectionResponse {
    
    private String error;
    private String message;
    private List<MemberCompany> companies;
    private LocalDateTime timestamp;
    
    public static class MemberCompany {
        private String companyId;
        private String displayName;
        private String role;
        private Boolean isActive;
        
        public MemberCompany() {}
        
        public MemberCompany(String companyId, String displayName, String role, Boolean isActive) {
            this.companyId = companyId;
            this.displayName = displayName;
            this.role = role;
            this.isActive = isActive;
        }
        
        // Getters and Setters
        public String getCompanyId() { return companyId; }
        public void setCompanyId(String companyId) { this.companyId = companyId; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
    
    // Constructors
    public TenantSelectionResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public TenantSelectionResponse(String error, String message, List<MemberCompany> companies) {
        this.error = error;
        this.message = message;
        this.companies = companies;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public List<MemberCompany> getCompanies() { return companies; }
    public void setCompanies(List<MemberCompany> companies) { this.companies = companies; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
