#!/bin/bash

# Test script to verify registration endpoints are publicly accessible
# This script tests various registration endpoint patterns

echo "🔒 Testing InventSight Registration Endpoint Security Configuration"
echo "=================================================================="

BASE_URL="http://localhost:8080"

echo ""
echo "Testing registration endpoints (expecting connection errors if server not running,"
echo "but NOT 401 Unauthorized errors which would indicate security blocking)"
echo ""

# Test 1: /api/auth/register (main endpoint with context path)
echo "📝 Test 1: POST $BASE_URL/api/auth/register"
curl -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com", 
    "password": "TestPassword123!",
    "firstName": "Test",
    "lastName": "User"
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  --connect-timeout 3 \
  2>&1

echo ""

# Test 2: /api/register (direct register endpoint)
echo "📝 Test 2: POST $BASE_URL/api/register"
curl -X POST "$BASE_URL/api/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPassword123!",
    "firstName": "Test", 
    "lastName": "User"
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  --connect-timeout 3 \
  2>&1

echo ""

# Test 3: /auth/register (without context path)
echo "📝 Test 3: POST $BASE_URL/auth/register"
curl -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPassword123!", 
    "firstName": "Test",
    "lastName": "User"
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  --connect-timeout 3 \
  2>&1

echo ""
echo "✅ Security configuration test complete!"
echo ""
echo "📋 Expected Results:"
echo "- Connection errors (curl: (7) Failed to connect) are normal if server is not running"
echo "- HTTP 404 errors are acceptable (endpoint exists but no handler)"
echo "- HTTP 401 Unauthorized would indicate security blocking (BAD)"
echo "- HTTP 500 errors might indicate server issues but security is allowing the request"
echo ""
echo "To test with running server:"
echo "1. Start server: ./mvnw spring-boot:run"
echo "2. Run this script again"