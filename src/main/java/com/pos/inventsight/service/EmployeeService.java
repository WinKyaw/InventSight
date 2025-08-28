package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.Employee;
import com.pos.inventsight.model.sql.EmployeeStatus;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.repository.sql.EmployeeRepository;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.DuplicateResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class EmployeeService {
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @Autowired
    private UserService userService;
    
    // CRUD Operations
    public Employee createEmployee(Employee employee) {
        System.out.println("👥 InventSight - Creating new employee: " + employee.getFullName());
        System.out.println("📅 Current Date and Time (UTC): 2025-08-26 09:12:40");
        System.out.println("👤 Current User's Login: WinKyaw");
        
        // Check for duplicate email
        if (employee.getEmail() != null && employeeRepository.existsByEmail(employee.getEmail())) {
            throw new DuplicateResourceException("Employee with email already exists: " + employee.getEmail());
        }
        
        employee.setCreatedBy("WinKyaw");
        employee.setCreatedAt(LocalDateTime.now());
        
        Employee savedEmployee = employeeRepository.save(employee);
        
        // Log activity
        activityLogService.logActivity(
            null,
            "WinKyaw",
            "EMPLOYEE_CREATED",
            "EMPLOYEE",
            "New employee added: " + employee.getFullName() + " - " + employee.getTitle()
        );
        
        System.out.println("✅ InventSight employee created: " + savedEmployee.getFullName());
        return savedEmployee;
    }
    
    public Employee updateEmployee(Long employeeId, Employee employeeUpdates) {
        Employee existingEmployee = getEmployeeById(employeeId);
        
        // Update fields
        if (employeeUpdates.getFirstName() != null) {
            existingEmployee.setFirstName(employeeUpdates.getFirstName());
        }
        if (employeeUpdates.getLastName() != null) {
            existingEmployee.setLastName(employeeUpdates.getLastName());
        }
        if (employeeUpdates.getEmail() != null) {
            existingEmployee.setEmail(employeeUpdates.getEmail());
        }
        if (employeeUpdates.getPhoneNumber() != null) {
            existingEmployee.setPhoneNumber(employeeUpdates.getPhoneNumber());
        }
        if (employeeUpdates.getTitle() != null) {
            existingEmployee.setTitle(employeeUpdates.getTitle());
        }
        if (employeeUpdates.getHourlyRate() != null) {
            existingEmployee.setHourlyRate(employeeUpdates.getHourlyRate());
        }
        if (employeeUpdates.getBonus() != null) {
            existingEmployee.setBonus(employeeUpdates.getBonus());
        }
        if (employeeUpdates.getDepartment() != null) {
            existingEmployee.setDepartment(employeeUpdates.getDepartment());
        }
        
        existingEmployee.setUpdatedAt(LocalDateTime.now());
        
        Employee updatedEmployee = employeeRepository.save(existingEmployee);
        
        // Log activity
        activityLogService.logActivity(
            null,
            "WinKyaw",
            "EMPLOYEE_UPDATED",
            "EMPLOYEE",
            "Employee profile updated: " + updatedEmployee.getFullName()
        );
        
        return updatedEmployee;
    }
    
    public Employee getEmployeeById(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));
    }
    
    public List<Employee> getAllActiveEmployees() {
        return employeeRepository.findByStatus(EmployeeStatus.ACTIVE);
    }
    
    public List<Employee> getCheckedInEmployees() {
        return employeeRepository.findByIsCheckedInTrue();
    }
    
    public List<Employee> searchEmployees(String searchTerm) {
        return employeeRepository.searchEmployees(searchTerm);
    }
    
    // Check-in/Check-out Operations
    public Employee checkInEmployee(Long employeeId) {
        Employee employee = getEmployeeById(employeeId);
        
        if (employee.getIsCheckedIn()) {
            throw new IllegalStateException("Employee is already checked in: " + employee.getFullName());
        }
        
        employee.checkIn();
        Employee checkedInEmployee = employeeRepository.save(employee);
        
        // Log activity
        activityLogService.logActivity(
            employeeId.toString(),
            "WinKyaw",
            "EMPLOYEE_CHECK_IN",
            "EMPLOYEE",
            "Employee checked in: " + employee.getFullName() + " at " + employee.getCheckInTime()
        );
        
        System.out.println("✅ InventSight employee checked in: " + checkedInEmployee.getFullName());
        return checkedInEmployee;
    }
    
    public Employee checkOutEmployee(Long employeeId) {
        Employee employee = getEmployeeById(employeeId);
        
        if (!employee.getIsCheckedIn()) {
            throw new IllegalStateException("Employee is not checked in: " + employee.getFullName());
        }
        
        employee.checkOut();
        Employee checkedOutEmployee = employeeRepository.save(employee);
        
        // Log activity
        activityLogService.logActivity(
            employeeId.toString(),
            "WinKyaw",
            "EMPLOYEE_CHECK_OUT",
            "EMPLOYEE",
            "Employee checked out: " + employee.getFullName() + " at " + employee.getCheckOutTime()
        );
        
        System.out.println("✅ InventSight employee checked out: " + checkedOutEmployee.getFullName());
        return checkedOutEmployee;
    }
    
    // Statistics
    public long getActiveEmployeeCount() {
        return employeeRepository.countActiveEmployees();
    }
    
    public long getCheckedInEmployeeCount() {
        return employeeRepository.countCheckedInEmployees();
    }
    
    public List<String> getAllTitles() {
        return employeeRepository.findAllTitles();
    }
    
    public List<String> getAllDepartments() {
        return employeeRepository.findAllDepartments();
    }
    
    // Business Operations
    public void deactivateEmployee(Long employeeId) {
        Employee employee = getEmployeeById(employeeId);
        employee.setStatus(EmployeeStatus.INACTIVE);
        employee.setUpdatedAt(LocalDateTime.now());
        
        employeeRepository.save(employee);
        
        // Log activity
        activityLogService.logActivity(
            employeeId.toString(),
            "WinKyaw",
            "EMPLOYEE_DEACTIVATED",
            "EMPLOYEE",
            "Employee deactivated: " + employee.getFullName()
        );
    }
    
    // Update employee with entity parameter
    public Employee updateEmployee(Employee employee) {
        employee.setUpdatedAt(LocalDateTime.now());
        Employee updatedEmployee = employeeRepository.save(employee);
        
        // Log activity
        activityLogService.logActivity(
            employee.getId().toString(),
            "WinKyaw",
            "EMPLOYEE_UPDATED",
            "EMPLOYEE",
            "Employee updated: " + employee.getFullName()
        );
        
        return updatedEmployee;
    }
    
    // Update employee role
    public Employee updateEmployeeRole(Long employeeId, String newRole) {
        Employee employee = getEmployeeById(employeeId);
        
        // If employee has associated user, update user role
        if (employee.getUser() != null) {
            User user = employee.getUser();
            try {
                UserRole role = UserRole.valueOf(newRole.toUpperCase());
                user.setRole(role);
                userService.updateUser(user.getId(), user);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + newRole);
            }
        }
        
        employee.setUpdatedAt(LocalDateTime.now());
        Employee updatedEmployee = employeeRepository.save(employee);
        
        // Log activity
        activityLogService.logActivity(
            employeeId.toString(),
            "WinKyaw",
            "EMPLOYEE_ROLE_UPDATED",
            "EMPLOYEE",
            "Employee role updated: " + employee.getFullName() + " -> " + newRole
        );
        
        return updatedEmployee;
    }
    
    // Get employee by user ID
    public Employee getEmployeeByUserId(Long userId) {
        return employeeRepository.findByUserId(userId)
                .orElse(null);
    }
}