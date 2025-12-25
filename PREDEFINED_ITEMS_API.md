# Predefined Items Management System - API Documentation

## Overview

The Predefined Items Management System provides a comprehensive catalog management solution for GM+ users and Supply Management Specialists to maintain master item templates.

## Authentication & Authorization

All endpoints require authentication. Authorization is granted to:
- **GM+ Users**: FOUNDER, CEO, GENERAL_MANAGER, or OWNER role
- **Supply Management Specialists**: Users with active supply management permissions

## Base URLs

- Predefined Items: `/api/predefined-items`
- Supply Management: `/api/supply-management`

---

## Predefined Items Endpoints

### 1. List Predefined Items
**GET** `/api/predefined-items?companyId={uuid}`

Query Parameters:
- `companyId` (required): Company UUID
- `category` (optional): Filter by category
- `search` (optional): Search by name
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)

Response:
```json
{
  "success": true,
  "message": "Items retrieved successfully",
  "data": {
    "items": [
      {
        "id": "uuid",
        "name": "Apples",
        "sku": "APL-001",
        "category": "Fruits",
        "unitType": "lb",
        "description": "Fresh Red Apples",
        "defaultPrice": 2.99,
        "companyId": "uuid",
        "companyName": "Company Name",
        "isActive": true,
        "createdAt": "2025-01-15T10:30:00",
        "updatedAt": "2025-01-15T10:30:00",
        "createdBy": "user@example.com"
      }
    ],
    "totalElements": 100,
    "totalPages": 5,
    "currentPage": 0,
    "pageSize": 20
  }
}
```

### 2. Create Predefined Item
**POST** `/api/predefined-items?companyId={uuid}`

Request Body:
```json
{
  "name": "Apples",
  "sku": "APL-001",
  "category": "Fruits",
  "unitType": "lb",
  "description": "Fresh Red Apples",
  "defaultPrice": 2.99
}
```

Response: Returns created item with ID and timestamps

### 3. Update Predefined Item
**PUT** `/api/predefined-items/{id}?companyId={uuid}`

Request Body: Same as create

### 4. Delete Predefined Item (Soft Delete)
**DELETE** `/api/predefined-items/{id}?companyId={uuid}`

Marks item as inactive without removing from database

### 5. Bulk Create Items
**POST** `/api/predefined-items/bulk-create?companyId={uuid}`

Request Body:
```json
[
  {
    "name": "Apples",
    "sku": "APL-001",
    "category": "Fruits",
    "unittype": "lb",
    "description": "Fresh Red Apples",
    "defaultprice": "2.99"
  },
  {
    "name": "Bananas",
    "sku": "BAN-001",
    "category": "Fruits",
    "unittype": "lb",
    "description": "Fresh Bananas",
    "defaultprice": "1.49"
  }
]
```

Response:
```json
{
  "success": true,
  "message": "Bulk create completed",
  "data": {
    "total": 2,
    "successful": 2,
    "failed": 0,
    "errors": [],
    "createdItems": [...]
  }
}
```

### 6. Import from CSV
**POST** `/api/predefined-items/import-csv?companyId={uuid}`

Form Data:
- `file`: CSV file upload

CSV Format:
```csv
name,sku,category,unitType,description,defaultPrice
Apples,APL-001,Fruits,lb,Fresh Red Apples,2.99
Bananas,BAN-001,Fruits,lb,Fresh Bananas,1.49
```

Required Headers: `name`, `unitType`
Optional Headers: `sku`, `category`, `description`, `defaultPrice`

Response: Same as bulk create

### 7. Export to CSV
**GET** `/api/predefined-items/export-csv?companyId={uuid}`

Downloads CSV file with all active items

### 8. Get Associated Stores
**GET** `/api/predefined-items/{id}/stores?companyId={uuid}`

Returns list of stores associated with the item

### 9. Associate Stores
**POST** `/api/predefined-items/{id}/stores?companyId={uuid}`

Request Body:
```json
{
  "locationIds": ["store-uuid-1", "store-uuid-2"]
}
```

### 10. Get Associated Warehouses
**GET** `/api/predefined-items/{id}/warehouses?companyId={uuid}`

Returns list of warehouses associated with the item

### 11. Associate Warehouses
**POST** `/api/predefined-items/{id}/warehouses?companyId={uuid}`

Request Body:
```json
{
  "locationIds": ["warehouse-uuid-1", "warehouse-uuid-2"]
}
```

---

## Supply Management Permissions Endpoints

### 1. Grant Permission
**POST** `/api/supply-management/permissions`

Request Body:
```json
{
  "userId": "uuid",
  "companyId": "uuid",
  "isPermanent": true,
  "expiresAt": null,
  "notes": "Supply chain specialist"
}
```

**Authorization**: GM+ only

### 2. Get User Permissions
**GET** `/api/supply-management/permissions/user/{userId}`

Returns all active permissions for a user

### 3. Get Company Permissions
**GET** `/api/supply-management/permissions/company/{companyId}`

Returns all active permissions for a company

**Authorization**: GM+ only

### 4. Revoke Permission
**DELETE** `/api/supply-management/permissions/{permissionId}`

Revokes a supply management permission

**Authorization**: GM+ only

### 5. Check Current User Permission
**GET** `/api/supply-management/check?companyId={uuid}`

Response:
```json
{
  "success": true,
  "message": "Permission check completed",
  "data": {
    "isGMPlus": true,
    "hasSupplyPermission": false,
    "canManagePredefinedItems": true,
    "permission": null
  }
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "Invalid data or validation error",
  "data": null
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "You don't have permission to manage predefined items",
  "data": null
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Predefined item not found with ID: {uuid}",
  "data": null
}
```

### 409 Conflict
```json
{
  "success": false,
  "message": "Item 'Apples' with unit type 'lb' already exists",
  "data": null
}
```

---

## Business Rules

1. **Uniqueness**: Item name + unit type must be unique per company
2. **Soft Delete**: Items are never hard deleted, only marked inactive
3. **Multi-tenant**: All operations scoped to user's company
4. **Permissions**: Only GM+ users or Supply Management Specialists can access
5. **CSV Import**: 
   - Skips duplicates (same name + unit type)
   - Validates required fields
   - Returns detailed import report
6. **Temporary Permissions**: Non-permanent permissions auto-expire

---

## Example Workflow

### 1. Check Permission
```bash
GET /api/supply-management/check?companyId=company-uuid
```

### 2. Create Items via CSV Import
```bash
POST /api/predefined-items/import-csv?companyId=company-uuid
Content-Type: multipart/form-data
file: items.csv
```

### 3. Associate with Stores
```bash
POST /api/predefined-items/{item-id}/stores?companyId=company-uuid
{
  "locationIds": ["store-1-uuid", "store-2-uuid"]
}
```

### 4. Export Catalog
```bash
GET /api/predefined-items/export-csv?companyId=company-uuid
```

---

## Database Schema

### predefined_items
- Primary key: `id` (UUID)
- Unique constraint: `(company_id, name, unit_type)`
- Soft delete: `is_active`, `deleted_at`, `deleted_by`

### predefined_item_stores
- Junction table: `predefined_item_id` ↔ `store_id`
- Unique constraint: `(predefined_item_id, store_id)`

### predefined_item_warehouses
- Junction table: `predefined_item_id` ↔ `warehouse_id`
- Unique constraint: `(predefined_item_id, warehouse_id)`

### supply_management_permissions
- Primary key: `id` (UUID)
- Unique constraint: `(user_id, company_id, permission_type)`
- Expiration: `is_permanent`, `expires_at`
- Revocation: `revoked_at`, `revoked_by`
