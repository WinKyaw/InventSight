package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.EmployeeRelationshipRepository;
import com.pos.inventsight.repository.sql.EmployeeRepository;
import com.pos.inventsight.repository.sql.UserRepository;
import com.pos.inventsight.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmployeeService - focusing on the new user creation functionality
 */
@ExtendWith(MockitoExtension.class)
public class EmployeeServiceTest {
    
    @Mock
    private EmployeeRepository employeeRepository;
    
    @Mock
    private EmployeeRelationshipRepository employeeRelationshipRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private ActivityLogService activityLogService;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private EmployeeService employeeService;
    
    private Company testCompany;
    private Store testStore;
    private User testEmployer;
    private Employee testEmployee;
    
    @BeforeEach
    void setUp() {
        // Setup test company
        testCompany = new Company();
        testCompany.setId(UUID.randomUUID());
        testCompany.setName("TestCompany");
        testCompany.setEmail("company@test.com");
        
        // Setup test store
        testStore = new Store();
        testStore.setId(UUID.randomUUID());
        testStore.setStoreName("TestStore");
        testStore.setCompany(testCompany);
        
        // Setup test employer (user creating the employee)
        testEmployer = new User();
        testEmployer.setId(1L);
        testEmployer.setUsername("employer");
        testEmployer.setEmail("employer@test.com");
        testEmployer.setRole(UserRole.MANAGER);
        
        // Setup test employee
        testEmployee = new Employee();
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
        testEmployee.setEmail("john.doe@test.com");
        testEmployee.setTitle("Cashier");
        testEmployee.setHourlyRate(new BigDecimal("15.00"));
        testEmployee.setCompany(testCompany);
        testEmployee.setStore(testStore);
    }
    
    @Test
    void testCreateEmployeeWithUser_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(employeeRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(1L);
            return emp;
        });
        when(employeeRelationshipRepository.save(any(EmployeeRelationship.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        Employee result = employeeService.createEmployeeWithUser(testEmployee, testEmployer);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.getUser());
        assertEquals(UserRole.EMPLOYEE, result.getUser().getRole());
        assertTrue(result.getUser().getEmailVerified());
        assertEquals(testEmployee.getEmail(), result.getUser().getEmail());
        
        // Verify interactions
        verify(userRepository).existsByEmail(testEmployee.getEmail());
        verify(employeeRepository).existsByEmail(testEmployee.getEmail());
        verify(userRepository).save(any(User.class));
        verify(employeeRepository).save(any(Employee.class));
        verify(employeeRelationshipRepository).save(any(EmployeeRelationship.class));
        verify(activityLogService).logActivity(anyString(), anyString(), anyString(), anyString(), anyString());
    }
    
    @Test
    void testCreateEmployeeWithUser_DuplicateEmailInUsers() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        
        // Act & Assert
        assertThrows(DuplicateResourceException.class, 
            () -> employeeService.createEmployeeWithUser(testEmployee, testEmployer));
        
        // Verify no user or employee was saved
        verify(userRepository, never()).save(any(User.class));
        verify(employeeRepository, never()).save(any(Employee.class));
    }
    
    @Test
    void testCreateEmployeeWithUser_DuplicateEmailInEmployees() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(employeeRepository.existsByEmail(anyString())).thenReturn(true);
        
        // Act & Assert
        assertThrows(DuplicateResourceException.class, 
            () -> employeeService.createEmployeeWithUser(testEmployee, testEmployer));
        
        // Verify no user or employee was saved
        verify(userRepository, never()).save(any(User.class));
        verify(employeeRepository, never()).save(any(Employee.class));
    }
    
    @Test
    void testCreateEmployeeWithUser_MissingEmail() {
        // Arrange
        testEmployee.setEmail(null);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> employeeService.createEmployeeWithUser(testEmployee, testEmployer));
    }
    
    @Test
    void testCreateEmployeeWithUser_MissingStore() {
        // Arrange
        testEmployee.setStore(null);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> employeeService.createEmployeeWithUser(testEmployee, testEmployer));
    }
    
    @Test
    void testCreateEmployeeWithUser_MissingCompany() {
        // Arrange
        testEmployee.setCompany(null);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> employeeService.createEmployeeWithUser(testEmployee, testEmployer));
    }
    
    @Test
    void testPasswordGeneration_WithCompanyAndStoreName() {
        // This test validates the password generation logic indirectly
        // by checking that the user is created with an encoded password
        
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(employeeRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            // Verify password was set
            assertNotNull(user.getPassword());
            assertEquals("encodedPassword", user.getPassword());
            return user;
        });
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(1L);
            return emp;
        });
        when(employeeRelationshipRepository.save(any(EmployeeRelationship.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        employeeService.createEmployeeWithUser(testEmployee, testEmployer);
        
        // Assert
        verify(passwordEncoder).encode(anyString());
    }
}
