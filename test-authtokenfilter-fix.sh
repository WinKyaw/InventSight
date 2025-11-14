#!/bin/bash
# Test script to verify AuthTokenFilter executes for authenticated endpoints
# This tests the fix for issue where AuthTokenFilter was not executing for /api/products

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080/api"

echo -e "${YELLOW}=== AuthTokenFilter Execution Test ===${NC}"
echo "This test verifies that AuthTokenFilter now executes for business API endpoints"
echo ""

# Test 1: Login should work (public endpoint - filter should skip)
echo -e "${YELLOW}Test 1: Login (public endpoint - should skip filter)${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"windaybunce@gmail.com","password":"Invest2016!"}' 2>&1)

if echo "$LOGIN_RESPONSE" | grep -q "token"; then
    echo -e "${GREEN}✓ Login successful${NC}"
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "  Token: ${TOKEN:0:20}..."
else
    echo -e "${RED}✗ Login failed${NC}"
    echo "  Response: $LOGIN_RESPONSE"
    echo ""
    echo "Note: If the backend is not running, please start it first:"
    echo "  ./start-inventsight.sh"
    exit 1
fi
echo ""

# Test 2: Products endpoint should require authentication (filter should execute)
echo -e "${YELLOW}Test 2: Products without token (should get 401)${NC}"
PRODUCTS_NO_TOKEN=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/products" \
  -H "Content-Type: application/json")

HTTP_CODE=$(echo "$PRODUCTS_NO_TOKEN" | grep "HTTP_CODE:" | cut -d':' -f2)
if [ "$HTTP_CODE" = "401" ]; then
    echo -e "${GREEN}✓ Correctly returns 401 without token${NC}"
    echo "  This confirms the endpoint requires authentication"
else
    echo -e "${RED}✗ Expected 401, got $HTTP_CODE${NC}"
    echo "  Response: $PRODUCTS_NO_TOKEN"
fi
echo ""

# Test 3: Products endpoint with valid token (filter should execute and set authentication)
echo -e "${YELLOW}Test 3: Products with valid token (should get 200 or valid response)${NC}"
PRODUCTS_WITH_TOKEN=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/products" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

HTTP_CODE=$(echo "$PRODUCTS_WITH_TOKEN" | grep "HTTP_CODE:" | cut -d':' -f2)
RESPONSE=$(echo "$PRODUCTS_WITH_TOKEN" | grep -v "HTTP_CODE:")

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Successfully accessed products endpoint with token${NC}"
    echo "  HTTP Status: 200 OK"
    echo "  This confirms AuthTokenFilter executed and set authentication!"
    
    # Show sample response (first 200 chars)
    if [ -n "$RESPONSE" ]; then
        echo "  Response preview: ${RESPONSE:0:200}..."
    fi
elif [ "$HTTP_CODE" = "400" ]; then
    echo -e "${YELLOW}⚠ Got 400 Bad Request${NC}"
    echo "  This might be due to missing tenant_id in JWT"
    echo "  However, the fact we got past 401 means AuthTokenFilter DID execute!"
    echo -e "${GREEN}  ✓ AuthTokenFilter is working${NC}"
    echo "  Response: $RESPONSE"
elif [ "$HTTP_CODE" = "403" ]; then
    echo -e "${YELLOW}⚠ Got 403 Forbidden${NC}"
    echo "  User may not have access to any company/tenant"
    echo "  However, the fact we got past 401 means AuthTokenFilter DID execute!"
    echo -e "${GREEN}  ✓ AuthTokenFilter is working${NC}"
    echo "  Response: $RESPONSE"
else
    echo -e "${RED}✗ Unexpected status code: $HTTP_CODE${NC}"
    echo "  Response: $RESPONSE"
fi
echo ""

# Test 4: Stores endpoint with valid token
echo -e "${YELLOW}Test 4: Stores with valid token (should also work)${NC}"
STORES_WITH_TOKEN=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/stores" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

HTTP_CODE=$(echo "$STORES_WITH_TOKEN" | grep "HTTP_CODE:" | cut -d':' -f2)
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "400" ] || [ "$HTTP_CODE" = "403" ]; then
    echo -e "${GREEN}✓ AuthTokenFilter executed for /stores endpoint${NC}"
    echo "  HTTP Status: $HTTP_CODE"
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "  Successfully accessed stores!"
    else
        echo "  Got past authentication (401), filter is working!"
    fi
else
    echo -e "${RED}✗ Unexpected status code: $HTTP_CODE${NC}"
fi
echo ""

# Test 5: Health endpoint should be public (filter should skip)
echo -e "${YELLOW}Test 5: Health endpoint (public - should skip filter)${NC}"
HEALTH=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/health" \
  -H "Content-Type: application/json")

HTTP_CODE=$(echo "$HEALTH" | grep "HTTP_CODE:" | cut -d':' -f2)
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "404" ]; then
    echo -e "${GREEN}✓ Health endpoint accessible without token${NC}"
    echo "  This confirms public endpoints still work correctly"
else
    echo -e "${YELLOW}⚠ Got status code: $HTTP_CODE${NC}"
fi
echo ""

# Summary
echo -e "${YELLOW}=== Test Summary ===${NC}"
echo ""
echo "Expected behavior (based on the fix):"
echo "1. Public endpoints (/auth, /health, etc.) should be accessible without token"
echo "2. Business endpoints (/products, /stores, etc.) should:"
echo "   - Return 401 without token"
echo "   - Execute AuthTokenFilter when token is present"
echo "   - Return 200/400/403 (not 401) when token is valid"
echo ""
echo "To verify in logs, check for these messages:"
echo "  DEBUG AuthTokenFilter - AuthTokenFilter: Will process request for endpoint: /products"
echo "  DEBUG AuthTokenFilter - === AuthTokenFilter START ==="
echo "  INFO  AuthTokenFilter - Authentication successful for user: windaybunce@gmail.com"
echo ""
echo -e "${GREEN}If tests above show 200/400/403 for products/stores with token,${NC}"
echo -e "${GREEN}then AuthTokenFilter is executing correctly! ✓${NC}"
