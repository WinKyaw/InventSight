-- Migration V1_8: Create employee_relationships table
-- Tracks the relationship between employees, employers (creators), stores, and companies
-- Simplified to use IDs only to avoid circular JSON serialization issues

-- Create employee_relationships table (simple ID mapping)
CREATE TABLE IF NOT EXISTS employee_relationships (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    employer_id UUID NOT NULL,
    store_id UUID NOT NULL,
    company_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_emp_rel_employee ON employee_relationships(employee_id);
CREATE INDEX IF NOT EXISTS idx_emp_rel_employer ON employee_relationships(employer_id);
CREATE INDEX IF NOT EXISTS idx_emp_rel_store ON employee_relationships(store_id);
CREATE INDEX IF NOT EXISTS idx_emp_rel_company ON employee_relationships(company_id);

-- Note: No foreign key constraints to keep it simple and avoid circular issues

-- Add comment to the table
COMMENT ON TABLE employee_relationships IS 'Tracks relationships between employees, employers (creators), stores, and companies using IDs only';
COMMENT ON COLUMN employee_relationships.employee_id IS 'The employee ID in this relationship';
COMMENT ON COLUMN employee_relationships.employer_id IS 'The user ID who created this employee record';
COMMENT ON COLUMN employee_relationships.store_id IS 'The store ID this employee is assigned to';
COMMENT ON COLUMN employee_relationships.company_id IS 'The company ID this employee belongs to';
