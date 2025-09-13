# InventSight - Intelligent Inventory & POS System

InventSight is a modern, intelligent inventory management and point-of-sale (POS) system built with Spring Boot, PostgreSQL, and multi-tenant architecture.

## Multi-Tenant Architecture

InventSight supports schema-based multi-tenancy, allowing multiple tenants (organizations) to use the same application instance while keeping their data completely isolated in separate PostgreSQL schemas.

### How Multi-Tenancy Works

- **Schema Isolation**: Each tenant gets their own PostgreSQL schema
- **Header-Based Routing**: Tenant identification via `X-Tenant-ID` HTTP header
- **Automatic Schema Switching**: Hibernate automatically switches to the correct schema per request
- **Backwards Compatibility**: Requests without tenant headers use the default `public` schema

### Setting Up New Tenants

When onboarding a new tenant, follow these steps:

#### 1. Create a New Schema

Connect to your PostgreSQL database and create a new schema for the tenant:

```sql
-- Replace 'tenant_name' with your actual tenant identifier
CREATE SCHEMA tenant_name;

-- Grant necessary permissions
GRANT USAGE ON SCHEMA tenant_name TO inventsight_user;
GRANT CREATE ON SCHEMA tenant_name TO inventsight_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA tenant_name TO inventsight_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA tenant_name TO inventsight_user;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA tenant_name GRANT ALL ON TABLES TO inventsight_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA tenant_name GRANT ALL ON SEQUENCES TO inventsight_user;
```

#### 2. Initialize Tables in the New Schema

You have several options to initialize the database tables for the new tenant:

**Option A: Using Hibernate Auto-DDL (Development)**
```yaml
# In application.yml, ensure ddl-auto is set to 'update' or 'create'
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

Then make a request with the new tenant header:
```bash
curl -H "X-Tenant-ID: tenant_name" http://localhost:8080/api/health
```

**Option B: Manual Schema Copy (Production)**
```sql
-- Copy the structure from public schema to new tenant schema
-- This approach gives you full control over the migration

-- 1. Create tables by copying structure from public schema
CREATE TABLE tenant_name.users AS TABLE public.users WITH NO DATA;
CREATE TABLE tenant_name.stores AS TABLE public.stores WITH NO DATA;
CREATE TABLE tenant_name.products AS TABLE public.products WITH NO DATA;
CREATE TABLE tenant_name.sales AS TABLE public.sales WITH NO DATA;
CREATE TABLE tenant_name.sale_items AS TABLE public.sale_items WITH NO DATA;
CREATE TABLE tenant_name.employees AS TABLE public.employees WITH NO DATA;
CREATE TABLE tenant_name.categories AS TABLE public.categories WITH NO DATA;
CREATE TABLE tenant_name.user_store_roles AS TABLE public.user_store_roles WITH NO DATA;
CREATE TABLE tenant_name.user_profiles AS TABLE public.user_profiles WITH NO DATA;
CREATE TABLE tenant_name.user_settings AS TABLE public.user_settings WITH NO DATA;
CREATE TABLE tenant_name.discount_audit_log AS TABLE public.discount_audit_log WITH NO DATA;
CREATE TABLE tenant_name.email_verification_tokens AS TABLE public.email_verification_tokens WITH NO DATA;
CREATE TABLE tenant_name.events AS TABLE public.events WITH NO DATA;

-- 2. Copy constraints and indexes
-- (You may need to adjust these based on your specific schema)
-- This is a simplified example - you should copy all constraints, indexes, and sequences

-- 3. Create sequences
CREATE SEQUENCE tenant_name.users_id_seq OWNED BY tenant_name.users.id;
CREATE SEQUENCE tenant_name.stores_id_seq OWNED BY tenant_name.stores.id;
CREATE SEQUENCE tenant_name.products_id_seq OWNED BY tenant_name.products.id;
-- ... create other sequences as needed

-- 4. Set sequence ownership and defaults
ALTER TABLE tenant_name.users ALTER COLUMN id SET DEFAULT nextval('tenant_name.users_id_seq');
ALTER TABLE tenant_name.stores ALTER COLUMN id SET DEFAULT nextval('tenant_name.stores_id_seq');
ALTER TABLE tenant_name.products ALTER COLUMN id SET DEFAULT nextval('tenant_name.products_id_seq');
-- ... set other defaults as needed
```

**Option C: Using Migration Scripts**
```sql
-- Create a comprehensive migration script
-- Save this as a file like 'tenant_schema_setup.sql'

