#!/bin/bash

# Demo Script for New Item Setup Frontend
# This script helps test the frontend by providing sample authentication and API calls

echo "======================================"
echo "InventSight - New Item Setup Demo"
echo "======================================"
echo ""

# Configuration
BASE_URL="${API_URL:-http://localhost:8080/api}"
COMPANY_ID="${COMPANY_ID:-}"
AUTH_TOKEN="${AUTH_TOKEN:-}"

echo "Configuration:"
echo "  Base URL: $BASE_URL"
echo "  Company ID: ${COMPANY_ID:-Not set}"
echo "  Auth Token: ${AUTH_TOKEN:0:20}${AUTH_TOKEN:+...}"
echo ""

# Function to make authenticated API calls
api_call() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    if [ -z "$AUTH_TOKEN" ]; then
        echo "❌ Error: AUTH_TOKEN not set. Please set it first."
        return 1
    fi
    
    if [ -z "$COMPANY_ID" ]; then
        echo "❌ Error: COMPANY_ID not set. Please set it first."
        return 1
    fi
    
    local url="${BASE_URL}${endpoint}"
    
    if [[ "$endpoint" == *"?"* ]]; then
        url="${url}&companyId=${COMPANY_ID}"
    else
        url="${url}?companyId=${COMPANY_ID}"
    fi
    
    echo "🌐 $method $url"
    
    if [ -n "$data" ]; then
        curl -X "$method" \
            -H "Authorization: Bearer $AUTH_TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$url" | jq '.' 2>/dev/null || cat
    else
        curl -X "$method" \
            -H "Authorization: Bearer $AUTH_TOKEN" \
            "$url" | jq '.' 2>/dev/null || cat
    fi
    echo ""
    echo ""
}

# Menu
show_menu() {
    echo "======================================"
    echo "Available Actions:"
    echo "======================================"
    echo "1. List all items"
    echo "2. Create sample item"
    echo "3. Create Myanmar sample items"
    echo "4. Search items by name"
    echo "5. Filter items by category"
    echo "6. Export CSV"
    echo "7. Test frontend URLs"
    echo "8. Set credentials"
    echo "9. Exit"
    echo ""
    read -p "Select an option (1-9): " choice
    
    case $choice in
        1) list_items ;;
        2) create_sample_item ;;
        3) create_myanmar_items ;;
        4) search_items ;;
        5) filter_by_category ;;
        6) export_csv ;;
        7) test_frontend ;;
        8) set_credentials ;;
        9) exit 0 ;;
        *) echo "Invalid option"; sleep 1 ;;
    esac
    
    show_menu
}

# Function implementations
list_items() {
    echo "📋 Listing all items..."
    api_call "GET" "/predefined-items"
}

create_sample_item() {
    echo "➕ Creating sample item..."
    local data='{
        "name": "Apple (Red Delicious)",
        "category": "Fruits",
        "unitType": "lb",
        "description": "Fresh Red Delicious Apples",
        "defaultPrice": 2.99
    }'
    api_call "POST" "/predefined-items" "$data"
}

create_myanmar_items() {
    echo "➕ Creating Myanmar sample items..."
    
    # Item 1
    local data1='{
        "name": "ရှာလကာကြီး",
        "category": "မုန့်",
        "unitType": "တစ်လုံး",
        "description": "Traditional Myanmar cake",
        "defaultPrice": 1500
    }'
    api_call "POST" "/predefined-items" "$data1"
    
    sleep 1
    
    # Item 2
    local data2='{
        "name": "ပုလင်း",
        "category": "အသုံးအဆောင်",
        "unitType": "တစ်ခု",
        "description": "Water bottle",
        "defaultPrice": 500
    }'
    api_call "POST" "/predefined-items" "$data2"
}

search_items() {
    read -p "Enter search term: " search_term
    echo "🔍 Searching for: $search_term"
    api_call "GET" "/predefined-items?search=$search_term"
}

filter_by_category() {
    read -p "Enter category: " category
    echo "🔍 Filtering by category: $category"
    api_call "GET" "/predefined-items?category=$category"
}

export_csv() {
    echo "📥 Exporting CSV..."
    if [ -z "$AUTH_TOKEN" ] || [ -z "$COMPANY_ID" ]; then
        echo "❌ Error: AUTH_TOKEN and COMPANY_ID must be set"
        return 1
    fi
    
    local url="${BASE_URL}/predefined-items/export-csv?companyId=${COMPANY_ID}"
    curl -X GET \
        -H "Authorization: Bearer $AUTH_TOKEN" \
        -o "predefined-items-export.csv" \
        "$url"
    
    if [ -f "predefined-items-export.csv" ]; then
        echo "✅ CSV exported to: predefined-items-export.csv"
        echo "Preview:"
        head -10 predefined-items-export.csv
    else
        echo "❌ Export failed"
    fi
}

test_frontend() {
    echo "🌐 Frontend URLs:"
    echo ""
    echo "Landing Page:"
    echo "  http://localhost:8080/static/index.html"
    echo ""
    echo "Authentication Setup:"
    echo "  http://localhost:8080/static/pages/setup-auth.html"
    echo ""
    echo "New Item Setup:"
    echo "  http://localhost:8080/static/pages/new-item-setup.html"
    echo ""
    
    if command -v xdg-open &> /dev/null; then
        read -p "Open frontend in browser? (y/n): " open_browser
        if [ "$open_browser" = "y" ]; then
            xdg-open "http://localhost:8080/static/index.html"
        fi
    fi
}

set_credentials() {
    echo "🔐 Set Credentials"
    echo ""
    read -p "Enter Company ID (UUID): " new_company_id
    read -p "Enter Auth Token: " new_auth_token
    
    export COMPANY_ID="$new_company_id"
    export AUTH_TOKEN="$new_auth_token"
    
    echo ""
    echo "✅ Credentials set!"
    echo "  Company ID: $COMPANY_ID"
    echo "  Auth Token: ${AUTH_TOKEN:0:20}..."
}

# Check if required tools are available
check_dependencies() {
    if ! command -v curl &> /dev/null; then
        echo "❌ Error: curl is not installed"
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        echo "⚠️  Warning: jq is not installed. Output will not be formatted."
        echo "   Install with: sudo apt-get install jq (Ubuntu) or brew install jq (Mac)"
        sleep 2
    fi
}

# Main
check_dependencies

if [ -z "$COMPANY_ID" ] || [ -z "$AUTH_TOKEN" ]; then
    echo "⚠️  Warning: Credentials not set"
    echo ""
    echo "To use this script, you need to set:"
    echo "  export COMPANY_ID='your-company-uuid'"
    echo "  export AUTH_TOKEN='your-jwt-token'"
    echo ""
    echo "Or you can set them using option 8 in the menu."
    echo ""
    read -p "Press Enter to continue..."
fi

show_menu
