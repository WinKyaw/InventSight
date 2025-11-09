package com.pos.inventsight.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MfaSetupResponse {
    private boolean success;
    private String message;
    private MfaSetupData data;
    
    public MfaSetupResponse() {}
    
    public MfaSetupResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public MfaSetupResponse(boolean success, String message, MfaSetupData data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public MfaSetupData getData() { return data; }
    public void setData(MfaSetupData data) { this.data = data; }
    
    public static class MfaSetupData {
        private String secret;
        private String qrCodeUrl;
        private String qrCodeImage; // Base64 encoded PNG image
        
        public MfaSetupData() {}
        
        public MfaSetupData(String secret, String qrCodeUrl, String qrCodeImage) {
            this.secret = secret;
            this.qrCodeUrl = qrCodeUrl;
            this.qrCodeImage = qrCodeImage;
        }
        
        // Getters and Setters
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        
        public String getQrCodeUrl() { return qrCodeUrl; }
        public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }
        
        public String getQrCodeImage() { return qrCodeImage; }
        public void setQrCodeImage(String qrCodeImage) { this.qrCodeImage = qrCodeImage; }
    }
}
