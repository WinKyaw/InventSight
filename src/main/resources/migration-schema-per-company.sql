-- Schema-Per-Company Multi-Tenant Architecture Migration
-- Description: Sets up schema-per-company multi-tenancy where each company has its own PostgreSQL schema
-- Each schema is named: company_<uuid> (with dashes replaced by underscores)
-- This migration creates a function to automatically create company schemas

-- Enable UUID extension if not already enabled (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Start transaction
BEGIN;

-- ============================================================================
-- 1. Function to create a new company schema with all required tables
-- ============================================================================

CREATE OR REPLACE FUNCTION create_company_schema(company_uuid UUID)
RETURNS void AS $$
DECLARE
    schema_name TEXT;
BEGIN
    -- Build schema name: company_<uuid_with_underscores>
    schema_name := 'company_' || REPLACE(company_uuid::text, '-', '_');
    
    -- Create the schema
    EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', schema_name);
    
    -- Grant usage to the application user (adjust role name as needed)
    -- EXECUTE format('GRANT USAGE ON SCHEMA %I TO inventsight_app', schema_name);
    
    -- Create tables in the new schema
    -- Note: This creates the essential tables for multi-tenant isolation
    -- Adjust the table definitions based on your application needs
    
    -- Users table (company-specific users or references)
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.users (
            id BIGSERIAL PRIMARY KEY,
            uuid UUID UNIQUE DEFAULT uuid_generate_v4(),
            username VARCHAR(50) UNIQUE NOT NULL,
            email VARCHAR(100) UNIQUE NOT NULL,
            password VARCHAR(120) NOT NULL,
            first_name VARCHAR(50),
            last_name VARCHAR(50),
            phone VARCHAR(20),
            is_active BOOLEAN DEFAULT true,
            is_email_verified BOOLEAN DEFAULT false,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ', schema_name);
    
    -- Stores table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.stores (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            name VARCHAR(100) NOT NULL,
            description VARCHAR(500),
            address VARCHAR(200),
            city VARCHAR(100),
            state VARCHAR(100),
            postal_code VARCHAR(20),
            country VARCHAR(100),
            phone VARCHAR(20),
            email VARCHAR(100),
            is_active BOOLEAN DEFAULT true,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ', schema_name);
    
    -- Products table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.products (
            id BIGSERIAL PRIMARY KEY,
            uuid UUID UNIQUE DEFAULT uuid_generate_v4(),
            name VARCHAR(200) NOT NULL,
            description TEXT,
            sku VARCHAR(100) UNIQUE,
            barcode VARCHAR(100),
            cost_price DECIMAL(15,2),
            selling_price DECIMAL(15,2),
            quantity INTEGER DEFAULT 0,
            reorder_level INTEGER DEFAULT 10,
            store_id UUID REFERENCES %I.stores(id),
            is_active BOOLEAN DEFAULT true,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ', schema_name, schema_name);
    
    -- Categories table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.categories (
            id BIGSERIAL PRIMARY KEY,
            uuid UUID UNIQUE DEFAULT uuid_generate_v4(),
            name VARCHAR(100) NOT NULL,
            description VARCHAR(500),
            is_active BOOLEAN DEFAULT true,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ', schema_name);
    
    -- Warehouses table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.warehouses (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            name VARCHAR(100) NOT NULL,
            description VARCHAR(500),
            address VARCHAR(200),
            city VARCHAR(100),
            state VARCHAR(100),
            postal_code VARCHAR(20),
            country VARCHAR(100),
            is_active BOOLEAN DEFAULT true,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ', schema_name);
    
    -- Sales table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.sales (
            id BIGSERIAL PRIMARY KEY,
            uuid UUID UNIQUE DEFAULT uuid_generate_v4(),
            store_id UUID REFERENCES %I.stores(id),
            total_amount DECIMAL(15,2) NOT NULL,
            discount_amount DECIMAL(15,2) DEFAULT 0,
            tax_amount DECIMAL(15,2) DEFAULT 0,
            net_amount DECIMAL(15,2) NOT NULL,
            payment_method VARCHAR(50),
            status VARCHAR(20) DEFAULT ''COMPLETED'',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ', schema_name, schema_name);
    
    -- Create indexes for better performance
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_users_uuid ON %I.users(uuid)', schema_name, schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_users_email ON %I.users(email)', schema_name, schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_products_store ON %I.products(store_id)', schema_name, schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_products_uuid ON %I.products(uuid)', schema_name, schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_sales_store ON %I.sales(store_id)', schema_name, schema_name);
    
    RAISE NOTICE 'Company schema % created successfully', schema_name;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 2. Function to drop a company schema (for cleanup/testing)
-- ============================================================================

CREATE OR REPLACE FUNCTION drop_company_schema(company_uuid UUID)
RETURNS void AS $$
DECLARE
    schema_name TEXT;
BEGIN
    -- Build schema name
    schema_name := 'company_' || REPLACE(company_uuid::text, '-', '_');
    
    -- Drop the schema and all its objects
    EXECUTE format('DROP SCHEMA IF EXISTS %I CASCADE', schema_name);
    
    RAISE NOTICE 'Company schema % dropped successfully', schema_name;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 3. Trigger to automatically create schema when company is created
-- ============================================================================

CREATE OR REPLACE FUNCTION auto_create_company_schema()
RETURNS TRIGGER AS $$
BEGIN
    -- Create schema for the new company
    PERFORM create_company_schema(NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop trigger if exists and recreate
DROP TRIGGER IF EXISTS trigger_auto_create_company_schema ON companies;

CREATE TRIGGER trigger_auto_create_company_schema
    AFTER INSERT ON companies
    FOR EACH ROW
    EXECUTE FUNCTION auto_create_company_schema();

-- ============================================================================
-- 4. Create schemas for existing companies
-- ============================================================================

-- This will create schemas for any existing companies in the database
DO $$
DECLARE
    company_record RECORD;
BEGIN
    FOR company_record IN SELECT id FROM companies WHERE is_active = true
    LOOP
        PERFORM create_company_schema(company_record.id);
    END LOOP;
END $$;

-- Commit transaction
COMMIT;

-- Log completion
SELECT 'Schema-per-company multi-tenant architecture setup completed successfully' as migration_status;

-- ============================================================================
-- Usage Examples:
-- ============================================================================

-- To manually create a schema for a company:
-- SELECT create_company_schema('550e8400-e29b-41d4-a716-446655440000'::UUID);

-- To manually drop a schema for a company:
-- SELECT drop_company_schema('550e8400-e29b-41d4-a716-446655440000'::UUID);

-- To list all company schemas:
-- SELECT schema_name FROM information_schema.schemata 
-- WHERE schema_name LIKE 'company_%';

-- To verify a company's schema exists:
-- SELECT EXISTS (
--     SELECT 1 FROM information_schema.schemata 
--     WHERE schema_name = 'company_550e8400_e29b_41d4_a716_446655440000'
-- );
