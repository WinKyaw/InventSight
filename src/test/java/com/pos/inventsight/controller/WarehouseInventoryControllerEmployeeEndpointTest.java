package com.pos.inventsight.controller;

import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.Employee;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.Warehouse;
import com.pos.inventsight.model.sql.WarehousePermission;
import com.pos.inventsight.repository.sql.WarehousePermissionRepository;
import com.pos.inventsight.service.EmployeeService;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.WarehouseInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test class for WarehouseInventoryController's getEmployeeWarehouseAssignments endpoint
 * Tests the new endpoint that accepts employeeId and maps to userId
 */
public class WarehouseInventoryControllerEmployeeEndpointTest {

    @Mock
    private UserService userService;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private WarehousePermissionRepository warehousePermissionRepository;

    @Mock
    private WarehouseInventoryService warehouseInventoryService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private WarehouseInventoryController controller;

    private UUID employeeId;
    private UUID userId;
    private UUID currentUserId;
    private User currentUser;
    private User targetUser;
    private Employee employee;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        employeeId = UUID.fromString("40bf8193-f62e-4653-9fe6-c2373e701d9b");
        userId = UUID.fromString("05cda25e-f040-49a1-9c36-ad88e0bcd062");
        currentUserId = UUID.randomUUID();
        
        currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setUsername("test-gm");
        currentUser.setEmail("gm@test.com");
        
        targetUser = new User();
        targetUser.setId(userId);
        targetUser.setUsername("shawn.win@inventsight.com");
        targetUser.setEmail("shawn.win@inventsight.com");
        
