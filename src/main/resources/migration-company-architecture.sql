-- Company-Centric Multi-Tenant Architecture Migration
-- Generated: 2025-01-27
-- Description: Creates company-centric tables and updates existing tables with company foreign keys

-- Enable UUID extension if not already enabled (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Start transaction
BEGIN;

-- 1. Companies table - Core company entity
CREATE TABLE IF NOT EXISTS companies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    website VARCHAR(200),
    address VARCHAR(200),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    tax_id VARCHAR(50),
    business_registration_number VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    -- Indexes for performance
    CONSTRAINT unique_company_name UNIQUE(name),
    CONSTRAINT unique_company_email UNIQUE(email)
);

-- 2. Company Store User relationship table (replaces UserStoreRole for company-centric approach)
CREATE TABLE IF NOT EXISTS company_store_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL,
    store_id UUID,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE',
    is_active BOOLEAN DEFAULT true,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(100),
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_company_store_user_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_company_store_user_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT fk_company_store_user_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate user-company-store combinations
    CONSTRAINT unique_user_company_store UNIQUE(user_id, company_id, store_id)
);

-- 3. Warehouse Store Relationship table
CREATE TABLE IF NOT EXISTS warehouse_store_relationship (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    warehouse_id UUID NOT NULL,
    store_id UUID NOT NULL,
    company_id UUID NOT NULL,
    relationship_type VARCHAR(50) DEFAULT 'SUPPLY', -- SUPPLY, DISTRIBUTION, BACKUP
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    -- Foreign key constraints  
    CONSTRAINT fk_warehouse_store_rel_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE,
    CONSTRAINT fk_warehouse_store_rel_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT fk_warehouse_store_rel_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate relationships
    CONSTRAINT unique_warehouse_store_company UNIQUE(warehouse_id, store_id, company_id)
);

-- 4. Add company_id to existing stores table (if not exists)
ALTER TABLE stores ADD COLUMN IF NOT EXISTS company_id UUID;
ALTER TABLE stores ADD CONSTRAINT IF NOT EXISTS fk_stores_company 
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- 5. Add company_id to existing warehouses table (if not exists)  
ALTER TABLE warehouses ADD COLUMN IF NOT EXISTS company_id UUID;
ALTER TABLE warehouses ADD CONSTRAINT IF NOT EXISTS fk_warehouses_company 
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- 6. Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_companies_name ON companies(name);
CREATE INDEX IF NOT EXISTS idx_companies_email ON companies(email);
CREATE INDEX IF NOT EXISTS idx_companies_active ON companies(is_active);
CREATE INDEX IF NOT EXISTS idx_companies_created_at ON companies(created_at);

CREATE INDEX IF NOT EXISTS idx_company_store_user_company ON company_store_user(company_id);
CREATE INDEX IF NOT EXISTS idx_company_store_user_store ON company_store_user(store_id);
CREATE INDEX IF NOT EXISTS idx_company_store_user_user ON company_store_user(user_id);
CREATE INDEX IF NOT EXISTS idx_company_store_user_role ON company_store_user(role);
CREATE INDEX IF NOT EXISTS idx_company_store_user_active ON company_store_user(is_active);

CREATE INDEX IF NOT EXISTS idx_warehouse_store_rel_warehouse ON warehouse_store_relationship(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_store_rel_store ON warehouse_store_relationship(store_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_store_rel_company ON warehouse_store_relationship(company_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_store_rel_active ON warehouse_store_relationship(is_active);

CREATE INDEX IF NOT EXISTS idx_stores_company ON stores(company_id);
CREATE INDEX IF NOT EXISTS idx_warehouses_company ON warehouses(company_id);

-- Commit transaction
COMMIT;

-- Log completion
SELECT 'Company-centric multi-tenant architecture tables created successfully' as migration_status;