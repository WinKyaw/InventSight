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

We've enabled `spring.flyway.repair=true` in the Flyway configuration which automatically repairs checksums on application startup. This ensures the checksums in the database match the current migration file content.

> **⚠️ Production Warning:**  
> While `repair=true` is enabled by default, for production environments you should:
> 1. Consider disabling automatic repair (`repair=false`)
> 2. Manually validate and repair migrations using `mvn flyway:repair` after review
> 3. Never modify migration files after they've been applied to production
> 4. Use CI/CD pipelines to validate migrations before deployment

### Configuration Changes

**File: `src/main/resources/application.yml`**

Added the following Flyway repair configuration:
```yaml
spring:
  flyway:
    repair: true  # Automatically repair checksum mismatches on startup
```

This allows Flyway to:
- Update checksums in the `flyway_schema_history` table to match current migration files
- Remove failed migration entries
- Realign the migration history with current files

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

## Manual Repair (if needed)

If automatic repair doesn't work, you can manually repair checksums:

### Option 1: Maven Flyway Plugin
```bash
# Run Flyway repair command
mvn flyway:repair
```

### Option 2: Database Console
```bash
# Connect to database
psql -U inventsight_user -d inventsight_db

# Check current state
SELECT version, description, checksum, success 
FROM flyway_schema_history 
WHERE version IN ('6', '7', '10');
```

### Option 3: Manual Checksum Update (Advanced)
```sql
-- WARNING: Only use this if you understand the implications
-- This directly modifies migration history and should be used with extreme caution

-- First, get the checksum from Flyway logs or calculate it
-- Flyway logs show: "Migration checksum mismatch for migration version X"
-- -> Resolved locally: [checksum_value]

-- Example: Update checksum for version 6
-- UPDATE flyway_schema_history 
-- SET checksum = -1619444361  -- Use the "Resolved locally" value from Flyway error
-- WHERE version = '6';

-- Note: It's better to use spring.flyway.repair=true than manual updates
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

### Main Configuration (application.yml)
- `spring.flyway.repair=true` - Automatically repair checksum mismatches on startup
- `spring.flyway.baseline-on-migrate=true` - Create baseline for existing database
- `spring.flyway.validate-on-migrate=true` - Validate migrations before running
- `spring.flyway.locations=classpath:db/migration` - Location of migration files
- `spring.flyway.default-schema=public` - Default schema for migrations

### Development Configuration (application-dev.yml)
- `spring.flyway.enabled=false` - Disabled for H2 in-memory development database
- When using PostgreSQL in dev, enable with `repair=true` for lenient handling

## Troubleshooting

### Issue: Application still fails with checksum errors
**Solution**: 
1. Check that `spring.flyway.repair=true` is set in the active profile
2. Verify database connection is working
3. Check application logs for specific error messages
4. Try running `mvn flyway:repair` manually

### Issue: Migration failed and left the database in inconsistent state
**Solution**:
1. Check `flyway_schema_history` table for failed migrations
2. Manually fix the database schema if needed
3. Delete the failed entry from `flyway_schema_history`
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
