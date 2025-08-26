package com.pos.inventsight.controller;

import com.pos.inventsight.model.sql.Employee;
import com.pos.inventsight.service.EmployeeService;
import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.EmployeeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employees")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EmployeeController {
    
    @Autowired
    private EmployeeService employeeService;
    
    // Get all active employees
    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        System.out.println("👥 InventSight - Fetching all active employees");
        System.out.println("📅 Current Date and Time (UTC): 2025-08-26 09:12:40");
        System.out.println("👤 Current User's Login: WinKyaw");
        
        List<Employee> employees = employeeService.getAllActiveEmployees();
        System.out.println("✅ InventSight retrieved " + employees.size() + " active employees");
        
        return ResponseEntity.ok(employees);
    }
    
    // Get employee by ID
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        System.out.println("👤 InventSight - Fetching employee with ID: " + id);
        
        Employee employee = employeeService.getEmployeeById(id);
        System.out.println("✅ Employee found: " + employee.getFullName() + " - " + employee.getTitle());
        
        return ResponseEntity.ok(employee);
    }
    
    // Get checked-in employees
    @GetMapping("/checked-in")
    public ResponseEntity<List<Employee>> getCheckedInEmployees() {
        System.out.println("🕐 InventSight - Fetching checked-in employees");
        
        List<Employee> checkedInEmployees = employeeService.getCheckedInEmployees();
        System.out.println("✅ Found " + checkedInEmployees.size() + " checked-in employees");
        
        return ResponseEntity.ok(checkedInEmployees);
    }
    
    // Search employees
    @GetMapping("/search")
    public ResponseEntity<List<Employee>> searchEmployees(@RequestParam String query) {
        System.out.println("🔍 InventSight - Searching employees: " + query);
        
        List<Employee> employees = employeeService.searchEmployees(query);
        System.out.println("✅ Found " + employees.size() + " employees matching: " + query);
        
        return ResponseEntity.ok(employees);
    }
    
    // Create new employee
    @PostMapping
    public ResponseEntity<?> createEmployee(@Valid @RequestBody EmployeeRequest employeeRequest, 
                                          Authentication authentication) {
        System.out.println("➕ InventSight - Creating new employee: " + employeeRequest.getFirstName() + " " + employeeRequest.getLastName());
        System.out.println("👤 Created by: WinKyaw");
        
        try {
            Employee employee = new Employee(
                employeeRequest.getFirstName(),
                employeeRequest.getLastName(),
                employeeRequest.getEmail(),
                employeeRequest.getTitle(),
                employeeRequest.getHourlyRate()
            );
            
            employee.setPhoneNumber(employeeRequest.getPhoneNumber());
            employee.setDepartment(employeeRequest.getDepartment());
            if (employeeRequest.getBonus() != null) {
                employee.setBonus(employeeRequest.getBonus());
            }
            
            Employee createdEmployee = employeeService.createEmployee(employee);
            System.out.println("✅ InventSight employee created: " + createdEmployee.getFullName());
            
            return ResponseEntity.ok(createdEmployee);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight failed to create employee: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    // Check-in employee
    @PostMapping("/{id}/check-in")
    public ResponseEntity<?> checkInEmployee(@PathVariable Long id, Authentication authentication) {
        System.out.println("🕐 InventSight - Employee check-in request for ID: " + id);
        System.out.println("👤 Processed by: WinKyaw");
        
        try {
            Employee checkedInEmployee = employeeService.checkInEmployee(id);
            System.out.println("✅ InventSight employee checked in: " + checkedInEmployee.getFullName());
            
            return ResponseEntity.ok(checkedInEmployee);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight check-in failed: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    // Check-out employee
    @PostMapping("/{id}/check-out")
    public ResponseEntity<?> checkOutEmployee(@PathVariable Long id, Authentication authentication) {
        System.out.println("🕐 InventSight - Employee check-out request for ID: " + id);
        System.out.println("👤 Processed by: WinKyaw");
        
        try {
            Employee checkedOutEmployee = employeeService.checkOutEmployee(id);
            System.out.println("✅ InventSight employee checked out: " + checkedOutEmployee.getFullName());
            
            return ResponseEntity.ok(checkedOutEmployee);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight check-out failed: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    // Get employee statistics
    @GetMapping("/statistics")
    public ResponseEntity<EmployeeStatistics> getEmployeeStatistics() {
        System.out.println("📊 InventSight - Generating employee statistics");
        
        long activeEmployees = employeeService.getActiveEmployeeCount();
        long checkedInEmployees = employeeService.getCheckedInEmployeeCount();
        List<String> titles = employeeService.getAllTitles();
        List<String> departments = employeeService.getAllDepartments();
        
        EmployeeStatistics stats = new EmployeeStatistics(
            activeEmployees,
            checkedInEmployees,
            titles,
            departments,
            "2025-08-26 09:12:40",
            "WinKyaw"
        );
        
        System.out.println("✅ InventSight employee statistics generated: " + activeEmployees + " active, " + checkedInEmployees + " checked in");
        return ResponseEntity.ok(stats);
    }
    
    // Employee Statistics DTO
    public static class EmployeeStatistics {
        private long activeEmployees;
        private long checkedInEmployees;
        private List<String> titles;
        private List<String> departments;
        private String currentDateTime;
        private String currentUser;
        
        public EmployeeStatistics(long activeEmployees, long checkedInEmployees, List<String> titles,
                                List<String> departments, String currentDateTime, String currentUser) {
            this.activeEmployees = activeEmployees;
            this.checkedInEmployees = checkedInEmployees;
            this.titles = titles;
            this.departments = departments;
            this.currentDateTime = currentDateTime;
            this.currentUser = currentUser;
        }
        
        // Getters
        public long getActiveEmployees() { return activeEmployees; }
        public long getCheckedInEmployees() { return checkedInEmployees; }
        public List<String> getTitles() { return titles; }
        public List<String> getDepartments() { return departments; }
        public String getCurrentDateTime() { return currentDateTime; }
        public String getCurrentUser() { return currentUser; }
    }
}