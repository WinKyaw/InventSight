#!/bin/bash
# InventSight Backend - Main Startup Script
# Current Date and Time (UTC): 2025-08-26 23:55:49
# Current User's Login: WinKyaw

echo "🏢 InventSight - Intelligent Inventory & POS Backend"
echo "📅 Current Date and Time (UTC): 2025-08-26 23:55:49"
echo "👤 Current User's Login: WinKyaw"
echo "📦 Repository: WinKyaw/InventSight"
echo ""

# Check if we're in the project root
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: Please run this script from the InventSight project root directory"
    echo "📁 Expected files: pom.xml, src/, scripts/"
    exit 1
fi

echo "✅ InventSight project root verified"
echo ""

# Option menu
echo "🎯 InventSight Backend Setup Options:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1. 🗄️  Setup Databases (PostgreSQL + MongoDB + Redis)"
echo "2. ⚙️  Configure Application (Create config files)"
echo "3. 🚀 Build and Run InventSight"
echo "4. 🎯 Complete Setup (Run all steps)"
echo "5. 🔧 Fix Common Issues"
echo "6. ❌ Exit"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Create scripts directory if it doesn't exist
mkdir -p scripts

read -p "👤 Enter your choice (1-6): " choice

case $choice in
    1)
        echo "🗄️  Running database setup..."
        cd scripts && ./1-setup-databases.sh
        ;;
    2)
        echo "⚙️  Running application configuration..."
        cd scripts && ./2-configure-app.sh
        ;;
    3)
        echo "🚀 Running build and start..."
        cd scripts && ./3-build-and-run.sh
        ;;
    4)
        echo "🎯 Running complete setup..."
        echo "Step 1/3: Setting up databases..."
        cd scripts && ./1-setup-databases.sh
        echo ""
        echo "Step 2/3: Configuring application..."
        ./2-configure-app.sh
        echo ""
        echo "Step 3/3: Building and running..."
        ./3-build-and-run.sh
        ;;
    5)
        echo "🔧 Running issue diagnostics..."
        cd scripts && ./fix-common-issues.sh
        ;;
    6)
        echo "👋 Exiting InventSight setup"
        exit 0
        ;;
    *)
        echo "❌ Invalid choice. Please run again and select 1-6"
        exit 1
        ;;
esac