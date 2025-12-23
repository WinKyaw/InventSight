# Warehouse Permission-Based Visibility Implementation

## Overview

This document describes the implementation of permission-based visibility for warehouse operations, specifically for Add/Withdraw buttons in the warehouse inventory management interface.

## Backend Implementation

### New API Endpoint

**Endpoint:** `GET /api/warehouse-inventory/warehouse/{warehouseId}/permissions`

**Purpose:** Check if the current authenticated user has READ/WRITE permissions on a specific warehouse.

**Authentication Required:** Yes (JWT Bearer token)

### Request

```http
GET /api/warehouse-inventory/warehouse/{warehouseId}/permissions HTTP/1.1
Host: api.inventsight.com
Authorization: Bearer {jwt_token}
```

**Path Parameters:**
- `warehouseId` (UUID, required): The ID of the warehouse to check permissions for

### Response

**Success Response (200 OK):**

```json
{
  "success": true,
  "warehouseId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "userId": "8b1234c5-6789-4abc-d012-3456789abcde",
  "username": "john.doe",
  "role": "EMPLOYEE",
  "permissions": {
    "canRead": true,
    "canWrite": true,
    "canAddInventory": true,
    "canWithdrawInventory": true,
    "isGMPlus": false
  }
}
```

**Error Response (500 Internal Server Error):**

```json
{
  "success": false,
  "error": "Warehouse not found"
}
```

### Permission Logic

The endpoint implements the following permission hierarchy:

#### 1. OWNER (UserRole Level)
- **Permissions:** Full access (READ + WRITE)
- **Description:** Users with the OWNER role have unrestricted access to all warehouses
- **Use Case:** System administrators, company owners

#### 2. FOUNDER/CEO/GENERAL_MANAGER (CompanyRole Level)
- **Permissions:** Full access (READ + WRITE)
- **Condition:** Must be in the same company as the warehouse
- **Description:** Top-level management roles within the company
- **Use Case:** Company executives, general managers

#### 3. STORE_MANAGER (CompanyRole Level)
- **Permissions:** READ + WRITE
- **Condition:** Must be in the same company as the warehouse
- **Description:** Store managers can manage inventory in their company's warehouses
- **Use Case:** Store managers, warehouse supervisors

#### 4. EMPLOYEE (CompanyRole Level)
- **Permissions:** READ only
- **Condition:** Must be in the same company as the warehouse
- **Description:** Employees can view inventory but cannot modify it
- **Use Case:** Regular employees, cashiers

#### 5. No Company Association
- **Permissions:** None (no READ or WRITE)
- **Description:** Users not associated with the warehouse's company have no access
- **Use Case:** Users from different companies

### Permission Matrix

| Role / Company Role | Can View | Can Add | Can Withdraw | Can See All Transactions |
|---------------------|----------|---------|--------------|--------------------------|
| **OWNER** | ✅ | ✅ | ✅ | ✅ |
| **FOUNDER** (same company) | ✅ | ✅ | ✅ | ✅ |
| **CEO** (same company) | ✅ | ✅ | ✅ | ✅ |
| **GENERAL_MANAGER** (same company) | ✅ | ✅ | ✅ | ✅ |
| **STORE_MANAGER** (same company) | ✅ | ✅ | ✅ | ❌ |
| **EMPLOYEE** (same company) | ✅ | ❌ | ❌ | ❌ |
| **Different Company** | ❌ | ❌ | ❌ | ❌ |

## Implementation Details

### Controller Method

Location: `src/main/java/com/pos/inventsight/controller/WarehouseInventoryController.java`

```java
@GetMapping("/warehouse/{warehouseId}/permissions")
public ResponseEntity<?> checkWarehousePermissions(
    @PathVariable UUID warehouseId,
    Authentication authentication
) {
    // Implementation details...
}
```

### Helper Methods

#### hasWarehousePermission

Checks if a user has a specific permission (READ or WRITE) on a warehouse.

```java
private boolean hasWarehousePermission(User user, Warehouse warehouse, String permissionType) {
    // 1. Check OWNER role
    // 2. Check company membership
    // 3. Check company role
    // 4. Return permission based on role and permission type
}
```

#### isGMPlusRole

Determines if a user has GM+ level permissions (FOUNDER, CEO, or GENERAL_MANAGER).

```java
private boolean isGMPlusRole(User user) {
    CompanyRole role = warehouseInventoryService.getUserCompanyRole(user);
    return role == CompanyRole.FOUNDER || 
           role == CompanyRole.CEO || 
           role == CompanyRole.GENERAL_MANAGER;
}
```

## Frontend Integration

### Frontend Implementation (Separate Repository Required)

The frontend should call this endpoint when loading a warehouse view to determine which buttons to display.

### Example TypeScript/React Native Integration

