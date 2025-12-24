# Frontend Integration Guide

## New Endpoint Available

The backend now provides the endpoint that the frontend was trying to call:

**Endpoint:** `GET /api/warehouse-inventory/user/{userId}/warehouses`

## Frontend Changes Required

If the frontend is in a separate repository, update the warehouse service to use this new endpoint.

### File to Update: `services/api/warehouse.ts`

Update the `getEmployeeWarehouses` method:

```typescript
/**
 * Get employee's warehouse assignments with permissions
 */
async getEmployeeWarehouses(employeeId: string): Promise<any[]> {
  try {
    console.log(`🏢 Fetching warehouses for employee: ${employeeId}`);

    // ✅ FIXED: Use the correct endpoint
    const response = await apiClient.get(
      `/warehouse-inventory/user/${employeeId}/warehouses`
    );

    console.log('🏢 Warehouse assignments response:', response.data);

    if (response.data.success) {
      const assignments = response.data.warehouses || [];
      console.log(`✅ Loaded ${assignments.length} warehouse assignments`);
      return assignments;
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

## Response Format

The endpoint returns:

```json
{
  "success": true,
  "userId": "user-uuid",
  "username": "username",
  "warehouses": [
    {
      "id": "permission-uuid",
      "warehouseId": "warehouse-uuid",
      "warehouseName": "Warehouse Name",
      "warehouseLocation": "Location",
      "permissionType": "READ_WRITE" | "READ",
      "grantedBy": "admin",
      "grantedAt": "2025-12-23T14:00:00",
      "isActive": true,
      "warehouse": {
        "id": "warehouse-uuid",
        "name": "Warehouse Name",
        "location": "Location",
        "address": "123 Main St",
        "city": "City",
        "state": "State",
        "country": "Country"
      }
    }
  ],
  "count": 1
}
```

## Authorization

- Users can view their own warehouse assignments
- GM+ roles (FOUNDER, CEO, GENERAL_MANAGER) can view any user's assignments
- Regular employees cannot view other users' assignments (returns 403)

## Testing

After updating the frontend:

1. Navigate to Team Management page
2. Click "Assign Warehouse" for an employee
3. The modal should now display the employee's current warehouse assignments
4. Each assignment shows:
   - Warehouse name and location
   - Permission type (Read-Only or Read/Write)
   - Who granted the permission
   - When it was granted
