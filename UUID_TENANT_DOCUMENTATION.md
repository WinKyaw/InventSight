# UUID and Tenant ID Implementation in InventSight

## Overview

InventSight now supports UUID-based identification for users and products, with improved tenant isolation using user UUIDs as tenant identifiers.

## Key Changes

### 1. UUID Fields Added

#### User Entity
- **`uuid`**: Unique UUID identifier for each user
- **`tenant_id`**: Set to the user's UUID, used for schema-based tenant isolation
- Both fields are automatically generated for new users
- Existing users get UUIDs assigned via migration

#### Product Entity
- **`uuid`**: Unique UUID identifier for each product
- Automatically generated for new products
- Existing products get UUIDs assigned via migration

### 2. Tenant Isolation Implementation

#### How It Works
1. **User Registration**: Each new user gets a unique UUID
2. **Tenant ID**: The user's UUID becomes their `tenant_id`
3. **Schema Mapping**: Schema names are based on user UUIDs for privacy and uniqueness
4. **Request Handling**: The `X-Tenant-ID` header should contain the user's UUID
5. **Database Operations**: All queries are automatically filtered by the current tenant's schema

#### Schema Naming Convention
- Default tenant: `public` schema (for legacy/global data)
- User tenants: Schema name derived from user UUID (e.g., `tenant_a1b2c3d4_e5f6_7890_abcd_1234567890ab`)

### 3. API Changes

#### Product API Response
Products now include the `uuid` field in all API responses:
```json
{
  "id": 123,
  "uuid": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
  "name": "Product Name",
  "sku": "PROD-001",
  ...
}
```

#### Headers Required
For tenant-specific operations, include the user's UUID in the header:
```http
X-Tenant-ID: a1b2c3d4-e5f6-7890-abcd-1234567890ab
```

### 4. Tenant Isolation Fixes

#### Previous Issue
Products were visible across tenants because the ProductService was using global queries like:
- `findByIsActiveTrue()` - returned ALL products from ALL schemas

#### Fixed Implementation
ProductService now uses tenant-aware queries:
- `findByStoreAndIsActiveTrue(currentStore)` - returns only products within current tenant's schema
- Automatic tenant detection based on current user's UUID
- Fallback to global queries only for default/public tenant

## Migration Guide

### For Existing Installations

1. **Run Database Migration**:
   ```sql
   -- Execute the migration script
   \i src/main/resources/migration-uuid-support.sql
   ```

2. **Verify Migration via API**:
   ```bash
   # Validate UUIDs were assigned
   curl -H "Authorization: Bearer <token>" \
        GET /api/admin/migration/uuid/validate
   
   # Manually trigger migration if needed
   curl -H "Authorization: Bearer <token>" \
        POST /api/admin/migration/uuid
   ```

3. **Update Client Applications**:
   - Include user's UUID in `X-Tenant-ID` header for all requests
   - Update any code that references product IDs to also handle UUIDs
   - Test tenant isolation by switching between different user UUIDs

### For New Installations

- New users automatically get UUIDs assigned during registration
- New products automatically get UUIDs assigned during creation
- No migration needed

## Usage Examples

### 1. User Registration
```bash
curl -X POST /api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "user@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

Response includes UUID:
```json
{
  "user": {
    "id": 1,
    "uuid": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
    "tenantId": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
    "username": "newuser",
    ...
  }
}
```

### 2. Tenant-Specific Product Query
```bash
curl -H "X-Tenant-ID: a1b2c3d4-e5f6-7890-abcd-1234567890ab" \
     -H "Authorization: Bearer <token>" \
     GET /api/products
```

Returns only products visible to this tenant/user.

### 3. Product Creation
```bash
curl -X POST /api/products \
  -H "X-Tenant-ID: a1b2c3d4-e5f6-7890-abcd-1234567890ab" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Product",
    "sku": "PROD-002",
    "price": 29.99,
    "quantity": 100
  }'
```

Product is automatically associated with the current tenant's store.

## Security Considerations

### UUID Benefits
1. **Privacy**: UUIDs don't reveal information about user count or creation order
2. **Uniqueness**: Globally unique across all tenants and time
3. **Unpredictability**: Cannot be guessed or enumerated

### Tenant Isolation
1. **Schema-Based**: Complete data isolation at database level
2. **Automatic Filtering**: No risk of accidentally querying wrong tenant's data
3. **Header Validation**: Tenant ID is validated against authenticated user's UUID

## Testing Tenant Isolation

### 1. Create Test Users
```bash
# User A
curl -X POST /api/auth/register -d '{"username":"userA", ...}'
# Note the UUID from response: uuid_A

# User B  
curl -X POST /api/auth/register -d '{"username":"userB", ...}'
# Note the UUID from response: uuid_B
```

### 2. Create Products for Each Tenant
```bash
# Create product for User A
curl -H "X-Tenant-ID: uuid_A" -X POST /api/products -d '{"name":"Product A", ...}'

# Create product for User B
curl -H "X-Tenant-ID: uuid_B" -X POST /api/products -d '{"name":"Product B", ...}'
```

### 3. Verify Isolation
```bash
# Query as User A - should only see "Product A"
curl -H "X-Tenant-ID: uuid_A" GET /api/products

# Query as User B - should only see "Product B"  
curl -H "X-Tenant-ID: uuid_B" GET /api/products
```

## Troubleshooting

### Common Issues

1. **Products Visible Across Tenants**
   - Ensure `X-Tenant-ID` header is set correctly
   - Verify user UUID matches the header value
   - Check that user has an associated store in UserStoreRole table

2. **Missing UUIDs**
   - Run migration script: `/api/admin/migration/uuid`
   - Validate results: `/api/admin/migration/uuid/validate`

3. **Schema Not Found Errors**
   - Ensure schema exists for the tenant UUID
   - Check database logs for schema creation errors
   - Verify PostgreSQL permissions

### Debug Commands

```bash
# Check current tenant context
curl -H "X-Tenant-ID: <uuid>" GET /api/tenant-info/current

# Validate all UUIDs
curl POST /api/admin/migration/uuid/validate

# View migration logs in application logs
tail -f logs/inventsight.log | grep UUID
```

## Performance Considerations

- UUID indexes are created for optimal query performance
- Schema switching is optimized by connection pooling
- Tenant-aware queries prevent unnecessary cross-schema joins

## Future Enhancements

1. **UUID as Primary Keys**: Consider using UUIDs as primary keys instead of Long IDs
2. **Tenant Management UI**: Admin interface for managing tenant schemas
3. **Cross-Tenant Reporting**: Aggregate reporting across multiple tenants (admin only)
4. **Tenant Migration Tools**: Tools for moving data between tenants