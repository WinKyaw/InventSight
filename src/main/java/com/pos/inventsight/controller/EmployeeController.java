package com.pos.inventsight.controller;

import com.pos.inventsight.model.sql.Employee;
import com.pos.inventsight.service.EmployeeService;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.EmployeeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/employees")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EmployeeController {
    
    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private UserService userService;
    
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
    
    // PUT /employees/{id} - Update employee information
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, 
                                          @Valid @RequestBody EmployeeRequest employeeRequest,
                                          Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            System.out.println("✏️ InventSight - Updating employee ID: " + id + " by user: " + username);
            
            Employee existingEmployee = employeeService.getEmployeeById(id);
            
            // Check if user can update this employee (admin or self)
            if (!currentUser.getRole().name().equals("ADMIN") && 
                existingEmployee.getUser() != null && 
                !existingEmployee.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied: You can only update your own profile or admin required"));
            }
            
            // Update employee fields
            existingEmployee.setFirstName(employeeRequest.getFirstName());
            existingEmployee.setLastName(employeeRequest.getLastName());
            existingEmployee.setEmail(employeeRequest.getEmail());
            existingEmployee.setTitle(employeeRequest.getTitle());
            existingEmployee.setHourlyRate(employeeRequest.getHourlyRate());
            existingEmployee.setPhoneNumber(employeeRequest.getPhoneNumber());
            existingEmployee.setDepartment(employeeRequest.getDepartment());
            
            if (employeeRequest.getBonus() != null) {
                existingEmployee.setBonus(employeeRequest.getBonus());
            }
            
            Employee updatedEmployee = employeeService.updateEmployee(existingEmployee);
            
            System.out.println("✅ InventSight employee updated: " + updatedEmployee.getFullName());
            return ResponseEntity.ok(updatedEmployee);
            
        } catch (Exception e) {
            System.err.println("❌ Error updating employee: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Error updating employee: " + e.getMessage()));
        }
    }
    
    // DELETE /employees/{id} - Deactivate employee (admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateEmployee(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🗑️ InventSight - Deactivating employee ID: " + id + " by admin: " + username);
            
            Employee employee = employeeService.getEmployeeById(id);
            employeeService.deactivateEmployee(id);
            
            System.out.println("✅ InventSight employee deactivated: " + employee.getFullName());
            return ResponseEntity.ok(new ApiResponse(true, "Employee deactivated successfully"));
            
        } catch (Exception e) {
            System.err.println("❌ Error deactivating employee: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Employee not found with ID: " + id));
        }
    }
    
    // PUT /employees/{id}/role - Update employee role (admin only)
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEmployeeRole(@PathVariable Long id, 
                                              @RequestBody Map<String, String> roleRequest,
                                              Authentication authentication) {
        try {
            String username = authentication.getName();
            String newRole = roleRequest.get("role");
            
            System.out.println("👑 InventSight - Updating employee role ID: " + id + " to: " + newRole + " by admin: " + username);
            
            if (newRole == null || newRole.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Role is required"));
            }
            
            Employee employee = employeeService.getEmployeeById(id);
            Employee updatedEmployee = employeeService.updateEmployeeRole(id, newRole);
            
            System.out.println("✅ InventSight employee role updated: " + employee.getFullName() + " -> " + newRole);
            return ResponseEntity.ok(updatedEmployee);
            
        } catch (Exception e) {
            System.err.println("❌ Error updating employee role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Error updating employee role: " + e.getMessage()));
        }
    }
    
    // GET /employees/me - Get current employee profile
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentEmployeeProfile(Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            System.out.println("👤 InventSight - Fetching current employee profile for user: " + username);
            
            Employee employee = employeeService.getEmployeeByUserId(currentUser.getId());
            
            if (employee == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Employee profile not found for current user"));
            }
            
            // Create enhanced employee profile response
            Map<String, Object> profileResponse = new HashMap<>();
            profileResponse.put("employee", employee);
            profileResponse.put("user", currentUser);
            profileResponse.put("permissions", List.of(currentUser.getRole().name()));
            profileResponse.put("isAdmin", currentUser.getRole().name().equals("ADMIN"));
            profileResponse.put("isManager", currentUser.getRole().name().equals("MANAGER"));
            
            System.out.println("✅ InventSight current employee profile retrieved: " + employee.getFullName());
            return ResponseEntity.ok(profileResponse);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching current employee profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching employee profile: " + e.getMessage()));
        }
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