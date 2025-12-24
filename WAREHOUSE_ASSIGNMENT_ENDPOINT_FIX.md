# Warehouse Assignment Endpoint Fix - Frontend Integration Guide

## 🎯 Overview

This document describes the fix for the warehouse assignment endpoint and provides guidance for frontend integration.

## ❌ Previous Issue

The endpoint `GET /warehouse-inventory/user/{userId}/warehouses` was receiving an **employee ID** from the frontend but treating it as a **user ID**, causing user lookup failures.

**Error Log:**
```
LOG  ⚠️ User not found: 40bf8193-f62e-4653-9fe6-c2373e701d9b, returning empty warehouse list
```

**Root Cause:**
- Frontend sent: `employeeId = 40bf8193-f62e-4653-9fe6-c2373e701d9b`
- Backend expected: `userId` parameter
- Backend looked for: `users.id = '40bf8193-...'` ❌ (Wrong!)
- Should have looked for: `employees.id = '40bf8193-...'` → `users.id = employee.user_id`

## ✅ Solution Implemented

### New Backend Endpoint

**Endpoint:** `GET /api/warehouse-inventory/employee/{employeeId}/warehouses`

**Path Parameters:**
- `employeeId` (UUID): The employee's ID from the `employees` table

**Response Format:**
```json
{
  "success": true,
  "employeeId": "40bf8193-f62e-4653-9fe6-c2373e701d9b",
  "employeeName": "Shawn Win",
  "userId": "05cda25e-f040-49a1-9c36-ad88e0bcd062",
  "username": "shawn.win@inventsight.com",
  "warehouses": [
    {
      "id": "permission-uuid",
      "warehouseId": "warehouse-uuid",
      "warehouseName": "Main Warehouse",
      "warehouseLocation": "New York, NY",
      "permissionType": "READ_WRITE",
      "grantedBy": "admin",
      "grantedAt": "2025-12-23T14:00:00",
      "isActive": true,
      "warehouse": {
        "id": "warehouse-uuid",
        "name": "Main Warehouse",
        "location": "New York, NY",
        "address": "123 Main St",
        "city": "New York",
        "state": "NY",
        "country": "USA"
      }
    }
  ],
  "count": 1
}
```

**Special Cases:**

1. **Employee without user account:**
```json
{
  "success": true,
  "employeeId": "40bf8193-f62e-4653-9fe6-c2373e701d9b",
  "employeeName": "John Doe",
  "userId": null,
  "username": null,
  "warehouses": [],
  "count": 0,
  "message": "Employee has no user account - no warehouses assigned"
}
```

2. **Employee not found:**
```json
{
  "success": false,
  "error": "Employee not found"
}
```

## 🔧 Frontend Changes Required

### File: `services/api/warehouse.ts` (or similar)

**Update the `getEmployeeWarehouses` method:**

```typescript
/**
 * Get employee's warehouse assignments with permissions
 * ✅ FIXED: Changed endpoint to use employeeId
 */
async getEmployeeWarehouses(employeeId: string): Promise<any[]> {
  try {
    console.log(`🏢 Fetching warehouse assignments for employee: ${employeeId}`);

    // ✅ FIXED: Changed from /user/{id}/warehouses 
    //           to /employee/{id}/warehouses
    const response = await apiClient.get(
      `/warehouse-inventory/employee/${employeeId}/warehouses`
    );

    console.log('🏢 Warehouse assignments response:', response.data);

    if (response.data.success) {
      const assignments = response.data.warehouses || [];
      console.log(
        `✅ Loaded ${assignments.length} warehouse assignments for employee ${response.data.employeeName}`
      );
      
      return assignments.map((assignment: any) => ({
        id: assignment.id,
        warehouseId: assignment.warehouseId || assignment.warehouse?.id,
        warehouseName: assignment.warehouseName || assignment.warehouse?.name,
        warehouseLocation: assignment.warehouseLocation || assignment.warehouse?.location,
        permissionType: assignment.permissionType,
        grantedBy: assignment.grantedBy,
        grantedAt: assignment.grantedAt,
        isActive: assignment.isActive,
        warehouse: assignment.warehouse,
      }));
    } else {
      console.warn('⚠️ API returned success: false');
      return [];
    }
  } catch (error: any) {
    console.error('❌ Error fetching employee warehouses:', error.message);
    return [];
  }
}
```

