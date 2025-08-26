-- InventSight - Intelligent Inventory & POS System Database Schema
-- Generated: 2025-08-26 09:12:40
-- Current User's Login: WinKyaw

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
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
    created_by VARCHAR(100) DEFAULT 'WinKyaw'
);

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    category VARCHAR(100) NOT NULL,
    sku VARCHAR(100) UNIQUE,
    barcode VARCHAR(100),
    supplier VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    low_stock_threshold INTEGER DEFAULT 10,
    reorder_level INTEGER DEFAULT 5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'WinKyaw'
);

-- Employees table  
CREATE TABLE IF NOT EXISTS employees (
    id BIGSERIAL PRIMARY KEY,
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
    user_id BIGINT REFERENCES users(id)
);

-- Sales table
CREATE TABLE IF NOT EXISTS sales (
    id BIGSERIAL PRIMARY KEY,
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
    user_id BIGINT REFERENCES users(id),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sale items table
CREATE TABLE IF NOT EXISTS sale_items (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
    total_price DECIMAL(10,2) NOT NULL CHECK (total_price >= 0),
    product_name VARCHAR(255),
    product_sku VARCHAR(100)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(is_active);

CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_active ON products(is_active);
CREATE INDEX IF NOT EXISTS idx_products_supplier ON products(supplier);

CREATE INDEX IF NOT EXISTS idx_employees_email ON employees(email);
CREATE INDEX IF NOT EXISTS idx_employees_status ON employees(status);
CREATE INDEX IF NOT EXISTS idx_employees_title ON employees(title);
CREATE INDEX IF NOT EXISTS idx_employees_department ON employees(department);

CREATE INDEX IF NOT EXISTS idx_sales_receipt ON sales(receipt_number);
CREATE INDEX IF NOT EXISTS idx_sales_created ON sales(created_at);
CREATE INDEX IF NOT EXISTS idx_sales_status ON sales(status);
CREATE INDEX IF NOT EXISTS idx_sales_user ON sales(user_id);

CREATE INDEX IF NOT EXISTS idx_sale_items_sale ON sale_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_product ON sale_items(product_id);

-- Insert initial data for InventSight system
INSERT INTO users (username, email, password, first_name, last_name, role, created_by) 
VALUES ('winkyaw', 'winkyaw@inventsight.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Win', 'Kyaw', 'ADMIN', 'WinKyaw')
ON CONFLICT (email) DO NOTHING;

-- Sample products data for InventSight
INSERT INTO products (name, description, price, quantity, category, sku, supplier, created_by) VALUES
('Premium Coffee Blend', 'High-quality Arabica coffee beans', 4.50, 25, 'Beverages', 'INV-COF-001', 'Coffee Co.', 'WinKyaw'),
('Gourmet Sandwich', 'Fresh sandwich with premium ingredients', 8.99, 15, 'Food', 'INV-SAN-001', 'Fresh Foods Ltd.', 'WinKyaw'),
('Artisan Croissant', 'Buttery flaky French croissant', 3.75, 20, 'Bakery', 'INV-CRO-001', 'Bakery Plus', 'WinKyaw'),
('Organic Tea Collection', 'Premium organic tea selection', 3.25, 30, 'Beverages', 'INV-TEA-001', 'Tea Masters', 'WinKyaw'),
('Fresh Baked Muffin', 'Daily fresh baked muffins', 2.99, 12, 'Bakery', 'INV-MUF-001', 'Bakery Plus', 'WinKyaw'),
('Garden Fresh Salad', 'Crisp garden salad with dressing', 6.50, 8, 'Food', 'INV-SAL-001', 'Green Gardens', 'WinKyaw'),
('Energy Drink', 'Natural energy boost drink', 2.75, 35, 'Beverages', 'INV-ENE-001', 'Drink Co.', 'WinKyaw'),
('Protein Bar', 'High-protein nutrition bar', 3.99, 22, 'Snacks', 'INV-PRO-001', 'Nutrition Inc.', 'WinKyaw')
ON CONFLICT (sku) DO NOTHING;

-- Sample employees data for InventSight
INSERT INTO employees (first_name, last_name, email, phone_number, title, hourly_rate, bonus, department, created_by) VALUES
('John', 'Doe', 'john.doe@inventsight.com', '(555) 123-4567', 'Senior Barista', 18.50, 1200, 'Operations', 'WinKyaw'),
('Sarah', 'Johnson', 'sarah.johnson@inventsight.com', '(555) 987-6543', 'Shift Supervisor', 22.00, 2500, 'Management', 'WinKyaw'),
('Mike', 'Chen', 'mike.chen@inventsight.com', '(555) 456-7890', 'Sales Associate', 16.75, 500, 'Sales', 'WinKyaw'),
('Emma', 'Williams', 'emma.williams@inventsight.com', '(555) 321-0987', 'Assistant Manager', 25.00, 3500, 'Management', 'WinKyaw'),
('David', 'Rodriguez', 'david.rodriguez@inventsight.com', '(555) 654-3210', 'Inventory Specialist', 19.25, 800, 'Operations', 'WinKyaw'),
('Lisa', 'Thompson', 'lisa.thompson@inventsight.com', '(555) 789-0123', 'Customer Service Lead', 17.50, 1000, 'Customer Service', 'WinKyaw')
ON CONFLICT (email) DO NOTHING;

COMMIT;

-- Log initialization
SELECT 'InventSight Database Schema initialized successfully at 2025-08-26 09:12:40 by WinKyaw' as initialization_status;