package com.pos.inventsight.dto;

import java.util.UUID;

/**
 * User response DTO that matches frontend expectations
 * Contains basic user information without sensitive data
 */
public class UserResponse {
    private UUID id;
    private String email;
    private String name;  // Full name for display
    private String firstName;
    private String lastName;
    private String username;
    private String role;
    
    public UserResponse(UUID id, String email, String firstName, String lastName, 
                       String username, String role) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = firstName + " " + lastName; // Combine for display name
        this.username = username;
        this.role = role;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { 
        this.firstName = firstName;
        updateName();
    }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { 
        this.lastName = lastName;
        updateName();
    }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    private void updateName() {
        if (firstName != null && lastName != null) {
            this.name = firstName + " " + lastName;
        }
    }
}