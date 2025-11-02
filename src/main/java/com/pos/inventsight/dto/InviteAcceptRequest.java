package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InviteAcceptRequest {
    
    @NotBlank(message = "Invite token is required")
    private String inviteToken;
    
    @Size(min = 8, max = 120, message = "Password must be between 8 and 120 characters")
    private String password;
    
    // Constructors
    public InviteAcceptRequest() {}
    
    public InviteAcceptRequest(String inviteToken, String password) {
        this.inviteToken = inviteToken;
        this.password = password;
    }
    
    // Getters and Setters
    public String getInviteToken() { return inviteToken; }
    public void setInviteToken(String inviteToken) { this.inviteToken = inviteToken; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    @Override
    public String toString() {
        return "InviteAcceptRequest{" +
                "inviteToken='" + (inviteToken != null ? "[PROVIDED]" : "null") + '\'' +
                ", password='" + (password != null ? "[PROTECTED]" : "null") + '\'' +
                '}';
    }
}
