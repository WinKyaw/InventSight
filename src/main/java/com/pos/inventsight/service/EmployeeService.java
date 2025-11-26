package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.Employee;
import com.pos.inventsight.model.sql.EmployeeStatus;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.EmployeeRelationship;
import com.pos.inventsight.repository.sql.EmployeeRepository;
import com.pos.inventsight.repository.sql.EmployeeRelationshipRepository;
import com.pos.inventsight.repository.sql.UserRepository;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.DuplicateResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EmployeeService {
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private EmployeeRelationshipRepository employeeRelationshipRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
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
    
    /**
     * Create employee with automatic user account creation and relationship tracking
     * @param employee The employee to create
     * @param employer The user who is creating this employee (employer)
     * @return The created employee with associated user account
     */
    public Employee createEmployeeWithUser(Employee employee, User employer) {
        System.out.println("👥 InventSight - Creating new employee with user account: " + employee.getFullName());
        System.out.println("👤 Created by employer: " + employer.getUsername());
        
        // Validate required fields
        if (employee.getEmail() == null || employee.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Employee email is required for user account creation");
        }
        
        if (employee.getStore() == null) {
            throw new IllegalArgumentException("Employee must be associated with a store");
        }
        
        if (employee.getCompany() == null) {
            throw new IllegalArgumentException("Employee must be associated with a company");
        }
        
        // Check for duplicate email in users table
        if (userRepository.existsByEmail(employee.getEmail())) {
            throw new DuplicateResourceException("User with email already exists: " + employee.getEmail());
        }
        
        // Check for duplicate email in employees table
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new DuplicateResourceException("Employee with email already exists: " + employee.getEmail());
        }
        
        // Generate password
        String password = generatePassword(employee.getCompany(), employee.getStore(), employee.getEmail());
        System.out.println("🔑 Password generated for employee account");
        
        // Create user account for employee
        User employeeUser = new User();
        employeeUser.setUsername(employee.getEmail()); // Use email as username
        employeeUser.setEmail(employee.getEmail());
        employeeUser.setPassword(passwordEncoder.encode(password));
        employeeUser.setFirstName(employee.getFirstName());
        employeeUser.setLastName(employee.getLastName());
        employeeUser.setPhone(employee.getPhoneNumber());
        employeeUser.setRole(UserRole.EMPLOYEE);
        employeeUser.setEmailVerified(true); // Auto-verify email
        employeeUser.setIsActive(true);
        employeeUser.setCreatedBy(employer.getUsername());
        employeeUser.setCreatedAt(LocalDateTime.now());
        employeeUser.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(employeeUser);
        System.out.println("✅ User account created for employee: " + savedUser.getEmail());
        
        // Link user to employee
        employee.setUser(savedUser);
        employee.setCreatedBy(employer.getUsername());
        employee.setCreatedAt(LocalDateTime.now());
        
        Employee savedEmployee = employeeRepository.save(employee);
        
        // Create employee relationship with IDs only
        EmployeeRelationship relationship = new EmployeeRelationship(
            savedEmployee.getId(),        // employee_id
            employer.getUuid(),            // employer_id (using UUID)
            employee.getStore().getId(),   // store_id
            employee.getCompany().getId(), // company_id
            employer.getUsername()         // created_by
        );
        employeeRelationshipRepository.save(relationship);
        System.out.println("✅ Employee relationship created");
        
        // Log activity
        activityLogService.logActivity(
            savedEmployee.getId().toString(),
            employer.getUsername(),
            "EMPLOYEE_WITH_USER_CREATED",
            "EMPLOYEE",
            "New employee with user account created: " + employee.getFullName() + " - " + employee.getTitle()
        );
        
        System.out.println("✅ InventSight employee created with user account: " + savedEmployee.getFullName());
        return savedEmployee;
    }
    
    /**
     * Generate password for employee user account
     * Default: companyName + storeName OR email + "12345" if null/empty
     * Ensures minimum password length of 8 characters
     */
    private String generatePassword(Company company, Store store, String email) {
        String companyName = company != null ? company.getName() : null;
        String storeName = store != null ? store.getStoreName() : null;
        
        // Check if both company and store names are available and not empty
        if (companyName != null && !companyName.trim().isEmpty() && 
            storeName != null && !storeName.trim().isEmpty()) {
            // Remove spaces and special characters, keep alphanumeric only
            String cleanCompanyName = companyName.replaceAll("[^a-zA-Z0-9]", "");
            String cleanStoreName = storeName.replaceAll("[^a-zA-Z0-9]", "");
            String password = cleanCompanyName + cleanStoreName;
            
            // Ensure minimum password length of 8 characters
            if (password.length() >= 8) {
                return password;
            }
        }
        
        // Fallback: use email + "12345" - ensures minimum 8 characters
        return email + "12345";
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