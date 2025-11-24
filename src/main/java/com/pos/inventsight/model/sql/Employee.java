package com.pos.inventsight.model.sql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class Employee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "first_name")
    private String firstName;
    
    @NotBlank
    @Column(name = "last_name")
    private String lastName;
    
    @Email
    @Column(unique = true)
    private String email;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @NotBlank
    private String title;
    
    @NotNull
    @DecimalMin("0.0")
    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;
    
    @DecimalMin("0.0")
    @Column(name = "bonus", precision = 10, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Enumerated(EnumType.STRING)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;
    
    // Multi-tenancy support
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @JsonIgnoreProperties({"company", "employees", "hibernateLazyInitializer", "handler"})
    private Store store;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @JsonIgnoreProperties({"stores", "employees", "hibernateLazyInitializer", "handler"})
    private Company company;
    
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;
    
    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;
    
    @Column(name = "is_checked_in")
    private Boolean isCheckedIn = false;
    
    @Column(name = "total_hours_worked", precision = 10, scale = 2)
    private BigDecimal totalHoursWorked = BigDecimal.ZERO;
    
    @Column(name = "department")
    private String department;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "created_by")
    private String createdBy = "WinKyaw";
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;
    
    // Constructors
    public Employee() {}
    
    public Employee(String firstName, String lastName, String email, String title, BigDecimal hourlyRate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.title = title;
        this.hourlyRate = hourlyRate;
        this.startDate = LocalDate.now();
    }
    
    // Business Logic Methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public BigDecimal getTotalCompensation() {
        // Assuming 40 hours per week, 52 weeks per year
        BigDecimal annualSalary = hourlyRate.multiply(new BigDecimal(40 * 52));
        return annualSalary.add(bonus);
    }
    
    public void checkIn() {
        this.checkInTime = LocalDateTime.now();
        this.isCheckedIn = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void checkOut() {
        this.checkOutTime = LocalDateTime.now();
        this.isCheckedIn = false;
        this.updatedAt = LocalDateTime.now();
        
        // Calculate hours worked for this session
        if (checkInTime != null && checkOutTime != null) {
            // Simple calculation - in production, use proper duration calculation
            // This is a simplified version
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public String getCheckInStatus() {
        if (isCheckedIn && checkInTime != null) {
            return checkInTime.toLocalTime().toString();
        }
        return "Not checked in";
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    
    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal bonus) { this.bonus = bonus; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public EmployeeStatus getStatus() { return status; }
    public void setStatus(EmployeeStatus status) { this.status = status; }
    
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    
    public Boolean getIsCheckedIn() { return isCheckedIn; }
    public void setIsCheckedIn(Boolean isCheckedIn) { this.isCheckedIn = isCheckedIn; }
    
    public BigDecimal getTotalHoursWorked() { return totalHoursWorked; }
    public void setTotalHoursWorked(BigDecimal totalHoursWorked) { this.totalHoursWorked = totalHoursWorked; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}