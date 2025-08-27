#!/bin/bash
# InventSight Backend - Step 3: Build and Run
# Current Date and Time (UTC): 2025-08-26 23:55:49
# Current User's Login: WinKyaw

echo "🚀 InventSight - Step 3: Build and Run"
echo "📅 Current Date and Time (UTC): 2025-08-26 23:55:49"
echo "👤 Current User's Login: WinKyaw"
echo ""

# Check if we're in the right location
if [ ! -f "../pom.xml" ]; then
    echo "❌ Error: Please run this script from the InventSight/scripts/ directory"
    echo "📁 Current location should be: InventSight/scripts/"
    exit 1
fi

# Navigate to project root
cd ..

# Check Java version
echo "☕ Checking Java version..."
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Installing Java 17..."
    
    # macOS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "🍎 Installing Java 17 on macOS..."
        brew install openjdk@17
        echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
        export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
    
    # Ubuntu/Debian
    elif [[ -f /etc/debian_version ]]; then
        echo "🐧 Installing Java 17 on Ubuntu/Debian..."
        sudo apt update
        sudo apt install -y openjdk-17-jdk
    
    # CentOS/RHEL/Fedora
    elif [[ -f /etc/redhat-release ]]; then
        echo "🔴 Installing Java 17 on CentOS/RHEL/Fedora..."
        sudo dnf install -y java-17-openjdk java-17-openjdk-devel
    else
        echo "❌ Please install Java 17 manually"
        echo "📖 Download from: https://adoptium.net/"
        exit 1
    fi
else
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        echo "❌ Java version $JAVA_VERSION found, need 17+"
        echo "🔧 Please upgrade to Java 17 or later"
        exit 1
    fi
    echo "✅ Java version $JAVA_VERSION found"
fi

# Check Maven
echo "📦 Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Installing Maven..."
    
    # macOS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install maven
    # Ubuntu/Debian
    elif [[ -f /etc/debian_version ]]; then
        sudo apt update && sudo apt install -y maven
    # CentOS/RHEL/Fedora
    elif [[ -f /etc/redhat-release ]]; then
        sudo dnf install -y maven
    else
        echo "❌ Please install Maven manually"
        echo "📖 Visit: https://maven.apache.org/install.html"
        exit 1
    fi
fi

MVN_VERSION=$(mvn -version 2>/dev/null | head -n1 | awk '{print $3}')
echo "✅ Maven version $MVN_VERSION found"

# Check if databases are running
echo "🔍 Checking database connections..."

# PostgreSQL check
if PGPASSWORD='inventsight_secure_password_2025' psql -h localhost -U inventsight_user -d inventsight_db -c "SELECT 1;" &>/dev/null; then
    echo "✅ PostgreSQL is running and accessible"
else
    echo "⚠️  PostgreSQL not accessible. Starting..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew services start postgresql@14
    else
        sudo systemctl start postgresql
    fi
    sleep 5
fi

# MongoDB check  
if command -v mongosh &> /dev/null; then
    if mongosh inventsight_analytics --eval "db.adminCommand('ping')" --quiet &>/dev/null; then
        echo "✅ MongoDB is running and accessible"
    else
        echo "⚠️  MongoDB not accessible. Starting..."
        if [[ "$OSTYPE" == "darwin"* ]]; then
            brew services start mongodb/brew/mongodb-community
        else
            sudo systemctl start mongod
        fi
        sleep 5
    fi
elif command -v mongo &> /dev/null; then
    if mongo inventsight_analytics --eval "db.adminCommand('ping')" --quiet &>/dev/null; then
        echo "✅ MongoDB is running and accessible"
    else
        echo "⚠️  MongoDB not accessible. Starting..."
        if [[ "$OSTYPE" == "darwin"* ]]; then
            brew services start mongodb/brew/mongodb-community
        else
            sudo systemctl start mongod
        fi
        sleep 5
    fi
fi

# Redis check
if redis-cli ping | grep -q "PONG"; then
    echo "✅ Redis is running and accessible"
else
    echo "⚠️  Redis not accessible. Starting..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew services start redis
    else
        sudo systemctl start redis-server
    fi
    sleep 3
fi

echo ""

# Create logs directory
echo "📁 Creating logs directory..."
mkdir -p logs
echo "✅ Logs directory ready: logs/"

# Clean previous builds
echo "🧹 Cleaning previous builds..."
mvn clean

if [ $? -ne 0 ]; then
    echo "❌ Maven clean failed"
    exit 1
fi

echo "✅ Previous builds cleaned"

# Download dependencies and compile
echo "📦 Downloading dependencies and compiling..."
mvn compile

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed. Common fixes:"
    echo "1. Check Java version: java -version"
    echo "2. Check Maven version: mvn -version"
    echo "3. Clear Maven cache: rm -rf ~/.m2/repository"
    echo "4. Check internet connection for dependencies"
    exit 1
