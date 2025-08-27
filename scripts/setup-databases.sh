# Make sure you're in the InventSight project root
# Create the scripts directory
mkdir -p scripts

# Create the database setup script
cat > scripts/1-setup-databases.sh << 'EOF'
#!/bin/bash
# InventSight Backend - Step 1: Database Setup
# Current Date and Time (UTC): 2025-08-27 00:06:16
# Current User's Login: WinKyaw

echo "🗄️ InventSight - Step 1: Database Setup"
echo "📅 Current Date and Time (UTC): 2025-08-27 00:06:16"
echo "👤 Current User's Login: WinKyaw"
echo ""

# Create logs directory if it doesn't exist
mkdir -p ../logs

# Check if we're in the right location
if [ ! -f "../pom.xml" ]; then
    echo "❌ Error: Please run this script from the InventSight/scripts/ directory"
    echo "📁 Current location should be: InventSight/scripts/"
    echo "🔧 Fix: cd /path/to/InventSight/scripts && ./1-setup-databases.sh"
    exit 1
fi

echo "✅ InventSight project structure verified"
echo ""

# PostgreSQL Setup (Core Business Data)
echo "🐘 Setting up PostgreSQL for InventSight core data..."
echo "📊 Database: inventsight_db"
echo "👤 User: inventsight_user"

# Check if PostgreSQL is installed
if ! command -v psql &> /dev/null; then
    echo "⚠️  PostgreSQL not found. Installing..."
    
    # macOS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "🍎 Installing PostgreSQL on macOS..."
        if ! command -v brew &> /dev/null; then
            echo "❌ Homebrew not found. Please install Homebrew first:"
            echo "   /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
            exit 1
        fi
        brew install postgresql@14
        brew services start postgresql@14
        sleep 10
    
    # Ubuntu/Debian
    elif [[ -f /etc/debian_version ]]; then
        echo "🐧 Installing PostgreSQL on Ubuntu/Debian..."
        sudo apt update
        sudo apt install -y postgresql postgresql-contrib
        sudo systemctl start postgresql
        sudo systemctl enable postgresql
        sleep 10
    
    # CentOS/RHEL/Fedora
    elif [[ -f /etc/redhat-release ]]; then
        echo "🔴 Installing PostgreSQL on CentOS/RHEL/Fedora..."
        sudo dnf install -y postgresql postgresql-server postgresql-contrib
        sudo postgresql-setup --initdb
        sudo systemctl start postgresql
        sudo systemctl enable postgresql
        sleep 10
    else
        echo "❌ Unsupported OS. Please install PostgreSQL manually"
        echo "📖 Visit: https://www.postgresql.org/download/"
        echo ""
        echo "🔧 Manual installation commands:"
        echo "   macOS: brew install postgresql@14"
        echo "   Ubuntu: sudo apt install postgresql postgresql-contrib"
        echo "   CentOS: sudo dnf install postgresql postgresql-server"
        exit 1
    fi
else
    echo "✅ PostgreSQL found"
    # Start PostgreSQL if not running
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "🔧 Starting PostgreSQL service..."
        brew services start postgresql@14 2>/dev/null || true
        brew services start postgresql 2>/dev/null || true
    else
        echo "🔧 Starting PostgreSQL service..."
        sudo systemctl start postgresql 2>/dev/null || true
    fi
    sleep 5
fi

# Check if PostgreSQL is running
echo "🔍 Checking PostgreSQL status..."
if pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
    echo "✅ PostgreSQL is running"
else
    echo "⚠️  PostgreSQL is not responding. Attempting to start..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew services restart postgresql@14
        brew services restart postgresql
    else
        sudo systemctl restart postgresql
    fi
    sleep 10
    
    if pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
        echo "✅ PostgreSQL started successfully"
    else
        echo "❌ PostgreSQL failed to start. Please check the service manually:"
        echo "   macOS: brew services list | grep postgresql"
        echo "   Linux: sudo systemctl status postgresql"
        exit 1
    fi
fi

# Create InventSight database and user
echo "🔧 Creating InventSight database and user..."

# Create database setup SQL with better error handling
cat > /tmp/setup_inventsight_db.sql << 'EOF'
-- InventSight Database Setup
-- Current Date and Time (UTC): 2025-08-27 00:06:16
-- Current User's Login: WinKyaw

