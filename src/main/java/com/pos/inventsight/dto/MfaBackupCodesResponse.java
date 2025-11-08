package com.pos.inventsight.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MfaBackupCodesResponse {
    private boolean success;
    private String message;
    private MfaBackupCodesData data;
    
    public MfaBackupCodesResponse() {}
    
    public MfaBackupCodesResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public MfaBackupCodesResponse(boolean success, String message, MfaBackupCodesData data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public MfaBackupCodesData getData() { return data; }
    public void setData(MfaBackupCodesData data) { this.data = data; }
    
    public static class MfaBackupCodesData {
        private List<String> backupCodes;
        
        public MfaBackupCodesData() {}
        
        public MfaBackupCodesData(List<String> backupCodes) {
            this.backupCodes = backupCodes;
        }
        
        // Getters and Setters
        public List<String> getBackupCodes() { return backupCodes; }
        public void setBackupCodes(List<String> backupCodes) { this.backupCodes = backupCodes; }
    }
}
