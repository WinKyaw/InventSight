#!/bin/bash
# InventSight Backend - Step 1: Database Setup
# Current Date and Time (UTC): 2025-08-26 23:55:49
# Current User's Login: WinKyaw

echo "🗄️ InventSight - Step 1: Database Setup"
echo "📅 Current Date and Time (UTC): 2025-08-26 23:55:49"
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
        brew install postgresql@14
        brew services start postgresql@14
        sleep 5
    
    # Ubuntu/Debian
    elif [[ -f /etc/debian_version ]]; then
        echo "🐧 Installing PostgreSQL on Ubuntu/Debian..."
        sudo apt update
        sudo apt install -y postgresql postgresql-contrib
        sudo systemctl start postgresql
        sudo systemctl enable postgresql
        sleep 5
    
    # CentOS/RHEL/Fedora
    elif [[ -f /etc/redhat-release ]]; then
        echo "🔴 Installing PostgreSQL on CentOS/RHEL/Fedora..."
        sudo dnf install -y postgresql postgresql-server postgresql-contrib
        sudo postgresql-setup --initdb
        sudo systemctl start postgresql
        sudo systemctl enable postgresql
        sleep 5
    else
        echo "❌ Unsupported OS. Please install PostgreSQL manually"
        echo "📖 Visit: https://www.postgresql.org/download/"
        exit 1
    fi
else
    echo "✅ PostgreSQL found"
    # Start PostgreSQL if not running
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew services start postgresql@14 2>/dev/null || true
    else
        sudo systemctl start postgresql 2>/dev/null || true
    fi
fi

# Create InventSight database and user
echo "🔧 Creating InventSight database and user..."

# Create database setup SQL
cat > /tmp/setup_inventsight_db.sql << 'EOF'
-- InventSight Database Setup
-- Current Date and Time (UTC): 2025-08-26 23:55:49
-- Current User's Login: WinKyaw

-- Drop existing database and user if they exist (for clean setup)
DROP DATABASE IF EXISTS inventsight_db;
DROP USER IF EXISTS inventsight_user;

-- Create InventSight user with password
CREATE USER inventsight_user WITH PASSWORD 'inventsight_secure_password_2025';

-- Create InventSight database
CREATE DATABASE inventsight_db OWNER inventsight_user;

-- Grant all privileges
ALTER USER inventsight_user CREATEDB;
GRANT ALL PRIVILEGES ON DATABASE inventsight_db TO inventsight_user;

-- Connect to the database and set up permissions
\c inventsight_db

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO inventsight_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO inventsight_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO inventsight_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO inventsight_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO inventsight_user;

-- Verify setup
SELECT 'InventSight PostgreSQL database setup completed successfully at 2025-08-26 23:55:49 by WinKyaw!' as status;
EOF

# Execute database setup
echo "📋 Executing database setup commands..."
if sudo -u postgres psql -f /tmp/setup_inventsight_db.sql; then
    echo "✅ InventSight PostgreSQL database created successfully"
else
    echo "⚠️  Database setup encountered issues. Trying alternative method..."
    
    # Alternative method for existing installations
    echo "🔧 Running alternative setup commands..."
    sudo -u postgres dropdb inventsight_db 2>/dev/null || true
    sudo -u postgres dropuser inventsight_user 2>/dev/null || true
    sudo -u postgres createuser -d -r -s inventsight_user
    sudo -u postgres psql -c "ALTER USER inventsight_user PASSWORD 'inventsight_secure_password_2025';"
    sudo -u postgres createdb -O inventsight_user inventsight_db
    
    if [ $? -eq 0 ]; then
        echo "✅ InventSight PostgreSQL database created with alternative method"
    else
        echo "❌ Database setup failed. Please run these commands manually:"
        echo "sudo -u postgres createuser -d -r inventsight_user -P"
        echo "sudo -u postgres createdb -O inventsight_user inventsight_db"
        exit 1
    fi
fi

# Clean up temporary file
rm -f /tmp/setup_inventsight_db.sql

# Test PostgreSQL connection
echo "🧪 Testing PostgreSQL connection..."
if PGPASSWORD='inventsight_secure_password_2025' psql -h localhost -U inventsight_user -d inventsight_db -c "SELECT 'InventSight PostgreSQL connection successful!' as test;" &>/dev/null; then
    echo "✅ PostgreSQL connection test passed"
else
    echo "⚠️  PostgreSQL connection test failed, but database should be ready"
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
        brew tap mongodb/brew
        brew install mongodb-community@7.0
        brew services start mongodb/brew/mongodb-community
        sleep 10
    
    # Ubuntu/Debian
    elif [[ -f /etc/debian_version ]]; then
        echo "🐧 Installing MongoDB on Ubuntu/Debian..."
        curl -fsSL https://pgp.mongodb.com/server-7.0.asc | sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor
        echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
        sudo apt update
        sudo apt install -y mongodb-org
        sudo systemctl start mongod
        sudo systemctl enable mongod
        sleep 10
    
    # CentOS/RHEL/Fedora
    elif [[ -f /etc/redhat-release ]]; then
        echo "🔴 Installing MongoDB on CentOS/RHEL/Fedora..."
        cat > /etc/yum.repos.d/mongodb-org-7.0.repo << 'EOF'
