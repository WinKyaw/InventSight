#!/bin/bash

# Test Store Activation API Endpoint
# This script tests the new store functionality

echo "🧪 Testing Store Activation API Endpoints"
echo "========================================="

# Base URL - adjust if needed
BASE_URL="http://localhost:8080"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_step() {
    echo -e "\n${YELLOW}Step $1: $2${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_step 1 "Starting the application (if not already running)"
echo "Make sure InventSight backend is running on port 8080"
echo "You can start it with: ./run-inventsight-backend.sh"
echo "Waiting for server to be ready..."

# Check if server is running
if ! curl -s $BASE_URL/health > /dev/null 2>&1; then
    echo "Server not responding, trying to check basic endpoint..."
fi

print_step 2 "Register a test user"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "storetest",
    "email": "storetest@example.com",
    "password": "Test123!",
    "firstName": "Store",
    "lastName": "Tester"
  }')

echo "Registration response: $REGISTER_RESPONSE"

if echo "$REGISTER_RESPONSE" | grep -q "success.*true\|User registered successfully"; then
    print_success "User registered successfully"
else
    echo "Registration might have failed or user already exists"
fi

print_step 3 "Login to get JWT token"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "storetest",
    "password": "Test123!"
  }')

echo "Login response: $LOGIN_RESPONSE"

# Extract token (assuming it's in accessToken field)
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -n "$TOKEN" ]; then
    print_success "Login successful, got token: ${TOKEN:0:20}..."
else
    print_error "Failed to extract token from login response"
    echo "Full response: $LOGIN_RESPONSE"
    exit 1
fi

print_step 4 "Test GET /stores - List user's stores"
STORES_RESPONSE=$(curl -s -X GET "$BASE_URL/stores" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "User stores response: $STORES_RESPONSE"

if echo "$STORES_RESPONSE" | grep -q "success.*true"; then
    print_success "Successfully retrieved user stores"
    STORE_COUNT=$(echo "$STORES_RESPONSE" | grep -o '"count":[0-9]*' | cut -d':' -f2)
    echo "Store count: $STORE_COUNT"
else
    print_error "Failed to retrieve user stores"
fi

print_step 5 "Test POST /stores - Create a new store"
CREATE_STORE_RESPONSE=$(curl -s -X POST "$BASE_URL/stores" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "storeName": "Test Store API",
    "description": "Store created via API test",
    "address": "123 API Test Street",
    "city": "Test City",
    "state": "Test State",
    "country": "Test Country",
    "phone": "555-API-TEST",
    "email": "apitest@store.com"
  }')

echo "Create store response: $CREATE_STORE_RESPONSE"

if echo "$CREATE_STORE_RESPONSE" | grep -q "success.*true\|Store created successfully"; then
    print_success "Store created successfully via API"
    # Extract store ID for activation test
    STORE_ID=$(echo "$CREATE_STORE_RESPONSE" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
    if [ -n "$STORE_ID" ]; then
        echo "New store ID: $STORE_ID"
    fi
else
    print_error "Failed to create store via API"
fi

print_step 6 "Test store activation"
if [ -n "$STORE_ID" ]; then
    ACTIVATE_RESPONSE=$(curl -s -X POST "$BASE_URL/stores/$STORE_ID/activate" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json")
    
    echo "Activation response: $ACTIVATE_RESPONSE"
    
    if echo "$ACTIVATE_RESPONSE" | grep -q "success.*true\|Store activated successfully"; then
        print_success "Store activated successfully"
    else
        print_error "Failed to activate store"
    fi
else
    echo "Skipping activation test - no store ID available"
fi

print_step 7 "Test GET /stores/current - Get current active store"
CURRENT_STORE_RESPONSE=$(curl -s -X GET "$BASE_URL/stores/current" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "Current store response: $CURRENT_STORE_RESPONSE"

if echo "$CURRENT_STORE_RESPONSE" | grep -q "success.*true"; then
    print_success "Successfully retrieved current store"
else
    echo "Current store might not be set or endpoint failed"
fi

print_step 8 "Test tenant context by creating a product"
echo "This verifies that the store activation fixes the tenant context issue"

PRODUCT_RESPONSE=$(curl -s -X POST "$BASE_URL/products" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: $(echo "$LOGIN_RESPONSE" | grep -o '"uuid":"[^"]*' | cut -d'"' -f4)" \
  -d '{
    "name": "Test Product API",
    "description": "Product created to test tenant context",
    "price": 19.99,
    "costPrice": 12.50,
    "quantity": 10,
    "category": "Test Category",
    "sku": "API-TEST-001"
  }')

echo "Product creation response: $PRODUCT_RESPONSE"

if echo "$PRODUCT_RESPONSE" | grep -q "success.*true\|Product.*created"; then
    print_success "Product created successfully - tenant context working!"
else
    print_error "Product creation failed - might be tenant context issue"
fi

echo -e "\n${YELLOW}=========================================${NC}"
echo -e "${YELLOW}Store Activation API Test Complete${NC}"
echo -e "${YELLOW}=========================================${NC}"

print_success "All store API endpoints tested"
echo "Key endpoints:"
echo "- GET /stores (list user stores)"
echo "- POST /stores (create new store)"
echo "- POST /stores/{id}/activate (activate store for tenant context)"
echo "- GET /stores/current (get current active store)"