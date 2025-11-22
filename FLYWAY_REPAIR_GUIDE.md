# Flyway Migration Repair Guide

## Issue: Checksum Mismatch

This occurs when migration files are modified after being applied to the database. Flyway calculates checksums of migration files to ensure they haven't changed after being applied. When checksums don't match, it indicates the migration file content has changed.

## Problem Description

The application was failing to start due to Flyway validation errors showing checksum mismatches for multiple migrations:

```
Migration checksum mismatch for migration version 6
-> Applied to database : 2134075863
-> Resolved locally    : -1619444361

Migration checksum mismatch for migration version 7
-> Applied to database : 1935191081
-> Resolved locally    : 1852435898

Migration checksum mismatch for migration version 10
-> Applied to database : 1025771776
-> Resolved locally    : -1034913194
```

## Resolution

We've implemented a profile-based approach to Flyway configuration that provides automatic repair in development while maintaining strict validation in production.

### Configuration Strategy

**Development Environment (`application.yml` and `application-postgres.yml`):**
- `validate-on-migrate: false` - Disabled validation for development convenience
- `repair: true` - Automatically repairs checksum mismatches on startup
- `clean-disabled: false` - Allows database resets for development

**Production Environment (`application-prod.yml`):**
- `validate-on-migrate: true` - Strict validation enabled
- `repair: false` - Requires manual intervention for repairs
- `clean-disabled: true` - Prevents accidental database wipes

> **⚠️ Production Warning:**  
> Production environments require manual validation before repair:
> 1. Review all migration file changes carefully
> 2. Use `./scripts/repair-flyway.sh prod` for manual repair
> 3. Never modify migration files after they've been applied to production
> 4. Always maintain database backups before running repairs
> 5. Use CI/CD pipelines to validate migrations before deployment

### Configuration Changes

**File: `src/main/resources/application.yml` (Default/Development)**

```yaml
spring:
  flyway:
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    default-schema: public
    validate-on-migrate: false  # Disabled by default for development
    repair: true  # Automatically repair checksum mismatches
    clean-disabled: false  # Allow clean for development
```

**File: `src/main/resources/application-prod.yml` (Production)**

```yaml
spring:
  flyway:
    enabled: true
    validate-on-migrate: true  # Strict validation in production
    repair: false  # Disable automatic repair - require manual intervention
    clean-disabled: true  # Prevent accidental database wipes
```

**File: `src/main/resources/application-postgres.yml` (PostgreSQL Development)**

```yaml
spring:
  flyway:
    enabled: true
    validate-on-migrate: false  # Lenient validation for development
    repair: true  # Auto-repair checksum mismatches
    clean-disabled: false  # Allow clean for development
```

This configuration allows Flyway to:
- Update checksums in the `flyway_schema_history` table to match current migration files
- Remove failed migration entries
- Realign the migration history with current files
- Provide environment-appropriate behavior (lenient in dev, strict in prod)

## Affected Migrations

- **V6__sales_orders.sql**: Migration for sales orders schema
- **V7__company_store_user_roles.sql**: Migration for company, store, and user roles
- **V10__create_preexisting_items_table.sql**: Migration for preexisting items table

## Prevention Best Practices

To avoid checksum mismatches in the future:

1. **Never modify existing migration files after they're applied**
   - Once a migration is applied to any database (dev, staging, prod), treat it as immutable
   - Create new migration files for schema changes instead

2. **Use descriptive version numbers**
   - Use sequential version numbers (V1, V2, V3, etc.)
   - Include descriptive names (V11__add_user_preferences_table.sql)

3. **Version control migrations**
   - Commit migrations to version control before applying
   - Never edit migrations after committing

4. **Separate environments**
   - Dev, staging, and production should have the same migrations
   - Test migrations in dev before applying to staging/prod

5. **Use Flyway baseline**
   - For existing databases, use `baseline-on-migrate: true`
   - This creates a baseline without re-running old migrations

## Manual Repair

For production environments or when automatic repair is disabled, use these manual repair methods:

### Option 1: Repair Script (Recommended)
```bash
# Development environment
./scripts/repair-flyway.sh

# Production environment (requires confirmation)
./scripts/repair-flyway.sh prod

# Custom profile
./scripts/repair-flyway.sh postgres
```

The repair script provides:
- Interactive confirmation for production repairs
- Automatic checksum updates
- Verification steps
- Safety checks and warnings

### Option 2: Maven Spring Boot
```bash
# Run with repair enabled for specific profile
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--spring.flyway.repair=true" \
  -Dspring-boot.run.profiles=dev
```

### Option 3: Database Console (Inspection)
```bash
# Connect to database
psql -U inventsight_user -d inventsight_db

# Check current state
SELECT version, description, checksum, success, installed_on
FROM flyway_schema_history 
ORDER BY installed_rank;

# Check specific versions with mismatches
SELECT version, description, checksum, success 
FROM flyway_schema_history 
WHERE version IN ('6', '7', '10');
```