-- Terminate existing connections to the database (if it exists)
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity 
WHERE datname = 'inventsight_db' AND pid <> pg_backend_pid();

-- Drop existing database and user if they exist (for clean setup)
DROP DATABASE IF EXISTS inventsight_db;
DROP USER IF EXISTS inventsight_user;

-- Create InventSight user with password
CREATE USER inventsight_user WITH PASSWORD 'inventsight_secure_password_2025';

-- Grant user creation privileges
ALTER USER inventsight_user CREATEDB;
ALTER USER inventsight_user CREATEROLE;

-- Create InventSight database
CREATE DATABASE inventsight_db OWNER inventsight_user;

-- Grant all privileges
GRANT ALL PRIVILEGES ON DATABASE inventsight_db TO inventsight_user;

-- Connect to the database and set up permissions
\c inventsight_db

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO inventsight_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO inventsight_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO inventsight_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO inventsight_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO inventsight_user;

-- Create a test table to verify setup
CREATE TABLE inventsight_test (
    id SERIAL PRIMARY KEY,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert test data
INSERT INTO inventsight_test (message) VALUES ('InventSight database initialized successfully at 2025-08-27 00:06:16 by WinKyaw');

-- Verify setup
SELECT 'InventSight PostgreSQL database setup completed successfully!' as status, COUNT(*) as test_records FROM inventsight_test;
EOF

# Execute database setup
echo "📋 Executing PostgreSQL database setup..."
if sudo -u postgres psql -f /tmp/setup_inventsight_db.sql 2>/dev/null; then
    echo "✅ InventSight PostgreSQL database created successfully"
elif psql postgres -f /tmp/setup_inventsight_db.sql 2>/dev/null; then
    echo "✅ InventSight PostgreSQL database created successfully (user method)"
else
    echo "⚠️  Standard database setup failed. Trying alternative methods..."
    
    # Try alternative method 1: Direct commands
    echo "🔧 Attempting alternative setup method..."
    
    # Method 1: Using createuser and createdb commands
    if command -v createuser &> /dev/null && command -v createdb &> /dev/null; then
        sudo -u postgres dropdb inventsight_db 2>/dev/null || true
        sudo -u postgres dropuser inventsight_user 2>/dev/null || true
        
        if sudo -u postgres createuser -d -r inventsight_user; then
            sudo -u postgres psql -c "ALTER USER inventsight_user PASSWORD 'inventsight_secure_password_2025';"
            sudo -u postgres createdb -O inventsight_user inventsight_db
            echo "✅ PostgreSQL database created using alternative method"
        else
            # Method 2: Interactive setup
            echo "🔧 Please set up PostgreSQL manually:"
            echo ""
            echo "📋 Run these commands:"
            echo "sudo -u postgres psql"
            echo "Then in PostgreSQL prompt:"
            echo "CREATE USER inventsight_user WITH PASSWORD 'inventsight_secure_password_2025';"
            echo "ALTER USER inventsight_user CREATEDB;"
            echo "CREATE DATABASE inventsight_db OWNER inventsight_user;"
            echo "\\q"
            echo ""
            read -p "Press Enter after you've completed the manual setup..."
        fi
    else
        echo "❌ Unable to create database automatically. Manual setup required."
        exit 1
    fi
fi

# Clean up temporary file
rm -f /tmp/setup_inventsight_db.sql

# Test PostgreSQL connection
echo "🧪 Testing PostgreSQL connection..."
if PGPASSWORD='inventsight_secure_password_2025' psql -h localhost -U inventsight_user -d inventsight_db -c "SELECT 'InventSight PostgreSQL connection successful at 2025-08-27 00:06:16!' as test, version();" 2>/dev/null; then
    echo "✅ PostgreSQL connection test PASSED"
else
    echo "⚠️  PostgreSQL connection test failed. Database might still work."
    echo "🔧 Manual test command:"
    echo "PGPASSWORD='inventsight_secure_password_2025' psql -h localhost -U inventsight_user -d inventsight_db"
fi

echo ""

# MongoDB Setup (Analytics & Logging)
echo "🍃 Setting up MongoDB for InventSight analytics..."
echo "📊 Database: inventsight_analytics"

if ! command -v mongosh &> /dev/null && ! command -v mongo &> /dev/null; then
    echo "⚠️  MongoDB not found. Installing..."
    
    # macOS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "🍎 Installing MongoDB on macOS..."
        if ! command -v brew &> /dev/null; then
            echo "❌ Homebrew not found. Please install Homebrew first"
            exit 1
        fi
        brew tap mongodb/brew
        brew install mongodb-community@7.0
        brew services start mongodb/brew/mongodb-community
        sleep 15
    
    # Ubuntu/Debian
    elif [[ -f /etc/debian_version ]]; then
        echo "🐧 Installing MongoDB on Ubuntu/Debian..."
        curl -fsSL https://pgp.mongodb.com/server-7.0.asc | sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor
        echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
        sudo apt update
        sudo apt install -y mongodb-org
        sudo systemctl start mongod
        sudo systemctl enable mongod
        sleep 15
    
    # CentOS/RHEL/Fedora
    elif [[ -f /etc/redhat-release ]]; then
        echo "🔴 Installing MongoDB on CentOS/RHEL/Fedora..."
        sudo tee /etc/yum.repos.d/mongodb-org-7.0.repo << 'MONGOREPO'
[mongodb-org-7.0]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/redhat/$releasever/mongodb-org/7.0/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://pgp.mongodb.com/server-7.0.asc
MONGOREPO
        sudo dnf install -y mongodb-org
        sudo systemctl start mongod
        sudo systemctl enable mongod
        sleep 15
    else
        echo "❌ Unsupported OS for automatic MongoDB installation"
        echo "📖 Please install MongoDB manually: https://docs.mongodb.com/manual/installation/"
        echo ""
        echo "🔧 Manual installation:"
        echo "   macOS: brew install mongodb-community"
        echo "   Ubuntu: Follow MongoDB official Ubuntu guide"
        echo ""
        echo "⏭️  Skipping MongoDB setup for now..."
    fi
else
    echo "✅ MongoDB client found"
    # Start MongoDB if not running
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "🔧 Starting MongoDB service..."
        brew services start mongodb/brew/mongodb-community 2>/dev/null || true
        brew services start mongodb-community 2>/dev/null || true
    else
        echo "🔧 Starting MongoDB service..."
        sudo systemctl start mongod 2>/dev/null || true
    fi
    sleep 10
fi

# Check if MongoDB is running
echo "🔍 Checking MongoDB status..."
MONGO_RUNNING=false

if command -v mongosh &> /dev/null; then
    if mongosh --eval "db.adminCommand('ping')" --quiet &>/dev/null; then
        MONGO_RUNNING=true
        echo "✅ MongoDB is running (mongosh)"
    fi
elif command -v mongo &> /dev/null; then
    if mongo --eval "db.adminCommand('ping')" --quiet &>/dev/null; then
        MONGO_RUNNING=true
        echo "✅ MongoDB is running (mongo)"
    fi
fi

if [ "$MONGO_RUNNING" = false ]; then
    echo "⚠️  MongoDB is not responding. Checking service..."
    
    # Try to start MongoDB
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew services restart mongodb/brew/mongodb-community
        brew services restart mongodb-community
    else
        sudo systemctl restart mongod
    fi
    
    sleep 15
    
    # Recheck
    if command -v mongosh &> /dev/null; then
        if mongosh --eval "db.adminCommand('ping')" --quiet &>/dev/null; then
            MONGO_RUNNING=true
            echo "✅ MongoDB started successfully"
        fi
    elif command -v mongo &> /dev/null; then
        if mongo --eval "db.adminCommand('ping')" --quiet &>/dev/null; then
            MONGO_RUNNING=true
            echo "✅ MongoDB started successfully"
        fi
    fi
fi

if [ "$MONGO_RUNNING" = true ]; then
    # Initialize MongoDB for InventSight
    echo "🔧 Initializing InventSight MongoDB database..."

    # Create MongoDB initialization script
    cat > /tmp/init_inventsight_mongo.js << 'MONGOEOF'
// InventSight MongoDB Initialization
// Current Date and Time (UTC): 2025-08-27 00:06:16
// Current User's Login: WinKyaw

// Switch to InventSight analytics database
use inventsight_analytics;

// Drop existing collections for clean setup
db.activity_logs.drop();
db.inventory_analytics.drop();

// Create collections with indexes for optimal performance
db.createCollection("activity_logs");
db.activity_logs.createIndex({ "timestamp": -1 });
db.activity_logs.createIndex({ "userId": 1, "timestamp": -1 });
db.activity_logs.createIndex({ "action": 1 });
db.activity_logs.createIndex({ "entityType": 1 });

db.createCollection("inventory_analytics");
db.inventory_analytics.createIndex({ "date": -1, "period": 1 });
db.inventory_analytics.createIndex({ "createdAt": -1 });

// Insert initial system log
db.activity_logs.insertOne({
    "userId": "SYSTEM",
    "username": "WinKyaw",
    "action": "DATABASE_INITIALIZED",
    "entityType": "SYSTEM",
    "description": "InventSight MongoDB database initialized successfully",
    "timestamp": new Date("2025-08-27T00:06:16.000Z"),
    "module": "SYSTEM",
    "severity": "INFO",
    "metadata": {
        "system": "InventSight",
        "version": "1.0.0",
        "initializedBy": "WinKyaw",
        "environment": "development"
    }
});

// Insert sample analytics data
db.inventory_analytics.insertOne({
    "date": new Date("2025-08-27"),
    "period": "DAILY",
    "totalProducts": 0,
    "totalInventoryValue": 0,
    "lowStockProducts": 0,
    "outOfStockProducts": 0,
    "totalRevenue": 0,
    "totalSales": 0,
    "averageOrderValue": 0,
    "revenueGrowthPercent": 0,
    "salesGrowthPercent": 0,
    "inventoryTurnoverRate": 0,
    "createdAt": new Date("2025-08-27T00:06:16.000Z"),
    "createdBy": "WinKyaw"
});

print("✅ InventSight MongoDB database initialized successfully at 2025-08-27 00:06:16 by WinKyaw!");
print("📊 Collections created: activity_logs, inventory_analytics");
print("🔍 Database: inventsight_analytics");
MONGOEOF

    # Execute MongoDB initialization
    echo "📋 Executing MongoDB initialization..."
    if command -v mongosh &> /dev/null; then
        mongosh /tmp/init_inventsight_mongo.js
    elif command -v mongo &> /dev/null; then
        mongo /tmp/init_inventsight_mongo.js
    fi

    # Clean up
    rm -f /tmp/init_inventsight_mongo.js

    # Test MongoDB connection
    echo "🧪 Testing MongoDB connection..."
    if command -v mongosh &> /dev/null; then
        TEST_RESULT=$(mongosh inventsight_analytics --eval "db.activity_logs.countDocuments()" --quiet 2>/dev/null || echo "0")
        if [ "$TEST_RESULT" -gt 0 ]; then
            echo "✅ MongoDB connection test PASSED ($TEST_RESULT records found)"
        else
            echo "⚠️  MongoDB connection test unclear, but service should be ready"
        fi
    elif command -v mongo &> /dev/null; then
        TEST_RESULT=$(mongo inventsight_analytics --eval "db.activity_logs.count()" --quiet 2>/dev/null | tail -n1 || echo "0")
        if [ "$TEST_RESULT" -gt 0 ]; then
            echo "✅ MongoDB connection test PASSED ($TEST_RESULT records found)"
        else
            echo "⚠️  MongoDB connection test unclear, but service should be ready"
        fi
    fi
else
    echo "❌ MongoDB is not running. Some InventSight features may not work properly."
    echo "🔧 Manual start commands:"
    echo "   macOS: brew services start mongodb/brew/mongodb-community"
    echo "   Linux: sudo systemctl start mongod"
fi

echo ""

# Redis Setup (Caching & Sessions)
echo "⚡ Setting up Redis for InventSight caching..."

if ! command -v redis-server &> /dev/null; then
    echo "⚠️  Redis not found. Installing..."
    
    # macOS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "🍎 Installing Redis on macOS..."
        if ! command -v brew &> /dev/null; then
            echo "❌ Homebrew not found. Please install Homebrew first"
            exit 1
        fi
        brew install redis
        brew services start redis
        sleep 5
    
    # Ubuntu/Debian
    elif [[ -f /etc/debian_version ]]; then
        echo "🐧 Installing Redis on Ubuntu/Debian..."
        sudo apt update
        sudo apt install -y redis-server
        sudo systemctl start redis-server
        sudo systemctl enable redis-server
        sleep 5
    
    # CentOS/RHEL/Fedora
    elif [[ -f /etc/redhat-release ]]; then
        echo "🔴 Installing Redis on CentOS/RHEL/Fedora..."
        sudo dnf install -y redis
        sudo systemctl start redis
        sudo systemctl enable redis
        sleep 5
    else
        echo "❌ Unsupported OS for automatic Redis installation"
        echo "📖 Please install Redis manually: https://redis.io/download"
        echo "⏭️  Skipping Redis setup for now..."
    fi
else
    echo "✅ Redis found"
    # Start Redis if not running
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "🔧 Starting Redis service..."
        brew services start redis 2>/dev/null || true
    else
        echo "🔧 Starting Redis service..."
        sudo systemctl start redis-server 2>/dev/null || true
        sudo systemctl start redis 2>/dev/null || true
    fi
    sleep 5
fi

# Test Redis connection
echo "🧪 Testing Redis connection..."
if command -v redis-cli &> /dev/null; then
    if redis-cli ping 2>/dev/null | grep -q "PONG"; then
        echo "✅ Redis connection test PASSED"
        
        # Set initial InventSight keys
        redis-cli set "inventsight:system:initialized" "2025-08-27T00:06:16Z by WinKyaw" EX 3600 >/dev/null 2>&1
        redis-cli set "inventsight:database:setup" "completed" EX 3600 >/dev/null 2>&1
        echo "🔧 InventSight Redis cache initialized"
        
        # Test cache operations
        TEST_VALUE=$(redis-cli get "inventsight:system:initialized" 2>/dev/null)
        if [[ "$TEST_VALUE" == *"WinKyaw"* ]]; then
            echo "✅ Redis cache operations working correctly"
        fi
    else
        echo "⚠️  Redis connection test failed"
        echo "🔧 Manual test command: redis-cli ping"
    fi
else
    echo "⚠️  redis-cli not available for testing"
fi

echo ""
echo "🎉 InventSight Database Setup Complete!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📅 Setup completed: 2025-08-27 00:06:16"
echo "👤 Setup by: WinKyaw"
echo ""
echo "✅ Database Status Summary:"
echo "   🐘 PostgreSQL:"
echo "      Database: inventsight_db"
echo "      User: inventsight_user"
echo "      Password: inventsight_secure_password_2025"
echo "      Status: $(PGPASSWORD='inventsight_secure_password_2025' psql -h localhost -U inventsight_user -d inventsight_db -c "SELECT 'Connected'" -t 2>/dev/null | xargs || echo 'Check manually')"
echo ""
echo "   🍃 MongoDB:"  
echo "      Database: inventsight_analytics"
echo "      Collections: activity_logs, inventory_analytics"
if [ "$MONGO_RUNNING" = true ]; then
    MONGO_STATUS="Running"
    if command -v mongosh &> /dev/null; then
        RECORD_COUNT=$(mongosh inventsight_analytics --eval "db.activity_logs.countDocuments()" --quiet 2>/dev/null || echo "Unknown")
    else
        RECORD_COUNT=$(mongo inventsight_analytics --eval "db.activity_logs.count()" --quiet 2>/dev/null | tail -n1 || echo "Unknown")
    fi
    echo "      Status: $MONGO_STATUS ($RECORD_COUNT activity records)"
else
    echo "      Status: Check manually"
fi
echo ""
echo "   ⚡ Redis:"
echo "      Host: localhost:6379"
if command -v redis-cli &> /dev/null && redis-cli ping 2>/dev/null | grep -q "PONG"; then
    REDIS_STATUS="Connected"
    CACHE_KEYS=$(redis-cli keys "inventsight:*" 2>/dev/null | wc -l | xargs || echo "0")
    echo "      Status: $REDIS_STATUS ($CACHE_KEYS InventSight cache keys)"
else
    echo "      Status: Check manually"
fi
echo ""
echo "📋 Database Connection Commands:"
echo "   PostgreSQL: PGPASSWORD='inventsight_secure_password_2025' psql -h localhost -U inventsight_user -d inventsight_db"
echo "   MongoDB: mongosh inventsight_analytics"
echo "   Redis: redis-cli"
echo ""
echo "🚀 Next Step: Run application configuration"
echo "   ./2-configure-app.sh"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
EOF

# Make the script executable
chmod +x scripts/1-setup-databases.sh

echo "✅ Database setup script created successfully!"
echo ""
echo "🚀 Now run the database setup:"
echo "cd scripts && ./1-setup-databases.sh"
echo ""
echo "Or go back and choose option 1 again:"
echo "./start-inventsight.sh"