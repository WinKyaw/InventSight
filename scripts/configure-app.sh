#!/bin/bash
# InventSight Backend - Step 2: Application Configuration
# Current Date and Time (UTC): 2025-08-26 23:55:49
# Current User's Login: WinKyaw

echo "⚙️ InventSight - Step 2: Application Configuration"
echo "📅 Current Date and Time (UTC): 2025-08-26 23:55:49"
echo "👤 Current User's Login: WinKyaw"
echo ""

# Check if we're in the right location
if [ ! -f "../pom.xml" ]; then
    echo "❌ Error: Please run this script from the InventSight/scripts/ directory"
    echo "📁 Current location should be: InventSight/scripts/"
    exit 1
fi

# Create application.yml configuration
echo "📝 Creating InventSight application configuration..."

mkdir -p ../src/main/resources

cat > ../src/main/resources/application.yml << 'EOF'
# InventSight - Intelligent Inventory & POS System Configuration
# Current Date and Time (UTC): 2025-08-26 23:55:49
# Current User's Login: WinKyaw
# Repository: WinKyaw/InventSight

spring:
  application:
    name: inventsight-backend
    
  # PostgreSQL Configuration (Core Business Data)
  datasource:
    url: jdbc:postgresql://localhost:5432/inventsight_db
    username: inventsight_user
    password: inventsight_secure_password_2025
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
    
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
        
  # MongoDB Configuration (Analytics & Logs)
  data:
    mongodb:
      uri: mongodb://localhost:27017/inventsight_analytics
      auto-index-creation: true
      
    # Redis Configuration (Cache & Sessions)  
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-wait: -1ms
          max-idle: 8
          min-idle: 0
      
# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /api
  compression:
    enabled: true
  error:
    include-message: always
    include-binding-errors: always
    
# Security Configuration
inventsight:
  security:
    jwt:
      secret: inventsight-ultra-secure-jwt-secret-winkyaw-2025-08-26-23-55-49
      expiration: 86400000 # 24 hours
      
  system:
    name: "InventSight - Intelligent Inventory & POS System"
    version: "1.0.0"
    current-user: "WinKyaw"
    timezone: "UTC"
    current-datetime: "2025-08-26 23:55:49"
    environment: "production"
    
  database:
    sql-enabled: true
    nosql-enabled: true
    cache-enabled: true
    
  features:
    real-time-sync: true
    analytics-tracking: true
    audit-logging: true
    intelligent-forecasting: true
    mobile-optimization: true

# Logging Configuration
logging:
  level:
    com.inventsight: INFO
    org.springframework.security: WARN
    org.hibernate.SQL: WARN
    org.springframework.web: WARN
  file:
    name: logs/inventsight.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    console: "%d{HH:mm:ss} %-5level %logger{36} - %msg%n"
    
# Management/Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
  info:
    env:
      enabled: true
      
# OpenAPI Documentation
springdoc:
  api-docs:
    path: /docs
  swagger-ui:
    path: /swagger
    operationsSorter: method
    tagsSorter: alpha
  info:
    title: "InventSight API"
    description: "Intelligent Inventory & POS System API"
    version: "1.0.0"
    contact:
      name: "WinKyaw"
      email: "winkyaw@inventsight.com"

# Custom InventSight Properties
inventsight-config:
  initialized-by: "WinKyaw"
  initialized-at: "2025-08-26 23:55:49"
  database-setup-completed: true
  mobile-ready: true
  production-ready: true
EOF

echo "✅ Application configuration created"

# Create development profile
echo "📝 Creating development profile configuration..."

cat > ../src/main/resources/application-dev.yml << 'EOF'
# InventSight Development Configuration
# Current Date and Time (UTC): 2025-08-26 23:55:49
# Current User's Login: WinKyaw

spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
        
logging:
  level:
    com.inventsight: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    
inventsight:
  system:
    environment: "development"
    
  security:
    jwt:
      expiration: 86400000 # 24 hours for development
EOF

echo "✅ Development profile created"

# Create production profile
echo "📝 Creating production profile configuration..."

cat > ../src/main/resources/application-prod.yml << 'EOF'
# InventSight Production Configuration
# Current Date and Time (UTC): 2025-08-26 23:55:49
# Current User's Login: WinKyaw

spring:
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
      
logging:
  level:
    com.inventsight: INFO
    org.springframework.security: WARN
    org.hibernate: WARN
    
