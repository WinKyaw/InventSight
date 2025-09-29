package com.pos.inventsight.dto;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper with data payload
 */
public class GenericApiResponse<T> {
    private Boolean success;
    private String message;
    private T data;
    private String system = "InventSight System";
    private LocalDateTime timestamp;
    
    public GenericApiResponse(Boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
    
    public GenericApiResponse(Boolean success, String message) {
        this(success, message, null);
    }
    
    // Getters and Setters
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    
    public String getSystem() { return system; }
    public void setSystem(String system) { this.system = system; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}