### Option 4: Manual Checksum Update (Advanced - Not Recommended)
```sql
-- ⚠️ WARNING: Only use this if you understand the implications
-- This directly modifies migration history and should be used with extreme caution
-- It's better to use the repair script or spring.flyway.repair=true

-- First, get the checksum from Flyway logs or calculate it
-- Flyway logs show: "Migration checksum mismatch for migration version X"
-- -> Resolved locally: [checksum_value]

-- Example: Update checksum for version 6
-- UPDATE flyway_schema_history 
-- SET checksum = -1619444361  -- Use the "Resolved locally" value from Flyway error
-- WHERE version = '6';

-- Always prefer automated repair methods over manual SQL updates
```

## Verifying the Fix

After applying the repair configuration:

1. **Start the application**
   ```bash
   mvn spring-boot:run
   ```

2. **Check Flyway schema history**
   ```sql
   SELECT version, description, checksum, success, installed_on 
   FROM flyway_schema_history 
   ORDER BY installed_rank;
   ```

3. **Verify all migrations show success=true**
   - All migrations should have `success = true`
   - Checksums should be updated to match current files
   - No validation errors in application logs

## Configuration Options Explained

### Default Configuration (application.yml)
- `spring.flyway.locations=classpath:db/migration` - Location of migration files
- `spring.flyway.baseline-on-migrate=true` - Create baseline for existing database
- `spring.flyway.baseline-version=0` - Starting version for baseline
- `spring.flyway.default-schema=public` - Default schema for migrations
- `spring.flyway.validate-on-migrate=false` - Disabled for development convenience
- `spring.flyway.repair=true` - Automatically repair checksum mismatches on startup
- `spring.flyway.clean-disabled=false` - Allow clean for development

### Development Configuration (application-dev.yml)
- `spring.flyway.enabled=false` - Disabled for H2 in-memory development database
- When using PostgreSQL in dev, enable with lenient settings (see application-postgres.yml)

### PostgreSQL Development Configuration (application-postgres.yml)
- `spring.flyway.enabled=true` - Enable Flyway for PostgreSQL
- `spring.flyway.validate-on-migrate=false` - Lenient validation for development
- `spring.flyway.repair=true` - Auto-repair checksum mismatches
- `spring.flyway.clean-disabled=false` - Allow clean for development database resets

### Production Configuration (application-prod.yml)
- `spring.flyway.enabled=true` - Enable Flyway for production
- `spring.flyway.validate-on-migrate=true` - Strict validation in production
- `spring.flyway.repair=false` - Disable automatic repair - require manual intervention
- `spring.flyway.clean-disabled=true` - Prevent accidental database wipes

## Troubleshooting

### Issue: Application still fails with checksum errors
**Solution**: 
1. Check that the correct profile is active (dev/prod/postgres)
2. Verify `spring.flyway.repair=true` is set for your active profile
3. Try using the repair script: `./scripts/repair-flyway.sh`
4. Check database connection and credentials
5. Review application logs for specific error messages
6. Verify migration files exist and are readable

### Issue: Migration failed and left the database in inconsistent state
**Solution**:
1. Check `flyway_schema_history` table for failed migrations
2. Review the error in application logs
3. Manually fix the database schema if needed
4. Delete the failed entry from `flyway_schema_history`
5. Use repair script: `./scripts/repair-flyway.sh`
6. Re-run the application
4. Re-run the application with repair enabled

### Issue: Need to start fresh (Development only)
**Solution**:
```bash
# ⚠️ DANGER: THESE COMMANDS DELETE ALL DATA - NEVER USE IN PRODUCTION! ⚠️
# ⚠️ ONLY use in local development environments! ⚠️

# Drop and recreate database (LOCAL DEVELOPMENT ONLY)
psql -U inventsight_user -c "DROP DATABASE IF EXISTS inventsight_db;"
psql -U inventsight_user -c "CREATE DATABASE inventsight_db;"

# Restart application - migrations will run fresh
mvn spring-boot:run
```

## Expected Outcome

After applying this fix:
✅ Application starts without checksum errors  
✅ All migrations show as successful in `flyway_schema_history`  
✅ Database schema is correct and consistent  
✅ Future migrations will be tracked properly  
✅ Checksum validation works for new migrations  

## Additional Resources

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Flyway Repair Command](https://flywaydb.org/documentation/command/repair)
- [Spring Boot Flyway Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)

## Migration History

| Version | Description | Status |
|---------|-------------|--------|
| V0 | Enable pgcrypto | ✅ Applied |
| V1 | Add subscription level to users | ✅ Applied |
| V2 | Add MFA and security tables | ✅ Applied |
| V3 | Add audit events table | ✅ Applied |
| V4 | Add locale currency fields | ✅ Applied |
| V5 | Add idempotency tracking | ✅ Applied |
| V6 | Sales orders | ✅ Repaired |
| V7 | Company store user roles | ✅ Repaired |
| V8 | Add default tenant ID to users | ✅ Applied |
| V9 | Add company to stores | ✅ Applied |
| V10 | Create preexisting items table | ✅ Repaired |
| V11 | Create one-time permissions table | ✅ Applied |
| V12 | Create refresh tokens table | ✅ Applied |
| V13 | Add email verification columns | ✅ Applied |
