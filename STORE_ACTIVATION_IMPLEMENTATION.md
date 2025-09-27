# Store Activation API Implementation

This implementation addresses the issue where auto-created stores during user registration are not being recognized during product creation due to tenant context issues.

## Problem Solved

The main issue was that when users register, a default store is automatically created, but the tenant context wasn't properly initialized, causing problems when trying to create products or access store-specific data.

## Changes Made

### 1. New DTOs Created

#### StoreRequest (`src/main/java/com/pos/inventsight/dto/StoreRequest.java`)
- Used for creating and updating stores via API
- Contains validation annotations for all store fields
- Includes optional fields like address, phone, email, etc.

#### StoreResponse (`src/main/java/com/pos/inventsight/dto/StoreResponse.java`)
- Used for API responses containing store information
- Constructor that converts from Store entity
- Includes all store fields with proper formatting

### 2. StoreService (`src/main/java/com/pos/inventsight/service/StoreService.java`)

Key methods:
- `createStore()` - Creates new stores for authenticated users
- `getUserStores()` - Lists all stores for a user
- `activateStore()` - **Key method** that sets tenant context properly
- `updateStore()` - Updates store information
- `getCurrentStore()` - Gets the current active store
- `ensureUserHasActiveStore()` - Helper to auto-create stores if needed

### 3. StoreController (`src/main/java/com/pos/inventsight/controller/StoreController.java`)

New REST endpoints:
- `GET /stores` - List user's stores
- `POST /stores` - Create new store
- `GET /stores/{id}` - Get specific store
- `POST /stores/{id}/activate` - **Critical endpoint** for activating store and setting tenant context
- `PUT /stores/{id}` - Update store
- `GET /stores/current` - Get current active store

### 4. Enhanced UserService (`src/main/java/com/pos/inventsight/service/UserService.java`)

New methods:
- `setTenantContextForUser()` - Sets tenant context to user's UUID
- `ensureUserTenantContext()` - Ensures user has proper tenant context and active store

Enhanced `createUser()` method:
- Now properly sets tenant context after store creation
- Ensures the auto-created store is properly linked to the user's UUID as tenant

## How It Works

### The Tenant Context Solution

1. **User Registration**: When a user registers, a default store is created and the tenant context is set to the user's UUID
2. **Store Activation**: The `/stores/{id}/activate` endpoint properly sets the tenant context using `TenantContext.setCurrentTenant(user.getUuid().toString())`
3. **Product Creation**: With proper tenant context, product creation now works correctly as the ProductService can find the user's store

### Key Integration Points

1. **UUID as Tenant ID**: The user's UUID serves as the tenant identifier, linking the user to their stores
2. **Tenant Context Management**: The `TenantContext` class manages the current tenant per thread
3. **Store-User Relationship**: `UserStoreRole` entities link users to stores with proper roles (OWNER, CO_OWNER, etc.)

## Usage Examples

### 1. Activate a Store for Tenant Context
```bash
POST /stores/{store-id}/activate
Authorization: Bearer {jwt-token}
```

This sets the tenant context so that subsequent product operations work correctly.

### 2. Create a New Store
```bash
POST /stores
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
  "storeName": "My New Store",
  "description": "A new store for my business",
  "address": "123 Main St",
  "city": "Anytown",
  "state": "State",
  "country": "Country"
}
```

### 3. List User's Stores
```bash
GET /stores
Authorization: Bearer {jwt-token}
```

## Testing

### Unit Tests
- `StoreServiceTest` - Tests all StoreService methods
- `UserStoreAutoCreationUnitTest` - Tests existing store auto-creation logic

### Integration Testing
- `test-store-activation-api.sh` - Shell script to test the complete API flow
- Manual testing can be done using the provided endpoints

## Key Benefits

1. **Fixes Tenant Context Issues**: Products can now be created successfully after store activation
2. **User-Friendly**: Users can manage multiple stores and switch between them
3. **Backward Compatible**: Existing auto-creation during registration still works
4. **Proper Error Handling**: Comprehensive validation and error responses
5. **Secure**: All endpoints require authentication and proper user-store relationships

## Important Notes

- The tenant ID is the user's UUID, not the store ID
- Users can have multiple stores but need to activate one for tenant context
- Auto-created stores during registration are automatically linked to the user's UUID
- The system maintains backward compatibility with existing functionality

This implementation solves the core issue while providing a comprehensive store management system for authenticated users.