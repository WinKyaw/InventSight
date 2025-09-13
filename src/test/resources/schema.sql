-- H2 Test Database Schema for InventSight
-- This schema is compatible with H2 database for testing
-- Uses UUID functions that work with H2

-- Users table (uses BIGINT id + UUID column)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid UUID NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) DEFAULT 'EMPLOYEE',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'WinKyaw',
    tenant_id UUID
);

-- Stores table (UUID primary key)
CREATE TABLE IF NOT EXISTS stores (
    id UUID PRIMARY KEY,
    store_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    address VARCHAR(200),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    website VARCHAR(200),
    tax_id VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'WinKyaw',
    updated_by VARCHAR(100)
);

-- Products table (UUID primary key)
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    sku VARCHAR(50) UNIQUE NOT NULL,
    store_id UUID NOT NULL,
    original_price DECIMAL(10,2) NOT NULL CHECK (original_price >= 0),
    owner_set_sell_price DECIMAL(10,2) NOT NULL CHECK (owner_set_sell_price >= 0),
    retail_price DECIMAL(10,2) NOT NULL CHECK (retail_price >= 0),
    price DECIMAL(10,2) CHECK (price >= 0), -- Legacy price field
    cost_price DECIMAL(10,2) CHECK (cost_price >= 0),
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    max_quantity INTEGER,
    unit VARCHAR(50),
    location VARCHAR(200),
    expiry_date DATE,
    category VARCHAR(100),
    supplier VARCHAR(100),
    barcode VARCHAR(50),
    low_stock_threshold INTEGER,
    reorder_level INTEGER,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'WinKyaw',
    updated_by VARCHAR(100),
    FOREIGN KEY (store_id) REFERENCES stores(id)
);

-- Employees table  
CREATE TABLE IF NOT EXISTS employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone_number VARCHAR(20),
    title VARCHAR(100) NOT NULL,
    hourly_rate DECIMAL(10,2) NOT NULL CHECK (hourly_rate >= 0),
    bonus DECIMAL(10,2) DEFAULT 0,
    start_date DATE DEFAULT CURRENT_DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    check_in_time TIMESTAMP,
    check_out_time TIMESTAMP,
    is_checked_in BOOLEAN DEFAULT false,
    total_hours_worked DECIMAL(10,2) DEFAULT 0,
    department VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'WinKyaw',
    user_id BIGINT,
    store_id UUID,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (store_id) REFERENCES stores(id)
);

-- Sales table
CREATE TABLE IF NOT EXISTS sales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receipt_number VARCHAR(100) UNIQUE NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL CHECK (subtotal >= 0),
    tax_amount DECIMAL(10,2) NOT NULL CHECK (tax_amount >= 0),
    discount_amount DECIMAL(10,2) DEFAULT 0 CHECK (discount_amount >= 0),
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    payment_method VARCHAR(20),
    customer_name VARCHAR(255),
    customer_email VARCHAR(100),
    customer_phone VARCHAR(20),
    user_id BIGINT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    store_id UUID,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (store_id) REFERENCES stores(id)
);

-- Sale items table (UUID foreign key to products)
CREATE TABLE IF NOT EXISTS sale_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id BIGINT NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
    total_price DECIMAL(10,2) NOT NULL CHECK (total_price >= 0),
    product_name VARCHAR(255),
    product_sku VARCHAR(100),
    FOREIGN KEY (sale_id) REFERENCES sales(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Discount audit log table (UUID foreign keys)
CREATE TABLE IF NOT EXISTS discount_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    store_id UUID NOT NULL,
    product_id UUID NOT NULL,
    attempted_price DECIMAL(10,2) NOT NULL CHECK (attempted_price >= 0),
    original_price DECIMAL(10,2) NOT NULL CHECK (original_price >= 0),
    result VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    approved_by VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(100),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(is_active);
CREATE INDEX IF NOT EXISTS idx_users_uuid ON users(uuid);

CREATE INDEX IF NOT EXISTS idx_stores_name ON stores(store_name);
CREATE INDEX IF NOT EXISTS idx_stores_active ON stores(is_active);

CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_active ON products(is_active);
CREATE INDEX IF NOT EXISTS idx_products_supplier ON products(supplier);
CREATE INDEX IF NOT EXISTS idx_products_store ON products(store_id);

CREATE INDEX IF NOT EXISTS idx_employees_email ON employees(email);
CREATE INDEX IF NOT EXISTS idx_employees_status ON employees(status);
CREATE INDEX IF NOT EXISTS idx_employees_title ON employees(title);
CREATE INDEX IF NOT EXISTS idx_employees_department ON employees(department);
CREATE INDEX IF NOT EXISTS idx_employees_store ON employees(store_id);

CREATE INDEX IF NOT EXISTS idx_sales_receipt ON sales(receipt_number);
CREATE INDEX IF NOT EXISTS idx_sales_created ON sales(created_at);
CREATE INDEX IF NOT EXISTS idx_sales_status ON sales(status);
CREATE INDEX IF NOT EXISTS idx_sales_user ON sales(user_id);
CREATE INDEX IF NOT EXISTS idx_sales_store ON sales(store_id);

CREATE INDEX IF NOT EXISTS idx_sale_items_sale ON sale_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_product ON sale_items(product_id);

CREATE INDEX IF NOT EXISTS idx_discount_audit_log_user ON discount_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_discount_audit_log_store ON discount_audit_log(store_id);
CREATE INDEX IF NOT EXISTS idx_discount_audit_log_product ON discount_audit_log(product_id);
CREATE INDEX IF NOT EXISTS idx_discount_audit_log_timestamp ON discount_audit_log(timestamp);