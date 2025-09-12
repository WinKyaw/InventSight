#!/bin/bash

# Test script for UUID and Tenant Isolation in InventSight
# Tests the new UUID implementation and fixed tenant isolation
# Author: Generated for WinKyaw/InventSight
# Date: 2025-08-26

set -e

echo "🧪 InventSight UUID and Tenant Isolation Test Suite"
echo "=================================================="
echo "Testing UUID implementation and tenant isolation fixes"
echo "Date: $(date)"
echo ""

# Configuration
BASE_URL="http://localhost:8080/api"
CONTENT_TYPE="Content-Type: application/json"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
print_step() {
    echo -e "${BLUE}📋 Step $1: $2${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

check_server() {
    echo "🔍 Checking if InventSight server is running..."
    if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        print_success "InventSight server is running"
        return 0
    else
        print_error "InventSight server is not running at $BASE_URL"
        echo "Please start the server first:"
        echo "  mvn spring-boot:run"
        echo "  or ./start-inventsight.sh"
        return 1
    fi
}

register_user() {
    local username=$1
    local email=$2
    local firstname=$3
    local lastname=$4
    
    echo "👤 Registering user: $username ($email)"
    
    local response=$(curl -s -X POST "$BASE_URL/auth/register" \
        -H "$CONTENT_TYPE" \
        -d "{
            \"username\": \"$username\",
            \"email\": \"$email\",
            \"password\": \"password123\",
            \"firstName\": \"$firstname\",
            \"lastName\": \"$lastname\"
        }")
    
    echo "$response"
}

login_user() {
    local username=$1
    
    echo "🔐 Logging in user: $username"
    
    local response=$(curl -s -X POST "$BASE_URL/auth/signin" \
        -H "$CONTENT_TYPE" \
        -d "{
            \"username\": \"$username\",
            \"password\": \"password123\"
        }")
    
    echo "$response"
}

extract_token() {
    local response=$1
    echo "$response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4
}

extract_uuid() {
    local response=$1
    echo "$response" | grep -o '"uuid":"[^"]*"' | cut -d'"' -f4
}

create_product() {
    local token=$1
    local tenant_id=$2
    local product_name=$3
    local sku=$4
    
    echo "📦 Creating product: $product_name (Tenant: $tenant_id)"
    
    local response=$(curl -s -X POST "$BASE_URL/products" \
        -H "$CONTENT_TYPE" \
        -H "Authorization: Bearer $token" \
        -H "X-Tenant-ID: $tenant_id" \
        -d "{
            \"name\": \"$product_name\",
            \"sku\": \"$sku\",
            \"price\": 29.99,
            \"quantity\": 100,
            \"category\": \"Test Category\",
            \"supplier\": \"Test Supplier\"
        }")
    
    echo "$response"
}

get_products() {
    local token=$1
    local tenant_id=$2
    
    echo "📋 Getting products for tenant: $tenant_id"
    
    local response=$(curl -s -X GET "$BASE_URL/products" \
        -H "Authorization: Bearer $token" \
        -H "X-Tenant-ID: $tenant_id")
    
    echo "$response"
}

run_migration() {
    local token=$1
    
    echo "🔧 Running UUID migration"
    
    local response=$(curl -s -X POST "$BASE_URL/admin/migration/uuid" \
        -H "Authorization: Bearer $token")
    
    echo "$response"
}

validate_uuids() {
    local token=$1
    
    echo "🔍 Validating UUIDs"
    
    local response=$(curl -s -X GET "$BASE_URL/admin/migration/uuid/validate" \
        -H "Authorization: Bearer $token")
    
    echo "$response"
}

# Main test execution
main() {
    echo "Starting UUID and Tenant Isolation tests..."
    echo ""
    
    # Check if server is running
    if ! check_server; then
        exit 1
    fi
    
    echo ""
    print_step "1" "Register test users with UUID generation"
    
    # Register User A
    echo ""
    echo "Registering User A..."
    USER_A_RESPONSE=$(register_user "uuid_test_user_a" "usera@test.com" "Test" "UserA")
    echo "User A Response: $USER_A_RESPONSE"
    
    USER_A_UUID=$(extract_uuid "$USER_A_RESPONSE")
    if [ -n "$USER_A_UUID" ]; then
        print_success "User A UUID: $USER_A_UUID"
    else
        print_warning "Could not extract UUID for User A (may not be in registration response)"
    fi
    
    # Register User B
    echo ""
    echo "Registering User B..."
    USER_B_RESPONSE=$(register_user "uuid_test_user_b" "userb@test.com" "Test" "UserB")
    echo "User B Response: $USER_B_RESPONSE"
    
    USER_B_UUID=$(extract_uuid "$USER_B_RESPONSE")
    if [ -n "$USER_B_UUID" ]; then
        print_success "User B UUID: $USER_B_UUID"
    else
        print_warning "Could not extract UUID for User B (may not be in registration response)"
    fi
    
    echo ""
    print_step "2" "Login users and get tokens"
    
    # Login User A
    echo ""
    echo "Logging in User A..."
    LOGIN_A_RESPONSE=$(login_user "uuid_test_user_a")
    echo "Login A Response: $LOGIN_A_RESPONSE"
    
    TOKEN_A=$(extract_token "$LOGIN_A_RESPONSE")
    if [ -n "$TOKEN_A" ]; then
        print_success "User A Token: ${TOKEN_A:0:20}..."
    else
        print_error "Failed to get token for User A"
        echo "Login Response: $LOGIN_A_RESPONSE"
        exit 1
    fi
    
    # Login User B
    echo ""
    echo "Logging in User B..."
    LOGIN_B_RESPONSE=$(login_user "uuid_test_user_b")
    echo "Login B Response: $LOGIN_B_RESPONSE"
    
    TOKEN_B=$(extract_token "$LOGIN_B_RESPONSE")
    if [ -n "$TOKEN_B" ]; then
        print_success "User B Token: ${TOKEN_B:0:20}..."
    else
        print_error "Failed to get token for User B"
        echo "Login Response: $LOGIN_B_RESPONSE"
        exit 1
    fi
    
    echo ""
    print_step "3" "Run UUID migration to ensure existing data has UUIDs"
    
    echo ""
    MIGRATION_RESPONSE=$(run_migration "$TOKEN_A")
    echo "Migration Response: $MIGRATION_RESPONSE"
    
    # Validate UUIDs
    echo ""
    VALIDATION_RESPONSE=$(validate_uuids "$TOKEN_A")
    echo "Validation Response: $VALIDATION_RESPONSE"
    
    # Extract UUIDs from user profiles if not available from registration
    if [ -z "$USER_A_UUID" ] || [ -z "$USER_B_UUID" ]; then
        echo ""
        print_step "4" "Get user profiles to extract UUIDs"
        
        # Get User A profile
        echo ""
        echo "Getting User A profile..."
        PROFILE_A_RESPONSE=$(curl -s -X GET "$BASE_URL/users/profile" \
            -H "Authorization: Bearer $TOKEN_A")
        echo "Profile A Response: $PROFILE_A_RESPONSE"
        
        if [ -z "$USER_A_UUID" ]; then
            USER_A_UUID=$(echo "$PROFILE_A_RESPONSE" | grep -o '"uuid":"[^"]*"' | cut -d'"' -f4)
        fi
        
        # Get User B profile
        echo ""
        echo "Getting User B profile..."
        PROFILE_B_RESPONSE=$(curl -s -X GET "$BASE_URL/users/profile" \
            -H "Authorization: Bearer $TOKEN_B")
        echo "Profile B Response: $PROFILE_B_RESPONSE"
        
        if [ -z "$USER_B_UUID" ]; then
            USER_B_UUID=$(echo "$PROFILE_B_RESPONSE" | grep -o '"uuid":"[^"]*"' | cut -d'"' -f4)
        fi
    fi
    
    if [ -n "$USER_A_UUID" ]; then
        print_success "User A UUID confirmed: $USER_A_UUID"
    else
        print_error "Could not determine User A UUID"
        echo "This may indicate UUID generation is not working"
    fi
    
    if [ -n "$USER_B_UUID" ]; then
        print_success "User B UUID confirmed: $USER_B_UUID"
    else
        print_error "Could not determine User B UUID"
        echo "This may indicate UUID generation is not working"
    fi
    
    # If we still don't have UUIDs, use placeholder values for testing
    if [ -z "$USER_A_UUID" ]; then
        USER_A_UUID="default-tenant-a"
        print_warning "Using placeholder UUID for User A: $USER_A_UUID"
    fi
    
    if [ -z "$USER_B_UUID" ]; then
        USER_B_UUID="default-tenant-b"
        print_warning "Using placeholder UUID for User B: $USER_B_UUID"
    fi
    
    echo ""
    print_step "5" "Test product creation with tenant isolation"
    
    # Create product for User A
    echo ""
    echo "Creating product for User A with tenant ID: $USER_A_UUID"
    PRODUCT_A_RESPONSE=$(create_product "$TOKEN_A" "$USER_A_UUID" "User A Product" "SKU-A-001")
    echo "Product A Response: $PRODUCT_A_RESPONSE"
    
    # Create product for User B
    echo ""
    echo "Creating product for User B with tenant ID: $USER_B_UUID"
    PRODUCT_B_RESPONSE=$(create_product "$TOKEN_B" "$USER_B_UUID" "User B Product" "SKU-B-001")
    echo "Product B Response: $PRODUCT_B_RESPONSE"
    
    echo ""
    print_step "6" "Test tenant isolation - verify products are isolated"
    
    # Get products for User A
    echo ""
    echo "Getting products for User A (should only see User A's products)..."
    PRODUCTS_A_RESPONSE=$(get_products "$TOKEN_A" "$USER_A_UUID")
    echo "User A Products: $PRODUCTS_A_RESPONSE"
    
    # Get products for User B
    echo ""
    echo "Getting products for User B (should only see User B's products)..."
    PRODUCTS_B_RESPONSE=$(get_products "$TOKEN_B" "$USER_B_UUID")
    echo "User B Products: $PRODUCTS_B_RESPONSE"
    
    echo ""
    print_step "7" "Test cross-tenant access (should be isolated)"
    
    # Try to get User B's products using User A's token and User B's tenant ID
    echo ""
    echo "Attempting to access User B's products with User A's token (should fail or return empty)..."
    CROSS_ACCESS_RESPONSE=$(get_products "$TOKEN_A" "$USER_B_UUID")
    echo "Cross-access Response: $CROSS_ACCESS_RESPONSE"
    
    echo ""
    print_step "8" "Analyze results"
    
    # Analyze the responses for tenant isolation
    echo ""
    echo "=== TENANT ISOLATION ANALYSIS ==="
    
    # Check if User A's product list contains only their products
    if echo "$PRODUCTS_A_RESPONSE" | grep -q "User A Product"; then
        print_success "User A can see their own products"
    else
        print_warning "User A cannot see their own products (may be due to store configuration)"
    fi
    
    if echo "$PRODUCTS_A_RESPONSE" | grep -q "User B Product"; then
        print_error "TENANT ISOLATION FAILED: User A can see User B's products"
    else
        print_success "User A cannot see User B's products (tenant isolation working)"
    fi
    
    # Check if User B's product list contains only their products
    if echo "$PRODUCTS_B_RESPONSE" | grep -q "User B Product"; then
        print_success "User B can see their own products"
    else
        print_warning "User B cannot see their own products (may be due to store configuration)"
    fi
    
    if echo "$PRODUCTS_B_RESPONSE" | grep -q "User A Product"; then
        print_error "TENANT ISOLATION FAILED: User B can see User A's products"
    else
        print_success "User B cannot see User A's products (tenant isolation working)"
    fi
    
    # Check UUID presence in responses
    if echo "$PRODUCT_A_RESPONSE" | grep -q '"uuid"'; then
        print_success "Product A response includes UUID field"
    else
        print_warning "Product A response does not include UUID field"
    fi
    
    if echo "$PRODUCT_B_RESPONSE" | grep -q '"uuid"'; then
        print_success "Product B response includes UUID field"
    else
        print_warning "Product B response does not include UUID field"
    fi
    
    echo ""
    echo "=== TEST SUMMARY ==="
    print_success "UUID and Tenant Isolation tests completed"
    echo "Review the output above to verify:"
    echo "1. Users have UUIDs generated"
    echo "2. Products have UUIDs generated"
    echo "3. Users can only see their own products"
    echo "4. Cross-tenant access is properly restricted"
    echo ""
    echo "If tenant isolation is not working as expected, check:"
    echo "- User-Store relationships in the database"
    echo "- X-Tenant-ID header handling"
    echo "- ProductService tenant-aware queries"
    echo ""
}

# Run the main function
main "$@"