```typescript
// services/api/warehouse.ts
async checkWarehousePermissions(warehouseId: string): Promise<any> {
  try {
    const response = await apiClient.get(
      `/warehouse-inventory/warehouse/${warehouseId}/permissions`
    );
    return response.data;
  } catch (error) {
    console.error('Error checking permissions:', error);
    return {
      success: false,
      permissions: {
        canRead: false,
        canWrite: false,
        canAddInventory: false,
        canWithdrawInventory: false,
        isGMPlus: false,
      },
    };
  }
}
```

### UI Component Integration

```typescript
// app/(tabs)/warehouse.tsx
const [permissions, setPermissions] = useState({
  canAddInventory: false,
  canWithdrawInventory: false,
  canWrite: false,
});

useEffect(() => {
  const loadPermissions = async () => {
    if (!selectedWarehouse) return;
    
    const response = await WarehouseService.checkWarehousePermissions(
      selectedWarehouse.id
    );
    
    if (response.success) {
      setPermissions(response.permissions);
    }
  };
  
  loadPermissions();
}, [selectedWarehouse]);

// Render buttons based on permissions
{permissions.canAddInventory && (
  <Button onPress={handleAddInventory}>Add Inventory</Button>
)}

{permissions.canWithdrawInventory && (
  <Button onPress={handleWithdraw}>Withdraw</Button>
)}

{!permissions.canWrite && (
  <Text>🔒 You have read-only access to this warehouse</Text>
)}
```

## Testing

### Unit Tests

Location: `src/test/java/com/pos/inventsight/controller/WarehousePermissionsEndpointTest.java`

**Test Coverage:**
- ✅ OWNER with full permissions
- ✅ GENERAL_MANAGER with full permissions  
- ✅ STORE_MANAGER with write permissions
- ✅ EMPLOYEE with read-only permissions
- ✅ User from different company with no permissions
- ✅ Error handling for non-existent warehouse

**Test Results:** All 6 tests passing ✅

### Manual Testing

Use the provided test script: `/tmp/test-warehouse-permissions.sh`

```bash
# Example manual test
curl -X GET \
  'http://localhost:8080/api/warehouse-inventory/warehouse/{warehouseId}/permissions' \
  -H 'Authorization: Bearer {jwt_token}'
```

## Security

### Security Analysis

- ✅ **CodeQL Scan:** 0 vulnerabilities found
- ✅ **Authentication:** JWT token required for all requests
- ✅ **Authorization:** Company-based access control
- ✅ **Data Validation:** Warehouse existence checked
- ✅ **Error Handling:** Graceful error responses

### Security Best Practices

1. **Authentication Required:** All requests must include a valid JWT token
2. **Company Isolation:** Users can only check permissions for warehouses in their company
3. **Role-Based Access:** Permissions determined by user's role and company membership
4. **Error Messages:** Generic error messages to prevent information disclosure

## Deployment

### Prerequisites

1. Spring Boot 3.5.5+
2. PostgreSQL database
3. Existing authentication system (JWT)
4. Multi-tenant architecture configured

### Configuration

No additional configuration required. The endpoint is automatically available when the application starts.

### Database Requirements

The implementation uses existing tables:
- `users`
- `warehouses`
- `company_store_user`
- `companies`

## Troubleshooting

### Common Issues

1. **401 Unauthorized**
   - **Cause:** Missing or invalid JWT token
   - **Solution:** Ensure valid JWT token is included in Authorization header

2. **500 Internal Server Error - "Warehouse not found"**
   - **Cause:** Invalid warehouse ID
   - **Solution:** Verify warehouse ID exists in database

3. **No permissions returned**
   - **Cause:** User not associated with warehouse's company
   - **Solution:** Verify user's company membership

## Future Enhancements

Potential improvements for future releases:

1. **Warehouse-Specific Permissions Table**
   - Add dedicated table for explicit warehouse permissions
   - Allow granular permission assignment per user per warehouse

2. **Permission Caching**
   - Cache permission results to reduce database queries
   - Implement cache invalidation on role changes

3. **Audit Logging**
   - Log permission checks for security auditing
   - Track permission changes over time

4. **Batch Permission Checks**
   - Add endpoint to check permissions for multiple warehouses
   - Optimize for list views with many warehouses

## References

- **Problem Statement:** See GitHub issue for original requirements
- **Controller:** `src/main/java/com/pos/inventsight/controller/WarehouseInventoryController.java`
- **Tests:** `src/test/java/com/pos/inventsight/controller/WarehousePermissionsEndpointTest.java`
- **Role Constants:** `src/main/java/com/pos/inventsight/security/RoleConstants.java`

## Changelog

### Version 1.0.0 (2025-12-23)

- ✅ Initial implementation of warehouse permissions endpoint
- ✅ Comprehensive test suite (6 tests)
- ✅ Security scan completed
- ✅ Documentation created

---

**Implementation Status:** ✅ Complete (Backend Only)

**Frontend Status:** ⚠️ Requires separate implementation in frontend repository
