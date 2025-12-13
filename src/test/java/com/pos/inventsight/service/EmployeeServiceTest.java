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
        testEmployer.setId(UUID.randomUUID());
        testEmployer.setUsername("employer");
        testEmployer.setEmail("employer@test.com");
        testEmployer.setRole(UserRole.MANAGER);
        
        // Setup test employee (email will be auto-generated, no need to set it)
        testEmployee = new Employee();
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
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
            user.setId(UUID.randomUUID());
            return user;
        });
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(UUID.randomUUID());
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
        // Verify email was auto-generated in the expected format
        assertEquals("john.doe@inventsight.com", result.getEmail());
        assertEquals("john.doe@inventsight.com", result.getUser().getEmail());
        
        // Verify interactions
        verify(userRepository, atLeastOnce()).existsByEmail(anyString());
        verify(employeeRepository, atLeastOnce()).existsByEmail(anyString());
        verify(userRepository).save(any(User.class));
        verify(employeeRepository).save(any(Employee.class));
        verify(employeeRelationshipRepository).save(any(EmployeeRelationship.class));
        verify(activityLogService).logActivity(anyString(), anyString(), anyString(), anyString(), anyString());
    }
    
    @Test
    void testCreateEmployeeWithUser_DuplicateEmail_GeneratesSuffix() {
        // Arrange - first call returns true (email exists), second returns false
        when(userRepository.existsByEmail("john.doe@inventsight.com")).thenReturn(true);
        when(userRepository.existsByEmail("john.doe1@inventsight.com")).thenReturn(false);
        when(employeeRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(UUID.randomUUID());
            return emp;
        });
        when(employeeRelationshipRepository.save(any(EmployeeRelationship.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        Employee result = employeeService.createEmployeeWithUser(testEmployee, testEmployer);
        
        // Assert - should have suffix
        assertEquals("john.doe1@inventsight.com", result.getEmail());
        assertEquals("john.doe1@inventsight.com", result.getUser().getEmail());
        
        // Verify duplicate check was called
        verify(userRepository).existsByEmail("john.doe@inventsight.com");
        verify(userRepository).existsByEmail("john.doe1@inventsight.com");
    }
    
    @Test
    void testCreateEmployeeWithUser_DuplicateInEmployees_GeneratesSuffix() {
        // Arrange - first call returns true in employees table, second returns false
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(employeeRepository.existsByEmail("john.doe@inventsight.com")).thenReturn(true);
        when(employeeRepository.existsByEmail("john.doe1@inventsight.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(UUID.randomUUID());
            return emp;
        });
        when(employeeRelationshipRepository.save(any(EmployeeRelationship.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        Employee result = employeeService.createEmployeeWithUser(testEmployee, testEmployer);
        
        // Assert - should have suffix
        assertEquals("john.doe1@inventsight.com", result.getEmail());
        
        // Verify duplicate check was called on employee repository
        verify(employeeRepository).existsByEmail("john.doe@inventsight.com");
        verify(employeeRepository).existsByEmail("john.doe1@inventsight.com");
    }
    
    @Test
    void testCreateEmployeeWithUser_MissingFirstName() {
        // Arrange
        testEmployee.setFirstName(null);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> employeeService.createEmployeeWithUser(testEmployee, testEmployer));
    }
    
    @Test
    void testCreateEmployeeWithUser_MissingLastName() {
        // Arrange
        testEmployee.setLastName(null);
        
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
    void testPasswordGeneration_WithNewFormat() {
        // This test validates the password generation logic indirectly
        // by checking that the user is created with an encoded password
        
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(employeeRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> {
            String rawPassword = invocation.getArgument(0);
            // Verify password follows new format: Firstnamelastname123!
            assertEquals("Johndoe123!", rawPassword);
            return "encodedPassword";
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            // Verify password was set
            assertNotNull(user.getPassword());
            assertEquals("encodedPassword", user.getPassword());
            return user;
        });
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(UUID.randomUUID());
            return emp;
        });
        when(employeeRelationshipRepository.save(any(EmployeeRelationship.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        employeeService.createEmployeeWithUser(testEmployee, testEmployer);
        
        // Assert
        verify(passwordEncoder).encode("Johndoe123!");
    }
    
    @Test
    void testPasswordGeneration_WithSuffix() {
        // Test that password includes suffix when email has number
        
        // Arrange - simulate duplicate email so suffix is added
        when(userRepository.existsByEmail("john.doe@inventsight.com")).thenReturn(true);
        when(userRepository.existsByEmail("john.doe1@inventsight.com")).thenReturn(false);
        when(employeeRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> {
            String rawPassword = invocation.getArgument(0);
            // Verify password includes suffix: Johndoe1123!
            assertEquals("Johndoe1123!", rawPassword);
            return "encodedPassword";
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(UUID.randomUUID());
            return emp;
        });
        when(employeeRelationshipRepository.save(any(EmployeeRelationship.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        employeeService.createEmployeeWithUser(testEmployee, testEmployer);
        
        // Assert
        verify(passwordEncoder).encode("Johndoe1123!");
    }
    
    @Test
    void testEmailGeneration_SpecialCharactersRemoved() {
        // Test that special characters in names are removed
        
        // Arrange
        testEmployee.setFirstName("O'Brien");
        testEmployee.setLastName("Smith-Jones");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(employeeRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(UUID.randomUUID());
            return emp;
        });
        when(employeeRelationshipRepository.save(any(EmployeeRelationship.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        Employee result = employeeService.createEmployeeWithUser(testEmployee, testEmployer);
        
        // Assert - special characters should be removed
        assertEquals("obrien.smithjones@inventsight.com", result.getEmail());
    }
    
    @Test
    void testPasswordGeneration_SpecialCharactersRemoved() {
        // Test that special characters in names are removed from password
        
        // Arrange
        testEmployee.setFirstName("O'Brien");
        testEmployee.setLastName("Smith-Jones");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(employeeRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> {
            String rawPassword = invocation.getArgument(0);
            // Verify password has special characters removed and first letter capitalized
            assertEquals("Obriensmithjones123!", rawPassword);
            return "encodedPassword";
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(UUID.randomUUID());
            return emp;
        });
        when(employeeRelationshipRepository.save(any(EmployeeRelationship.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        employeeService.createEmployeeWithUser(testEmployee, testEmployer);
        
        // Assert
        verify(passwordEncoder).encode("Obriensmithjones123!");
    }
}
