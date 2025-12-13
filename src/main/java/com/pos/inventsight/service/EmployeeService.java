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
     * Generate unique email for employee in format: firstname.lastname@inventsight.com
     * If duplicate exists, append number: firstname.lastname1@inventsight.com
     * 
     * @param firstName Employee's first name
     * @param lastName Employee's last name
     * @return Unique email address
     */
    private String generateUniqueEmail(String firstName, String lastName) {
        // Convert to lowercase and remove special characters
        String cleanFirstName = firstName.toLowerCase().trim().replaceAll("[^a-z]", "");
        String cleanLastName = lastName.toLowerCase().trim().replaceAll("[^a-z]", "");
        
        String baseEmail = cleanFirstName + "." + cleanLastName + "@inventsight.com";
        String email = baseEmail;
        int suffix = 1;
        
        // Check for duplicates in both users and employees tables
        while (userRepository.existsByEmail(email) || employeeRepository.existsByEmail(email)) {
            email = cleanFirstName + "." + cleanLastName + suffix + "@inventsight.com";
            suffix++;
        }
        
        System.out.println("📧 Generated unique email: " + email);
        return email;
    }
    
    /**
     * Generate password for employee in format: Firstnamelastname123!
     * If email has suffix (e.g., john.white1), password becomes: Johnwhite1123!
     * 
     * @param firstName Employee's first name
     * @param lastName Employee's last name
     * @param email Generated email (to extract suffix if exists)
     * @return Generated password
     */
    private String generateEmployeePassword(String firstName, String lastName, String email) {
        // Clean and remove special characters
        String cleanFirstName = firstName.trim().replaceAll("[^a-zA-Z]", "");
        String cleanLastName = lastName.toLowerCase().trim().replaceAll("[^a-z]", "");
        
        // Validate cleaned first name is not empty
        if (cleanFirstName.isEmpty()) {
            throw new IllegalArgumentException("First name must contain at least one letter");
        }
        
        // Capitalize first letter of first name, rest lowercase
        String formattedFirstName = cleanFirstName.substring(0, 1).toUpperCase() + 
                                    cleanFirstName.substring(1).toLowerCase();
        
        // Extract suffix number from email if exists (e.g., john.white1@... → 1)
        String suffix = "";
        String emailPrefix = email.split("@")[0]; // Get part before @
        
        // Check if email ends with a number (e.g., john.white1)
        if (emailPrefix.matches(".*\\d+$")) {
            // Extract the trailing digits
            suffix = emailPrefix.replaceAll(".*?(\\d+)$", "$1");
        }
        
        // Format: Firstnamelastname123! (capital F, rest lowercase, ends with numbers and !)
        String password = formattedFirstName + cleanLastName + suffix + "123!";
        System.out.println("🔑 Generated password format: " + formattedFirstName + cleanLastName + suffix + "123!");
        
        return password;
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
        if (employee.getFirstName() == null || employee.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("Employee first name is required");
        }
        
        if (employee.getLastName() == null || employee.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Employee last name is required");
        }
        
        if (employee.getStore() == null) {
            throw new IllegalArgumentException("Employee must be associated with a store");
        }
        
        if (employee.getCompany() == null) {
            throw new IllegalArgumentException("Employee must be associated with a company");
        }
        
        // Generate unique email in format: firstname.lastname@inventsight.com
        String generatedEmail = generateUniqueEmail(employee.getFirstName(), employee.getLastName());
        employee.setEmail(generatedEmail);
        System.out.println("📧 Employee email: " + generatedEmail);
        
        // Generate password in format: firstnamelastname123! (with suffix if email has number)
        String generatedPassword = generateEmployeePassword(
            employee.getFirstName(), 
            employee.getLastName(), 
            generatedEmail
        );
        
        // Create user account for employee
        User employeeUser = new User();
        employeeUser.setUsername(generatedEmail); // Use generated email as username
        employeeUser.setEmail(generatedEmail);
        employeeUser.setPassword(passwordEncoder.encode(generatedPassword));
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
            savedEmployee.getId(),         // employee_id (using UUID)
            employer.getId(),              // employer_id (using UUID)
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
            "New employee with user account created: " + employee.getFullName() + 
            " - Email: " + generatedEmail + " - " + employee.getTitle()
        );
        
        System.out.println("✅ InventSight employee created with user account: " + savedEmployee.getFullName());
        return savedEmployee;
    }
    
    public Employee updateEmployee(UUID employeeId, Employee employeeUpdates) {
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
    
    public Employee getEmployeeById(UUID employeeId) {
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
    public Employee checkInEmployee(UUID employeeId) {
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
    
    public Employee checkOutEmployee(UUID employeeId) {
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
    public void deactivateEmployee(UUID employeeId) {
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
    public Employee updateEmployeeRole(UUID employeeId, String newRole) {
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
    public Employee getEmployeeByUserId(UUID userId) {
        return employeeRepository.findByUserId(userId)
                .orElse(null);
    }
}