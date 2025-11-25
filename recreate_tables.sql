-- ============================================
-- STEP 1: Drop tables in correct order (respect foreign keys)
-- ============================================

DROP TABLE IF EXISTS employee_relationships CASCADE;
DROP TABLE IF EXISTS employees CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Note: You may need to drop other tables that reference users/employees
-- Add them here if needed

-- ============================================
-- STEP 2: Recreate users table with UUID
-- ============================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    full_name VARCHAR(255),
    phone VARCHAR(50),
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    created_by VARCHAR(255),
    tenant_id UUID,
    default_tenant_id UUID,
    subscription_level VARCHAR(50) DEFAULT 'FREE',
    uuid UUID DEFAULT gen_random_uuid(),
    
    -- Add role check constraint with all roles
    CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'USER', 'MANAGER', 'OWNER', 'EMPLOYEE', 'CUSTOMER', 'MERCHANT', 'PARTNER'))
);

-- Create indexes for users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_tenant ON users(tenant_id);

-- ============================================
-- STEP 3: Recreate employees table with UUID
-- ============================================

CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(50),
    title VARCHAR(255),
    department VARCHAR(255),
    hourly_rate DECIMAL(10, 2),
    bonus DECIMAL(10, 2) DEFAULT 0.00,
    start_date DATE DEFAULT CURRENT_DATE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    store_id UUID NOT NULL,
    company_id UUID NOT NULL,
    user_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    
    -- Foreign keys
    CONSTRAINT fk_employee_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_employee_company FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_employee_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create indexes for employees
CREATE INDEX idx_employees_email ON employees(email);
CREATE INDEX idx_employees_store ON employees(store_id);
CREATE INDEX idx_employees_company ON employees(company_id);
CREATE INDEX idx_employees_user ON employees(user_id);

-- ============================================
-- STEP 4: Create employee_relationships table
-- ============================================

CREATE TABLE employee_relationships (
    id BIGSERIAL PRIMARY KEY,
    employee_id UUID NOT NULL,
    employer_id UUID NOT NULL,
    store_id UUID NOT NULL,
    company_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    
    -- Foreign key constraints
    CONSTRAINT fk_emp_rel_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_emp_rel_employer FOREIGN KEY (employer_id) REFERENCES users(id),
    CONSTRAINT fk_emp_rel_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_emp_rel_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

-- Create indexes for employee_relationships
CREATE INDEX idx_emp_rel_employee ON employee_relationships(employee_id);
CREATE INDEX idx_emp_rel_employer ON employee_relationships(employer_id);
CREATE INDEX idx_emp_rel_store ON employee_relationships(store_id);
CREATE INDEX idx_emp_rel_company ON employee_relationships(company_id);