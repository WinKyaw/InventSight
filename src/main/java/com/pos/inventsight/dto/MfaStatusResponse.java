package com.pos.inventsight.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MfaStatusResponse {
    private boolean success;
    private String message;
    private MfaStatusData data;
    
    public MfaStatusResponse() {}
    
    public MfaStatusResponse(boolean success, String message, MfaStatusData data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public MfaStatusData getData() { return data; }
    public void setData(MfaStatusData data) { this.data = data; }
    
    public static class MfaStatusData {
        private boolean enabled;
        
        public MfaStatusData() {}
        
        public MfaStatusData(boolean enabled) {
            this.enabled = enabled;
        }
        
        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