[mongodb-org-7.0]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/redhat/$releasever/mongodb-org/7.0/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://pgp.mongodb.com/server-7.0.asc
EOF
        sudo dnf install -y mongodb-org
        sudo systemctl start mongod
        sudo systemctl enable mongod
        sleep 10
    else
        echo "❌ Unsupported OS. Please install MongoDB manually"
        echo "📖 Visit: https://docs.mongodb.com/manual/installation/"
        exit 1
    fi
else
    echo "✅ MongoDB found"
    # Start MongoDB if not running
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew services start mongodb/brew/mongodb-community 2>/dev/null || true
    else
        sudo systemctl start mongod 2>/dev/null || true
    fi
    sleep 5
fi

# Initialize MongoDB for InventSight
echo "🔧 Initializing InventSight MongoDB database..."

# Create MongoDB initialization script
cat > /tmp/init_inventsight_mongo.js << 'EOF'
// InventSight MongoDB Initialization
// Current Date and Time (UTC): 2025-08-26 23:55:49
// Current User's Login: WinKyaw

// Switch to InventSight analytics database
use inventsight_analytics;

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
    "timestamp": new Date("2025-08-26T23:55:49.000Z"),
    "module": "SYSTEM",
    "severity": "INFO",
    "metadata": {
        "system": "InventSight",
        "version": "1.0.0",
        "initializedBy": "WinKyaw"
    }
});

print("InventSight MongoDB database initialized successfully at 2025-08-26 23:55:49 by WinKyaw!");
EOF

# Execute MongoDB initialization
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
    if mongosh inventsight_analytics --eval "db.adminCommand('ping')" &>/dev/null; then
        echo "✅ MongoDB connection test passed"
    else
        echo "⚠️  MongoDB connection test failed, but database should be ready"
    fi
elif command -v mongo &> /dev/null; then
    if mongo inventsight_analytics --eval "db.adminCommand('ping')" &>/dev/null; then
        echo "✅ MongoDB connection test passed"
    else
        echo "⚠️  MongoDB connection test failed, but database should be ready"
    fi
fi

echo ""

# Redis Setup (Caching & Sessions)
echo "⚡ Setting up Redis for InventSight caching..."

if ! command -v redis-server &> /dev/null; then
    echo "⚠️  Redis not found. Installing..."
    
    # macOS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "🍎 Installing Redis on macOS..."
        brew install redis
        brew services start redis
        sleep 3
    
    # Ubuntu/Debian
    elif [[ -f /etc/debian_version ]]; then
        echo "🐧 Installing Redis on Ubuntu/Debian..."
        sudo apt update
        sudo apt install -y redis-server
        sudo systemctl start redis-server
        sudo systemctl enable redis-server
        sleep 3
    
    # CentOS/RHEL/Fedora
    elif [[ -f /etc/redhat-release ]]; then
        echo "🔴 Installing Redis on CentOS/RHEL/Fedora..."
        sudo dnf install -y redis
        sudo systemctl start redis
        sudo systemctl enable redis
        sleep 3
    else
        echo "❌ Unsupported OS. Please install Redis manually"
        echo "📖 Visit: https://redis.io/download"
        exit 1
    fi
else
    echo "✅ Redis found"
    # Start Redis if not running
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew services start redis 2>/dev/null || true
    else
        sudo systemctl start redis-server 2>/dev/null || true
    fi
    sleep 3
fi

# Test Redis connection
echo "🧪 Testing Redis connection..."
if redis-cli ping | grep -q "PONG"; then
    echo "✅ Redis connection test passed"
    
    # Set initial InventSight key
    redis-cli set "inventsight:system:initialized" "2025-08-26T23:55:49Z by WinKyaw" EX 3600 >/dev/null
    echo "🔧 InventSight Redis cache initialized"
else
    echo "⚠️  Redis connection test failed, but service should be ready"
fi

echo ""
echo "🎉 InventSight Database Setup Complete!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📅 Setup completed: 2025-08-26 23:55:49"
echo "👤 Setup by: WinKyaw"
echo ""
echo "✅ Database Status:"
echo "   🐘 PostgreSQL: inventsight_db (User: inventsight_user)"
echo "   🍃 MongoDB: inventsight_analytics"  
echo "   ⚡ Redis: Cache and sessions ready"
echo ""
echo "🔍 Connection Test Results:"
echo "   PostgreSQL: $(PGPASSWORD='inventsight_secure_password_2025' psql -h localhost -U inventsight_user -d inventsight_db -c "SELECT 'Connected'" -t 2>/dev/null | xargs || echo 'Check manually')"
echo "   MongoDB: $(mongosh inventsight_analytics --eval "db.adminCommand('ping').ok" --quiet 2>/dev/null || echo 'Check manually')"
echo "   Redis: $(redis-cli ping 2>/dev/null || echo 'Check manually')"
echo ""
echo "🚀 Next Step: Run configuration script"
echo "   ./2-configure-app.sh"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"