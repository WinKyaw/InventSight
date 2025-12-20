#!/bin/bash

# Test script for Warehouse Creation Validation Error Fix
# This script tests the warehouse creation endpoint with various scenarios
#
# USAGE:
#   1. Start the backend server: ./start-inventsight.sh
#   2. Obtain a JWT token by logging in via /api/auth/login
#   3. Set the TOKEN environment variable: export TOKEN="your-jwt-token"
#   4. Run this script: ./test-warehouse-validation.sh
#
# ALTERNATIVE (without authentication):
#   You can modify SecurityConfig to temporarily allow public access to /warehouses
#   for testing purposes during development.

echo "🧪 Testing Warehouse Creation Validation"
echo "=========================================="
echo ""

# Get token from environment or use placeholder
TOKEN="${TOKEN:-YOUR_TOKEN_HERE}"

if [ "$TOKEN" = "YOUR_TOKEN_HERE" ]; then
    echo "⚠️  WARNING: No JWT token provided. Set TOKEN environment variable."
    echo "   Example: export TOKEN=\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\""
    echo "   The tests will run but may fail with 401 Unauthorized."
    echo ""
fi

# Base URL
BASE_URL="http://localhost:8080/api"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "📋 Test 1: Missing Location Field (Should Return Validation Error)"
echo "-------------------------------------------------------------------"

# Test with missing location
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/warehouses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Test Warehouse",
    "description": "Test Description"
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "HTTP Status: $HTTP_CODE"
echo "Response Body:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" -eq 400 ]; then
    if echo "$BODY" | jq -e '.errors.location' &>/dev/null; then
        echo -e "${GREEN}✅ PASS: Validation error returned for missing location${NC}"
    else
        echo -e "${RED}❌ FAIL: Response doesn't contain location error${NC}"
    fi
else
    echo -e "${RED}❌ FAIL: Expected 400, got $HTTP_CODE${NC}"
fi

echo ""
echo "📋 Test 2: Missing Name Field (Should Return Validation Error)"
echo "--------------------------------------------------------------"

# Test with missing name
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/warehouses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "location": "123 Main St",
    "description": "Test Description"
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "HTTP Status: $HTTP_CODE"
echo "Response Body:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" -eq 400 ]; then
    if echo "$BODY" | jq -e '.errors.name' &>/dev/null; then
        echo -e "${GREEN}✅ PASS: Validation error returned for missing name${NC}"
    else
        echo -e "${RED}❌ FAIL: Response doesn't contain name error${NC}"
    fi
else
    echo -e "${RED}❌ FAIL: Expected 400, got $HTTP_CODE${NC}"
fi

echo ""
echo "📋 Test 3: Valid Request (Should Create Warehouse)"
echo "---------------------------------------------------"

# Test with valid data
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/warehouses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Main Warehouse",
    "location": "123 Main Street",
    "description": "Primary storage facility"
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "HTTP Status: $HTTP_CODE"
echo "Response Body:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" -eq 201 ]; then
    if echo "$BODY" | jq -e '.success == true' &>/dev/null; then
        echo -e "${GREEN}✅ PASS: Warehouse created successfully${NC}"
    else
        echo -e "${YELLOW}⚠️  WARNING: Success but response format unexpected${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  NOTE: Got $HTTP_CODE (may need authentication)${NC}"
fi

echo ""
echo "=========================================="
echo "🏁 Test Summary"
echo "=========================================="
echo ""
echo "Expected Response Format for Validation Errors:"
echo '{'
echo '  "success": false,'
echo '  "message": "Validation failed",'
echo '  "errors": {'
echo '    "location": "Location is required"'
echo '  }'
echo '}'
echo ""
echo "Expected Response Format for Success:"
echo '{'
echo '  "success": true,'
echo '  "message": "Warehouse created successfully",'
echo '  "data": { ... warehouse object ... }'
echo '}'
echo ""
echo "Expected Backend Logs:"
echo "----------------------"
echo "For validation error:"
echo "  ➕ InventSight - Creating warehouse"
echo "     User: <username>"
echo "     Name: Test Warehouse"
echo "     Location: null"
echo "  ❌ Validation errors:"
echo "     - location: Location is required"
echo ""
echo "For success:"
echo "  ➕ InventSight - Creating warehouse"
echo "     User: <username>"
echo "     Name: Main Warehouse"
echo "     Location: 123 Main Street"
echo "  🏢 WarehouseService: Creating warehouse"
echo "     Name: Main Warehouse"
echo "     Location: 123 Main Street"
echo "  ✅ Warehouse saved with ID: <uuid>"
echo "  ✅ Warehouse created: <uuid>"
echo ""
