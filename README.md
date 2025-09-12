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

### Important Notes

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