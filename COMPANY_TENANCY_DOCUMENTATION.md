# Company-Based Multi-Tenancy in InventSight

## Overview

InventSight implements **schema-per-company multi-tenancy** where each company has its own PostgreSQL schema. This ensures complete data isolation between companies while maintaining a single application instance and database server.

## Architecture

### Key Concepts

1. **Company UUID as Tenant ID**: Each company is identified by a unique UUID that serves as the tenant identifier
2. **Schema Isolation**: Each company gets its own PostgreSQL schema named `company_<uuid>` (with dashes replaced by underscores)
3. **No Public Fallback**: Company schemas do NOT include the public schema in search_path, ensuring complete isolation
4. **Membership Validation**: Every request validates that the authenticated user is a member of the company specified in the X-Tenant-ID header

### Schema Naming Convention

- **Format**: `company_<uuid_with_underscores>`
- **Example**: For company UUID `550e8400-e29b-41d4-a716-446655440000`
  - Schema name: `company_550e8400_e29b_41d4_a716_446655440000`

### Database Structure

```
PostgreSQL Database
├── public schema (global tables)
│   ├── companies
│   ├── company_store_user (memberships)
│   └── users (authentication)
├── company_<uuid_1> (Company 1's schema)
│   ├── stores
│   ├── products
│   ├── sales
│   ├── warehouses
│   └── ...
├── company_<uuid_2> (Company 2's schema)
│   ├── stores
│   ├── products
│   ├── sales
│   ├── warehouses
│   └── ...
```

## Implementation Components

### 1. CompanyTenantFilter

**Location**: `src/main/java/com/pos/inventsight/tenant/CompanyTenantFilter.java`

**Purpose**: 
- Extracts and validates the `X-Tenant-ID` header (company UUID)
- Verifies the authenticated user has membership in the specified company
- Sets the tenant context to the company's schema
- Returns appropriate errors for invalid requests

**Order**: Runs at `HIGHEST_PRECEDENCE + 5` (before JWT authentication filter)

**Error Codes**:
- `400 Bad Request`: Missing or invalid X-Tenant-ID header
- `401 Unauthorized`: No authenticated user
- `403 Forbidden`: User is not a member of the company
- `404 Not Found`: Company does not exist
- `500 Internal Server Error`: Unexpected error

### 2. SchemaBasedMultiTenantConnectionProvider

**Location**: `src/main/java/com/pos/inventsight/tenant/SchemaBasedMultiTenantConnectionProvider.java`

**Purpose**:
- Manages database connections for each tenant
- Sets PostgreSQL `search_path` based on tenant schema
- For company schemas (`company_*`): Sets search_path WITHOUT public fallback
- For other schemas: Includes public for backward compatibility

**Key Method**:
```java
public Connection getConnection(String tenantId) throws SQLException
```

### 3. CompanyStoreUser Entity

**Location**: `src/main/java/com/pos/inventsight/model/sql/CompanyStoreUser.java`

**Purpose**: Represents company memberships (replaces UserStoreRole for company-centric approach)

**Key Fields**:
- `company_id`: The company the user belongs to
- `user_id`: The user who is a member
- `store_id`: Optional specific store within the company
- `role`: User's role (FOUNDER, GENERAL_MANAGER, STORE_MANAGER, EMPLOYEE)
- `is_active`: Whether the membership is currently active

## API Usage

### Request Headers

All protected API endpoints MUST include the `X-Tenant-ID` header with a valid company UUID:

```http
X-Tenant-ID: 550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <jwt_token>
```

### Public Endpoints (No X-Tenant-ID Required)

The following endpoints do NOT require the X-Tenant-ID header:
- `/auth/**` - Authentication endpoints
- `/api/register` - User registration
- `/api/auth/register` - Alternative registration endpoint
- `/api/auth/signup` - Signup endpoint
- `/health/**` - Health check endpoints
- `/actuator/**` - Actuator endpoints
- `/swagger-ui/**` - API documentation
- `/v3/api-docs/**` - OpenAPI documentation

### Example API Calls

