package com.pos.inventsight.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 20, message = "Phone number too long")
    private String phone;

    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