### Usage in Components

**No changes needed** if you're already passing `employeeId`:

```typescript
// In team.tsx - "Assign Warehouse" modal
const response = await WarehouseService.getEmployeeWarehouses(
  selectedEmployeeForDetails.id  // ✅ Already sending employee ID - Good!
);
```

## 🔐 Authorization Rules

1. **Self-Access:** Employees can view their own warehouse assignments
2. **GM+ Access:** Users with roles FOUNDER, CEO, or GENERAL_MANAGER can view any employee's assignments
3. **Restricted Access:** Regular employees cannot view other employees' assignments (returns 403 Forbidden)

## 🔄 Migration Path

### Old Endpoint (Still Available)
`GET /warehouse-inventory/user/{userId}/warehouses`
- Still functional for backward compatibility
- Returns empty list if user not found (graceful degradation)
- **Recommend migrating to new endpoint**

### New Endpoint (Recommended)
`GET /warehouse-inventory/employee/{employeeId}/warehouses`
- Correctly maps employee ID → user ID → warehouse permissions
- Handles all edge cases (no user account, employee not found)
- More semantically correct

## 🧪 Testing Checklist

After updating the frontend:

- [ ] Navigate to Team Management page
- [ ] Click "Assign Warehouse" for an employee
- [ ] Verify the modal displays employee's current warehouse assignments
- [ ] Check that each assignment shows:
  - Warehouse name and location
  - Permission type (Read-Only or Read/Write)
  - Who granted the permission
  - When it was granted
- [ ] Test with employee who has no user account (should show empty list with message)
- [ ] Test with GM+ user viewing other employee's assignments (should work)
- [ ] Test with regular employee trying to view another's assignments (should fail with 403)

## 📊 Data Flow

```
Frontend
   ↓ GET /warehouse-inventory/employee/{employeeId}/warehouses
   ↓
Backend: WarehouseInventoryController.getEmployeeWarehouseAssignments()
   ↓
1. Look up employee: SELECT * FROM employees WHERE id = {employeeId}
   ↓
2. Get user_id: employee.user_id
   ↓
3. Fetch permissions: SELECT * FROM warehouse_permissions WHERE user_id = {userId} AND is_active = true
   ↓
4. Map to response format
   ↓
Frontend receives warehouse assignments
```

## 🔍 Example Request/Response

**Request:**
```http
GET /api/warehouse-inventory/employee/40bf8193-f62e-4653-9fe6-c2373e701d9b/warehouses
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "employeeId": "40bf8193-f62e-4653-9fe6-c2373e701d9b",
  "employeeName": "Shawn Win",
  "userId": "05cda25e-f040-49a1-9c36-ad88e0bcd062",
  "username": "shawn.win@inventsight.com",
  "warehouses": [
    {
      "id": "perm-123",
      "warehouseId": "wh-456",
      "warehouseName": "Main Warehouse",
      "warehouseLocation": "New York, NY",
      "permissionType": "READ_WRITE",
      "grantedBy": "admin",
      "grantedAt": "2025-12-23T14:00:00",
      "isActive": true,
      "warehouse": {
        "id": "wh-456",
        "name": "Main Warehouse",
        "location": "New York, NY",
        "address": "123 Main St",
        "city": "New York",
        "state": "NY",
        "country": "USA"
      }
    }
  ],
  "count": 1
}
```

## 📝 Summary

**Backend Changes:**
- ✅ Added new endpoint: `/employee/{employeeId}/warehouses`
- ✅ Correctly maps employee ID → user ID → warehouse permissions
- ✅ Handles edge cases gracefully
- ✅ Maintains backward compatibility with old endpoint
- ✅ Comprehensive test coverage

**Frontend Changes Needed:**
- Update endpoint URL from `/user/{id}/warehouses` to `/employee/{id}/warehouses`
- No other changes required (already sending employee ID)

**Result:** 
The "User not found" error will be resolved, and warehouse assignments will load correctly! 🎉