#### JavaScript/TypeScript
```javascript
// Set up API client with company tenant header
const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'X-Tenant-ID': '550e8400-e29b-41d4-a716-446655440000',
    'Authorization': `Bearer ${jwtToken}`
  }
});

// All requests will use the company's schema
const products = await apiClient.get('/products');
const sales = await apiClient.post('/sales', saleData);
```

#### cURL
```bash
# List products for a company
curl -H "X-Tenant-ID: 550e8400-e29b-41d4-a716-446655440000" \
     -H "Authorization: Bearer <token>" \
     http://localhost:8080/api/products

# Create a new sale
curl -X POST \
     -H "X-Tenant-ID: 550e8400-e29b-41d4-a716-446655440000" \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{"storeId": "...", "items": [...]}' \
     http://localhost:8080/api/sales
```

#### Java Spring RestTemplate
```java
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.set("X-Tenant-ID", "550e8400-e29b-41d4-a716-446655440000");
headers.set("Authorization", "Bearer " + jwtToken);
HttpEntity<String> entity = new HttpEntity<>(headers);

ResponseEntity<Product[]> response = restTemplate.exchange(
    "http://localhost:8080/api/products",
    HttpMethod.GET,
    entity,
    Product[].class
);
```

## Database Migration

### Initial Setup

1. **Run the migration script**:
```bash
psql -U postgres -d inventsight -f src/main/resources/migration-schema-per-company.sql
```

This script:
- Creates functions to manage company schemas
- Sets up automatic schema creation trigger
- Creates schemas for all existing active companies

### Creating a New Company

When a new company is created in the `companies` table, a database trigger automatically:
1. Generates the schema name from the company UUID
2. Creates the new schema
3. Creates all necessary tables in that schema
4. Sets up indexes for performance

**Manual Schema Creation** (if needed):
```sql
-- For company UUID: 550e8400-e29b-41d4-a716-446655440000
SELECT create_company_schema('550e8400-e29b-41d4-a716-446655440000'::UUID);
```

### Verifying Schema Creation

```sql
-- List all company schemas
SELECT schema_name 
FROM information_schema.schemata 
WHERE schema_name LIKE 'company_%'
ORDER BY schema_name;

-- Check if a specific company schema exists
SELECT EXISTS (
    SELECT 1 FROM information_schema.schemata 
    WHERE schema_name = 'company_550e8400_e29b_41d4_a716_446655440000'
);

-- List tables in a company schema
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'company_550e8400_e29b_41d4_a716_446655440000';
```

### Cleanup/Testing

To remove a company schema (⚠️ **CAUTION**: This deletes all company data):
```sql
SELECT drop_company_schema('550e8400-e29b-41d4-a716-446655440000'::UUID);
```

## Company Membership Management

### Adding a User to a Company

```java
@Service
public class CompanyMembershipService {
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    public void addUserToCompany(User user, Company company, CompanyRole role) {
        CompanyStoreUser membership = new CompanyStoreUser(
            company, 
            user, 
            role,
            "admin" // assignedBy
        );
        companyStoreUserRepository.save(membership);
    }
}
```

### Checking User Membership

The `CompanyTenantFilter` automatically validates membership on every request. You can also manually check:

```java
// Check if user has access to company
boolean hasAccess = companyStoreUserRepository
    .existsByUserAndCompanyAndIsActiveTrue(user, company);

// Get all user's companies
List<Company> userCompanies = companyStoreUserRepository
    .getUserCompanies(user);
```

## Service Layer Updates

Services should rely on the tenant context set by `CompanyTenantFilter`:

```java
@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    public List<Product> getAllProducts() {
        // Automatically queries from current company's schema
        // No need to manually filter by company
        return productRepository.findByIsActiveTrue();
    }
    
    public Product createProduct(Product product) {
        // Automatically saves to current company's schema
        return productRepository.save(product);
    }
}
```

**Note**: The tenant context is automatically managed by the filter. Services don't need to explicitly handle company filtering.

## Security Considerations

### 1. Data Isolation
- Each company's data is in a separate schema
- No risk of cross-company data leakage
- PostgreSQL enforces schema boundaries

### 2. Access Control
- Users must be authenticated (JWT token)
- Users must have active membership in the company
- Invalid company UUIDs are rejected
- SQL injection is prevented through UUID validation

