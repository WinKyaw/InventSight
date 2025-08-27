#!/bin/bash
# InventSight Backend - Common Issues and Fixes
# Current Date and Time (UTC): 2025-08-26 09:33:31
# Current User's Login: WinKyaw

echo "🔧 InventSight Backend - Common Issues and Fixes"
echo "📅 Current Date and Time (UTC): 2025-08-26 09:33:31"
echo "👤 Current User's Login: WinKyaw"

# Issue 1: Port already in use
fix_port_issue() {
    echo "🔍 Issue 1: Port 8080 already in use"
    echo "Finding process using port 8080..."
    
    if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null ; then
        echo "📋 Process using port 8080:"
        lsof -Pi :8080 -sTCP:LISTEN
        echo ""
        echo "🔧 Fix options:"
        echo "1. Kill the process: sudo kill -9 \$(lsof -t -i:8080)"
        echo "2. Change InventSight port in application.yml: server.port=8081"
        echo "3. Use different port: mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081"
    else
        echo "✅ Port 8080 is available"
    fi
}

# Issue 2: Database connection failed
fix_database_issue() {
    echo "🔍 Issue 2: Database connection problems"
    
    # PostgreSQL check
    echo "🐘 Checking PostgreSQL..."
    if ! pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
        echo "❌ PostgreSQL not running"
        echo "🔧 Fix commands:"
        echo "# macOS: brew services start postgresql@14"
        echo "# Linux: sudo systemctl start postgresql"
        echo "# Create database: sudo -u postgres createdb inventsight_db"
    else
        echo "✅ PostgreSQL is running"
    fi
    
    # MongoDB check
    echo "🍃 Checking MongoDB..."
    if ! pgrep mongod >/dev/null 2>&1; then
        echo "❌ MongoDB not running"
        echo "🔧 Fix commands:"
        echo "# macOS: brew services start mongodb/brew/mongodb-community"
        echo "# Linux: sudo systemctl start mongod"
    else
        echo "✅ MongoDB is running"
    fi
    
    # Redis check  
    echo "⚡ Checking Redis..."
    if ! redis-cli ping >/dev/null 2>&1; then
        echo "❌ Redis not running"
        echo "🔧 Fix commands:"
        echo "# macOS: brew services start redis"
        echo "# Linux: sudo systemctl start redis-server"
    else
        echo "✅ Redis is running"
    fi
}

# Issue 3: Java version problems
fix_java_issue() {
    echo "🔍 Issue 3: Java version compatibility"
    
    if ! command -v java &> /dev/null; then
        echo "❌ Java not found"
        echo "🔧 Install Java 17:"
        echo "# macOS: brew install openjdk@17"
        echo "# Ubuntu: sudo apt install openjdk-17-jdk"
        echo "# Download: https://adoptium.net/"
        return
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}' | awk -F '.' '{print $1}')
    if [ "$JAVA_VERSION" -lt 17 ]; then
        echo "❌ Java version $JAVA_VERSION found, need 17+"
        echo "🔧 Upgrade to Java 17 or later"
    else
        echo "✅ Java version $JAVA_VERSION is compatible"
    fi
}

# Issue 4: Maven dependency problems
fix_maven_issue() {
    echo "🔍 Issue 4: Maven dependency problems"
    
    if [ ! -f "~/.m2/settings.xml" ]; then
        echo "🔧 Creating Maven settings..."
        mkdir -p ~/.m2
        cat > ~/.m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
</settings>
EOF
    fi
    
    echo "🧹 Cleaning Maven cache and rebuilding..."
    echo "rm -rf ~/.m2/repository"
    echo "mvn clean install -U"
}

# Issue 5: Application startup fails
fix_startup_issue() {
    echo "🔍 Issue 5: Application startup failures"
    
    echo "🔧 Diagnostic commands:"
    echo "1. Check logs: tail -f logs/inventsight.log"
    echo "2. Verbose startup: mvn spring-boot:run -X"
    echo "3. Check configuration: mvn spring-boot:run -Dspring.profiles.active=debug"
    echo "4. Skip tests: mvn spring-boot:run -DskipTests"
    echo ""
    echo "🔍 Common startup issues:"
    echo "- Database connection failed: Check database services"
    echo "- Port already in use: Change port or kill process"
    echo "- Missing dependencies: Run mvn clean install"
    echo "- Configuration errors: Check application.yml"
}

# Run all checks
echo "🔍 Running all diagnostic checks..."
fix_port_issue
echo ""
fix_database_issue  
echo ""
fix_java_issue
echo ""

echo ""
echo "🎯 Quick Fix Commands:"
echo "# Fix all issues at once"
echo "sudo systemctl start postgresql mongod redis-server 2>/dev/null || true"
echo "brew services start postgresql@14 mongodb/brew/mongodb-community redis 2>/dev/null || true"
echo "mvn clean install -U"
echo "mvn spring-boot:run"