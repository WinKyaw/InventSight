package com.pos.inventsight.dto;

public class ApiResponse {
    private Boolean success;
    private String message;
    private String system = "InventSight System";
    private String timestamp;
    private Object data;
    
    public ApiResponse(Boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = "2025-08-26 09:04:35";
    }
    
    public ApiResponse(Boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = "2025-08-26 09:04:35";
    }
    
    public ApiResponse(Boolean success, String message, String timestamp) {
        this.success = success;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getSystem() { return system; }
    public void setSystem(String system) { this.system = system; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}