fi

echo "✅ InventSight backend compiled successfully"

# Run tests
echo "🧪 Running tests..."
mvn test

if [ $? -ne 0 ]; then
    echo "⚠️  Some tests failed, but continuing with startup..."
else
    echo "✅ All tests passed"
fi

# Package the application
echo "📦 Packaging application..."
mvn package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Packaging failed"
    exit 1
fi

echo "✅ InventSight backend packaged successfully"

# Create enhanced startup script
echo "📝 Creating enhanced startup script..."

cat > start-inventsight.sh << 'EOF'
#!/bin/bash
# InventSight Backend - Enhanced Startup Script
# Current Date and Time (UTC): 2025-08-26 23:55:49
# Current User's Login: WinKyaw

echo "🚀 Starting InventSight - Intelligent Inventory & POS Backend"
echo "📅 Startup time: $(date -u '+%Y-%m-%d %H:%M:%S') UTC"
echo "👤 Started by: WinKyaw"
echo "📦 Repository: WinKyaw/InventSight"
echo ""

# Pre-startup checks
echo "🔍 Pre-startup system checks..."

# Check Java
JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -ge 17 ]; then
    echo "✅ Java $JAVA_VERSION ready"
else
    echo "❌ Java 17+ required, found: $JAVA_VERSION"
    exit 1
fi

# Check databases
echo "🗄️  Checking databases..."

# PostgreSQL
if PGPASSWORD='inventsight_secure_password_2025' psql -h localhost -U inventsight_user -d inventsight_db -c "SELECT 1;" &>/dev/null; then
    echo "✅ PostgreSQL connected"
else
    echo "⚠️  Starting PostgreSQL..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew services start postgresql@14
    else
        sudo systemctl start postgresql
    fi
    sleep 5
fi

# MongoDB
if command -v mongosh &> /dev/null; then
    MONGO_STATUS=$(mongosh inventsight_analytics --eval "db.adminCommand('ping').ok" --quiet 2>/dev/null || echo "0")
else
    MONGO_STATUS=$(mongo inventsight_analytics --eval "db.adminCommand('ping').ok" --quiet 2>/dev/null || echo "0")
fi

if [ "$MONGO_STATUS" = "1" ]; then
    echo "✅ MongoDB connected"
else
    echo "⚠️  Starting MongoDB..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew services start mongodb/brew/mongodb-community
    else
        sudo systemctl start mongod
    fi
    sleep 5
fi

# Redis
if redis-cli ping | grep -q "PONG"; then
    echo "✅ Redis connected"
else
    echo "⚠️  Starting Redis..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew services start redis
    else
        sudo systemctl start redis-server
    fi
    sleep 3
fi

echo ""
echo "🎯 InventSight Backend System Information:"
echo "   🌐 API Base URL: http://localhost:8080/api"
echo "   📖 API Documentation: http://localhost:8080/api/swagger"
echo "   🏥 Health Check: http://localhost:8080/api/health"
echo "   📊 System Metrics: http://localhost:8080/api/actuator"
echo "   📁 Log File: logs/inventsight.log"
echo ""
echo "🔐 Default Admin Login:"
echo "   📧 Email: winkyaw@inventsight.com"
echo "   🔑 Password: password"
echo ""
echo "🚀 Starting InventSight application..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Start the application with enhanced JVM options
export JAVA_OPTS="-Xmx512m -Xms256m -Dspring.profiles.active=dev -Duser.timezone=UTC"

mvn spring-boot:run -Dspring-boot.run.jvmArguments="$JAVA_OPTS"
EOF

chmod +x start-inventsight.sh

echo "✅ Enhanced startup script created: start-inventsight.sh"

# Start the application
echo ""
echo "🚀 Starting InventSight Backend Application..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📅 Application startup: 2025-08-26 23:55:49"
echo "👤 Started by: WinKyaw"
echo ""
echo "🎯 Access Points:"
echo "   🌐 API Base: http://localhost:8080/api"
echo "   📖 Documentation: http://localhost:8080/api/swagger"
echo "   🏥 Health Check: http://localhost:8080/api/health"
echo ""
echo "🔐 Default Login:"
echo "   📧 winkyaw@inventsight.com"
echo "   🔑 password"
echo ""
echo "📊 Starting InventSight now..."

# Set environment variables
export SPRING_PROFILES_ACTIVE=dev
export JAVA_OPTS="-Xmx512m -Xms256m -Duser.timezone=UTC"

# Start the application
mvn spring-boot:run -Dspring-boot.run.jvmArguments="$JAVA_OPTS"