DO $$
DECLARE
    tenant_schema TEXT := 'tenant_name'; -- Change this for each tenant
BEGIN
    -- Set search_path to include the tenant schema
    EXECUTE 'SET search_path TO ' || tenant_schema || ', public';
    
    -- Run your complete schema creation script here
    -- This could be your existing schema.sql file modified for the tenant
    
    -- Example: Copy structure from public schema
    FOR table_name IN 
        SELECT tablename FROM pg_tables WHERE schemaname = 'public'
    LOOP
        EXECUTE 'CREATE TABLE ' || tenant_schema || '.' || table_name || 
                ' AS TABLE public.' || table_name || ' WITH NO DATA';
    END LOOP;
    
    -- Add constraints, indexes, etc.
    -- ... (implement based on your specific needs)
    
END $$;
```

#### 3. Verify Tenant Setup

Test the new tenant setup by making requests with the tenant header:

```bash
# Test basic connectivity
curl -H "X-Tenant-ID: tenant_name" \
     -H "Content-Type: application/json" \
     http://localhost:8080/api/health

# Test with authentication (if applicable)
curl -H "X-Tenant-ID: tenant_name" \
     -H "Authorization: Bearer your-jwt-token" \
     -H "Content-Type: application/json" \
     http://localhost:8080/api/user/profile
```

#### 4. Configure Application Access

Make sure your application clients include the tenant header in all requests:

```javascript
// JavaScript/React example
const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'X-Tenant-ID': 'tenant_name'
  }
});

// Or set it dynamically
apiClient.defaults.headers['X-Tenant-ID'] = getCurrentTenant();
```

```java
// Java client example
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.set("X-Tenant-ID", "tenant_name");
HttpEntity<String> entity = new HttpEntity<>(headers);

ResponseEntity<String> response = restTemplate.exchange(
    "http://localhost:8080/api/endpoint",
    HttpMethod.GET,
    entity,
    String.class
);
```

### Example Usage

Here's how to use the multi-tenancy feature in your application:

#### Client Applications

**JavaScript/TypeScript (React, Angular, etc.)**
```javascript
// Set up API client with tenant header
const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'X-Tenant-ID': 'company_abc' // Replace with actual tenant ID
  }
});

// All requests will now use the 'company_abc' schema
const response = await apiClient.get('/users');
```

**Java Client**
```java
// Spring RestTemplate example
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.set("X-Tenant-ID", "company_xyz");
HttpEntity<String> entity = new HttpEntity<>(headers);

ResponseEntity<String> response = restTemplate.exchange(
    "http://localhost:8080/api/users",
    HttpMethod.GET,
    entity,
    String.class
);
```

**cURL Examples**
```bash
# Request with tenant header
curl -H "X-Tenant-ID: company_abc" \
     -H "Authorization: Bearer your-jwt-token" \
     http://localhost:8080/api/users

# Request without tenant header (uses public schema)
curl -H "Authorization: Bearer your-jwt-token" \
     http://localhost:8080/api/users
```

#### Server-Side Usage

**In Controllers or Services**
```java
@RestController
@RequestMapping("/api/tenant-info")
public class TenantInfoController {
    
    @GetMapping("/current")
    public ResponseEntity<Map<String, String>> getCurrentTenant() {
        String currentTenant = TenantContext.getCurrentTenant();
        boolean isSet = TenantContext.isSet();
        
        Map<String, String> info = Map.of(
            "tenant", currentTenant,
            "isSet", String.valueOf(isSet)
        );
        
        return ResponseEntity.ok(info);
    }
}
```

**Programmatic Tenant Setting (for background jobs, etc.)**
```java
@Service
public class BackgroundJobService {
    
    public void processDataForTenant(String tenantId) {
        try {
            // Set tenant context for this operation
            TenantContext.setCurrentTenant(tenantId);
            
            // All database operations will now use the tenant's schema
            userService.updateAllUsers();
            productService.generateReports();
            
        } finally {
            // Always clean up the tenant context
            TenantContext.clear();
        }
    }
}

1. **Schema Naming**: Use lowercase, alphanumeric characters, underscores, and dashes only
2. **Security**: The application validates tenant names to prevent SQL injection
3. **Default Behavior**: Requests without `X-Tenant-ID` header use the `public` schema
4. **Performance**: Each tenant's data is completely isolated, providing better security and performance
5. **Monitoring**: Check application logs for tenant switching activities

### Troubleshooting

