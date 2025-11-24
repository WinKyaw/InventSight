-- Migration V1_8: Create employee_relationships table
-- Tracks the relationship between employees, employers (creators), stores, and companies

-- Create employee_relationships table
CREATE TABLE IF NOT EXISTS employee_relationships (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    employer_id BIGINT NOT NULL,
    store_id UUID NOT NULL,
    company_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    relationship_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    
    -- Foreign key constraints
    CONSTRAINT fk_employee_relationship_employee 
        FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_relationship_employer 
        FOREIGN KEY (employer_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_relationship_store 
        FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_relationship_company 
        FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE RESTRICT
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_employee_relationships_employee_id ON employee_relationships(employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_relationships_employer_id ON employee_relationships(employer_id);
CREATE INDEX IF NOT EXISTS idx_employee_relationships_store_id ON employee_relationships(store_id);
CREATE INDEX IF NOT EXISTS idx_employee_relationships_company_id ON employee_relationships(company_id);
CREATE INDEX IF NOT EXISTS idx_employee_relationships_is_active ON employee_relationships(is_active);
CREATE INDEX IF NOT EXISTS idx_employee_relationships_status ON employee_relationships(relationship_status);

-- Add comment to the table
COMMENT ON TABLE employee_relationships IS 'Tracks relationships between employees, employers (creators), stores, and companies';
COMMENT ON COLUMN employee_relationships.employee_id IS 'The employee in this relationship';
COMMENT ON COLUMN employee_relationships.employer_id IS 'The user who created this employee record';
COMMENT ON COLUMN employee_relationships.store_id IS 'The store this employee is assigned to';
COMMENT ON COLUMN employee_relationships.company_id IS 'The company this employee belongs to';
COMMENT ON COLUMN employee_relationships.relationship_status IS 'Status of the relationship: ACTIVE, INACTIVE, SUSPENDED, or TERMINATED';