inventsight:
  system:
    environment: "production"
    
  security:
    jwt:
      expiration: 3600000 # 1 hour for production
EOF

echo "✅ Production profile created"

# Create database initialization script
echo "📝 Creating database initialization SQL..."

cat > ../src/main/resources/data.sql << 'EOF'
-- InventSight Database Initialization Data
-- Current Date and Time (UTC): 2025-08-26 23:55:49
-- Current User's Login: WinKyaw

-- Insert default admin user (password: 'password' encoded with BCrypt)
INSERT INTO users (username, email, password, first_name, last_name, role, created_by, created_at) 
VALUES ('winkyaw', 'winkyaw@inventsight.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqyc/Zi/letq8DkMaVrLhQ6', 'Win', 'Kyaw', 'ADMIN', 'WinKyaw', CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Sample products for testing
INSERT INTO products (name, description, price, quantity, category, sku, supplier, created_by, created_at) VALUES
('Premium Coffee Blend', 'High-quality Arabica coffee beans', 4.50, 25, 'Beverages', 'INV-COF-001', 'Coffee Masters Co.', 'WinKyaw', CURRENT_TIMESTAMP),
('Artisan Croissant', 'Buttery flaky French croissant', 3.75, 20, 'Bakery', 'INV-CRO-001', 'Fresh Bakery Ltd.', 'WinKyaw', CURRENT_TIMESTAMP),
('Organic Green Tea', 'Premium organic green tea', 3.25, 30, 'Beverages', 'INV-TEA-001', 'Tea Garden Inc.', 'WinKyaw', CURRENT_TIMESTAMP),
('Fresh Sandwich', 'Daily made sandwich with premium ingredients', 8.99, 15, 'Food', 'INV-SAN-001', 'Gourmet Foods', 'WinKyaw', CURRENT_TIMESTAMP),
('Energy Bar', 'High-protein energy bar', 2.99, 40, 'Snacks', 'INV-BAR-001', 'Nutrition Plus', 'WinKyaw', CURRENT_TIMESTAMP)
ON CONFLICT (sku) DO NOTHING;

-- Sample employees
INSERT INTO employees (first_name, last_name, email, phone_number, title, hourly_rate, department, created_by, created_at) VALUES
('Alice', 'Johnson', 'alice@inventsight.com', '(555) 123-4567', 'Senior Barista', 18.50, 'Operations', 'WinKyaw', CURRENT_TIMESTAMP),
('Bob', 'Smith', 'bob@inventsight.com', '(555) 987-6543', 'Shift Supervisor', 22.00, 'Management', 'WinKyaw', CURRENT_TIMESTAMP),
('Carol', 'Davis', 'carol@inventsight.com', '(555) 456-7890', 'Sales Associate', 16.75, 'Sales', 'WinKyaw', CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Log initialization
SELECT 'InventSight database initialization completed at 2025-08-26 23:55:49 by WinKyaw' as initialization_status;
EOF

echo "✅ Database initialization script created"

# Verify configuration files
echo "🔍 Verifying configuration files..."

CONFIG_FILES=(
    "../src/main/resources/application.yml"
    "../src/main/resources/application-dev.yml"  
    "../src/main/resources/application-prod.yml"
    "../src/main/resources/data.sql"
)

ALL_GOOD=true
for file in "${CONFIG_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $(basename $file) created successfully"
    else
        echo "❌ $(basename $file) creation failed"
        ALL_GOOD=false
    fi
done

if [ "$ALL_GOOD" = true ]; then
    echo ""
    echo "🎉 InventSight Application Configuration Complete!"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📅 Configuration completed: 2025-08-26 23:55:49"
    echo "👤 Configured by: WinKyaw"
    echo ""
    echo "📄 Configuration Files Created:"
    echo "   ⚙️  application.yml (Main configuration)"
    echo "   🔧 application-dev.yml (Development profile)"
    echo "   🚀 application-prod.yml (Production profile)"
    echo "   🗄️  data.sql (Initial data)"
    echo ""
    echo "🔐 Default Admin Account:"
    echo "   📧 Email: winkyaw@inventsight.com"
    echo "   🔑 Password: password"
    echo "   ⚠️  Change password in production!"
    echo ""
    echo "🚀 Next Step: Run build and start script"
    echo "   ./3-build-and-run.sh"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
else
    echo ""
    echo "❌ Some configuration files failed to create"
    echo "🔧 Please check file permissions and try again"
    exit 1
fi