**Issue: Tables not found**
- Ensure the tenant schema exists
- Verify tables are created in the tenant schema
- Check that the `X-Tenant-ID` header is being sent

**Issue: Permission denied**
- Verify database user has access to the tenant schema
- Check schema permissions are correctly set

**Issue: Schema not switching**
- Verify the tenant filter is working
- Check application logs for tenant context messages
- Ensure multi-tenant configuration is properly loaded

For more details on the technical implementation, see the classes in the `com.pos.inventsight.tenant` package.

## Database Migration: UUID Primary Keys

InventSight has migrated from auto-increment `BIGINT` primary keys to `UUID` primary keys for enhanced security, scalability, and distributed system compatibility.

### Running the UUID Migration

If you're upgrading from a version that used `BIGINT` primary keys, you need to run the UUID migration script:

#### Prerequisites
1. **Backup your database** before running any migration
2. Ensure you have administrative access to your PostgreSQL database
3. Stop the application during migration to prevent data inconsistencies

#### Migration Steps

**1. Create a full database backup:**
```bash
pg_dump -h localhost -U your_username -d your_database_name > backup_before_uuid_migration.sql
```

**2. Run the UUID migration script:**
```bash
# For the main/public schema
psql -h localhost -U your_username -d your_database_name -f src/main/resources/migration-uuid-products-final.sql

# For tenant-specific schemas (run for each tenant)
psql -h localhost -U your_username -d your_database_name -c "SET search_path TO tenant_schema_name" -f src/main/resources/migration-uuid-products-final.sql
```

**3. Verify the migration:**
```sql
-- Check that products.id is now UUID
SELECT data_type 
FROM information_schema.columns 
WHERE table_name = 'products' AND column_name = 'id';

-- Should return: uuid

-- Check foreign key references are also UUID
SELECT table_name, column_name, data_type 
FROM information_schema.columns 
WHERE column_name = 'product_id' 
AND table_name IN ('sale_items', 'discount_audit_log');

-- Should return UUID for all product_id columns
```

**4. Start the application and test functionality:**
```bash
# Test basic product operations
curl -X GET "http://localhost:8080/api/products" \
     -H "X-Tenant-ID: your_tenant" \
     -H "Authorization: Bearer your_token"
```

**5. Clean up backup tables (after confirming everything works):**
```sql
DROP TABLE IF EXISTS products_backup_uuid_final, 
                     sale_items_backup_uuid_final, 
                     discount_audit_log_backup_uuid_final;
```

#### Migration Features

The migration script includes:
- **Automatic constraint discovery**: Finds and drops foreign key constraints dynamically
- **Rollback capability**: Creates backup tables for emergency recovery
- **Multi-tenant support**: Works with any PostgreSQL schema
- **Comprehensive validation**: Verifies data integrity after migration
- **Idempotent execution**: Safe to run multiple times

#### Troubleshooting Migration Issues

**Issue: Foreign key constraint errors**
```
ERROR: constraint "some_constraint_name" of relation "table_name" does not exist
```
**Solution**: The migration script dynamically discovers constraints, but if you encounter this:
```sql
-- Manually check and drop specific constraints
SELECT constraint_name, table_name 
FROM information_schema.table_constraints 
WHERE constraint_type = 'FOREIGN KEY' 
AND table_name IN ('sale_items', 'discount_audit_log');

-- Drop manually if needed
ALTER TABLE table_name DROP CONSTRAINT constraint_name;
```

**Issue: Column already exists errors**
```
ERROR: column "new_id" of relation "products" already exists
```
**Solution**: This indicates a previous partial migration. The script handles this automatically.

**Issue: Permission denied**
```
ERROR: permission denied for table products
```
**Solution**: Ensure your database user has necessary privileges:
```sql
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA schema_name TO your_username;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA schema_name TO your_username;
```

#### What the Migration Changes

**Before Migration:**
- `products.id`: `BIGINT` (auto-increment)
- `sale_items.product_id`: `BIGINT` (foreign key)
- `discount_audit_log.product_id`: `BIGINT` (foreign key)

**After Migration:**
- `products.id`: `UUID` (primary key)
- `sale_items.product_id`: `UUID` (foreign key)
- `discount_audit_log.product_id`: `UUID` (foreign key)

**Application Impact:**
- Java entities already use `UUID` types
- API endpoints accept/return UUIDs as strings
- No breaking changes to API contracts
- Enhanced security (non-predictable IDs)
- Better performance in distributed systems