        employee = new Employee();
        employee.setId(employeeId);
        employee.setFirstName("Shawn");
        employee.setLastName("Win");
        employee.setEmail("shawn.win@inventsight.com");
        employee.setUser(targetUser);
    }

    /**
     * Test that getEmployeeWarehouseAssignments correctly maps employee ID to user ID
     * and returns warehouse permissions
     */
    @Test
    public void testGetEmployeeWarehouseAssignments_Success() {
        // Setup: GM+ user trying to view assignments for employee
        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(employeeService.getEmployeeById(employeeId)).thenReturn(employee);
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.GENERAL_MANAGER);
        
        // Create a warehouse permission
        Warehouse warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setName("Main Warehouse");
        warehouse.setLocation("New York");
        warehouse.setAddress("123 Main St");
        warehouse.setCity("New York");
        warehouse.setState("NY");
        warehouse.setCountry("USA");
        
        WarehousePermission permission = new WarehousePermission();
        permission.setId(UUID.randomUUID());
        permission.setWarehouse(warehouse);
        permission.setUser(targetUser);
        permission.setPermissionType(WarehousePermission.PermissionType.READ_WRITE);
        permission.setGrantedBy("admin");
        permission.setGrantedAt(LocalDateTime.now());
        permission.setIsActive(true);
        
        when(warehousePermissionRepository.findByUserIdAndIsActive(userId, true))
            .thenReturn(Collections.singletonList(permission));
        
        // Execute
        ResponseEntity<?> response = controller.getEmployeeWarehouseAssignments(employeeId, authentication);
        
        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return 200 OK");
        
        // Verify response body structure
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"), "Success should be true");
        assertEquals(employeeId, body.get("employeeId"), "Should include the employeeId");
        assertEquals("Shawn Win", body.get("employeeName"), "Should include employee name");
        assertEquals(userId, body.get("userId"), "Should include the correct userId");
        assertEquals("shawn.win@inventsight.com", body.get("username"), "Username should match");
        assertEquals(1, body.get("count"), "Count should be 1");
        
        // Verify warehouses list
        Object warehousesObj = body.get("warehouses");
        assertNotNull(warehousesObj);
        assertTrue(warehousesObj instanceof List, "Warehouses should be a list");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> warehouses = (List<Map<String, Object>>) warehousesObj;
        assertEquals(1, warehouses.size(), "Should have 1 warehouse");
        
        Map<String, Object> assignment = warehouses.get(0);
        assertEquals("Main Warehouse", assignment.get("warehouseName"));
        assertEquals("READ_WRITE", assignment.get("permissionType"));
    }

    /**
     * Test that endpoint returns 404 when employee doesn't exist
     */
    @Test
    public void testGetEmployeeWarehouseAssignments_EmployeeNotFound() {
        // Setup: Employee doesn't exist - throw ResourceNotFoundException
        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(employeeService.getEmployeeById(employeeId))
            .thenThrow(new ResourceNotFoundException("Employee not found with ID: " + employeeId));
        
        // Execute
        ResponseEntity<?> response = controller.getEmployeeWarehouseAssignments(employeeId, authentication);
        
        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should return 404");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertTrue(((String)body.get("error")).contains("Employee not found"));
    }

    /**
     * Test that endpoint handles employee without user account gracefully
     */
    @Test
    public void testGetEmployeeWarehouseAssignments_EmployeeWithoutUser() {
        // Setup: Employee exists but has no user account
        Employee employeeWithoutUser = new Employee();
        employeeWithoutUser.setId(employeeId);
        employeeWithoutUser.setFirstName("John");
        employeeWithoutUser.setLastName("Doe");
        employeeWithoutUser.setEmail("john.doe@inventsight.com");
        employeeWithoutUser.setUser(null); // No user account
        
        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(employeeService.getEmployeeById(employeeId)).thenReturn(employeeWithoutUser);
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.GENERAL_MANAGER);
        
        // Execute
        ResponseEntity<?> response = controller.getEmployeeWarehouseAssignments(employeeId, authentication);
        
        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return 200 OK");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"), "Success should be true");
        assertEquals(employeeId, body.get("employeeId"));
        assertEquals("John Doe", body.get("employeeName"));
        assertNull(body.get("userId"), "userId should be null");
        assertNull(body.get("username"), "username should be null");
        assertEquals(0, body.get("count"), "Count should be 0");
        
        // Verify warehouses is an empty list
        Object warehousesObj = body.get("warehouses");
        assertNotNull(warehousesObj);
        assertTrue(warehousesObj instanceof List, "Warehouses should be a list");
        @SuppressWarnings("unchecked")
        List<Object> warehouses = (List<Object>) warehousesObj;
        assertTrue(warehouses.isEmpty(), "Warehouses list should be empty");
        
        // Verify helpful message is included
        assertTrue(body.containsKey("message"), "Should include a message");
        String message = (String) body.get("message");
        assertTrue(message.contains("no user account"), "Message should mention no user account");
    }

    /**
     * Test authorization: regular user can view their own assignments
     */
    @Test
    public void testGetEmployeeWarehouseAssignments_SelfAccess() {
        // Setup: User viewing their own employee record
        currentUser.setId(userId); // Same as targetUser
        
        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(employeeService.getEmployeeById(employeeId)).thenReturn(employee);
        when(warehousePermissionRepository.findByUserIdAndIsActive(userId, true))
            .thenReturn(Collections.emptyList());
        
        // Execute
        ResponseEntity<?> response = controller.getEmployeeWarehouseAssignments(employeeId, authentication);
        
        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return 200 OK for self-access");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
    }

    /**
     * Test authorization: regular user cannot view other employees' assignments
     */
    @Test
    public void testGetEmployeeWarehouseAssignments_UnauthorizedAccess() {
        // Setup: Regular user trying to view another employee's assignments
        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(employeeService.getEmployeeById(employeeId)).thenReturn(employee);
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.EMPLOYEE);
        
        // Execute
        ResponseEntity<?> response = controller.getEmployeeWarehouseAssignments(employeeId, authentication);
        
        // Verify
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should return 403 Forbidden");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertTrue(((String)body.get("error")).contains("only view your own"));
    }
}