### 3. Schema Management
- Schema names are generated from UUIDs (no user input)
- Schema operations use parameterized queries
- Invalid schema names are rejected

## Troubleshooting

### Common Issues

#### 1. "X-Tenant-ID header is required"
**Cause**: Missing X-Tenant-ID header in request
**Solution**: Ensure all API requests include the company UUID in X-Tenant-ID header

#### 2. "Access denied: user is not a member of the specified company"
**Cause**: User doesn't have membership in the specified company
**Solution**: 
- Verify the user has an active record in `company_store_user` table
- Check `is_active` is true
- Use the correct company UUID

#### 3. "Company not found"
**Cause**: The company UUID in X-Tenant-ID doesn't exist
**Solution**: Verify the company exists in the `companies` table

#### 4. "Schema does not exist" errors
**Cause**: Company schema wasn't created
**Solution**: 
```sql
-- Check if schema exists
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name = 'company_<uuid_with_underscores>';

-- Create it manually if needed
SELECT create_company_schema('<uuid>'::UUID);
```

#### 5. Tables not found in schema
**Cause**: Schema exists but tables weren't created
**Solution**: Re-run the schema creation function or check the migration script

### Debug Commands

```bash
# Check current tenant context (requires authentication)
curl -H "X-Tenant-ID: <company-uuid>" \
     -H "Authorization: Bearer <token>" \
     http://localhost:8080/api/tenant-info/current

# Verify user memberships
psql -U postgres -d inventsight -c \
  "SELECT u.username, c.name, csu.role, csu.is_active 
   FROM company_store_user csu 
   JOIN users u ON csu.user_id = u.id 
   JOIN companies c ON csu.company_id = c.id 
   WHERE u.username = '<username>';"

# List all schemas
psql -U postgres -d inventsight -c \
  "SELECT schema_name FROM information_schema.schemata 
   WHERE schema_name LIKE 'company_%';"
```

## Migration from User-Based to Company-Based Tenancy

If you're migrating from user-based (user UUID) tenancy to company-based tenancy:

1. **Identify user-to-company mappings**
2. **Create company records** for each tenant
3. **Create company schemas** using the migration script
4. **Migrate data** from user schemas to company schemas
5. **Update company_store_user** table with memberships
6. **Update client applications** to use company UUIDs in X-Tenant-ID

## Performance Considerations

### Schema Creation
- Schema creation is one-time per company
- Automatic via trigger on company creation
- Takes milliseconds for typical schema

### Connection Management
- Connection pooling is maintained
- search_path is set per connection
- Minimal overhead (<1ms per request)

### Indexing
- All company schemas have identical indexes
- Indexes are created automatically
- Monitor and optimize based on query patterns

### Monitoring
```sql
-- Monitor schema sizes
SELECT 
    schema_name,
    pg_size_pretty(sum(pg_total_relation_size(quote_ident(schemaname) || '.' || quote_ident(tablename)))::bigint) as size
FROM pg_tables
WHERE schemaname LIKE 'company_%'
GROUP BY schema_name
ORDER BY sum(pg_total_relation_size(quote_ident(schemaname) || '.' || quote_ident(tablename))) DESC;
```

## Best Practices

1. **Always include X-Tenant-ID**: Make it part of your API client configuration
2. **Validate company UUID**: Ensure UUIDs are valid before making requests
3. **Handle errors gracefully**: Implement proper error handling for 403/404 responses
4. **Monitor schema growth**: Track storage usage per company
5. **Backup strategies**: Implement per-company backup policies
6. **Schema migrations**: Use a consistent migration strategy across all company schemas
7. **Testing**: Test with multiple companies to ensure proper isolation

## Future Enhancements

- Schema migration tools for updating all company schemas
- Per-company feature flags
- Company-specific configuration
- Automated schema maintenance and optimization
- Company data export/import tools
- Cross-company reporting (with proper authorization)

## Support

For issues or questions related to company-based multi-tenancy:
1. Check this documentation
2. Review application logs for detailed error messages
3. Verify database schema state
4. Check company_store_user memberships
5. Consult the development team
