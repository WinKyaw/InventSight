#!/bin/bash

# InventSight Authentication Controller Test Script
# This script demonstrates the usage of all authentication endpoints
# Run the Spring Boot application first before executing this script

BASE_URL="http://localhost:8080"

echo "🔐 InventSight Authentication Controller Test Script"
echo "📅 Current DateTime (UTC): 2025-08-27 10:27:11"
echo "👤 Created by: WinKyaw"
echo ""

# Test 1: User Registration
echo "1. Testing User Registration (POST /auth/register):"
REGISTER_RESPONSE=$(curl -s -X POST ${BASE_URL}/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser123",
    "email": "testuser123@inventsight.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }' || echo "ERROR: Server not running")
echo "Response: $REGISTER_RESPONSE"
echo ""

# Extract token from registration response (if successful)
TOKEN=$(echo $REGISTER_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Test 2: User Login
echo "2. Testing User Login (POST /auth/login):"
LOGIN_RESPONSE=$(curl -s -X POST ${BASE_URL}/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser123@inventsight.com",
    "password": "password123"
  }' || echo "ERROR: Server not running")
echo "Response: $LOGIN_RESPONSE"

# Extract token from login response (if successful and registration failed)
if [ -z "$TOKEN" ]; then
    TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
fi
echo ""

# Test 3: Get Current User Profile
echo "3. Testing Get Current User (GET /auth/me):"
if [ ! -z "$TOKEN" ]; then
    ME_RESPONSE=$(curl -s -X GET ${BASE_URL}/auth/me \
      -H "Authorization: Bearer ${TOKEN}" || echo "ERROR: Server not running")
    echo "Response: $ME_RESPONSE"
else
    echo "Skipping - No token available"
fi
echo ""

# Test 4: Token Refresh
echo "4. Testing Token Refresh (POST /auth/refresh):"
if [ ! -z "$TOKEN" ]; then
    REFRESH_RESPONSE=$(curl -s -X POST ${BASE_URL}/auth/refresh \
      -H "Authorization: Bearer ${TOKEN}" || echo "ERROR: Server not running")
    echo "Response: $REFRESH_RESPONSE"
else
    echo "Skipping - No token available"
fi
echo ""

# Test 5: User Logout
echo "5. Testing User Logout (POST /auth/logout):"
if [ ! -z "$TOKEN" ]; then
    LOGOUT_RESPONSE=$(curl -s -X POST ${BASE_URL}/auth/logout \
      -H "Authorization: Bearer ${TOKEN}" || echo "ERROR: Server not running")
    echo "Response: $LOGOUT_RESPONSE"
else
    echo "Skipping - No token available"
fi
echo ""

# Test 6: Invalid Login
echo "6. Testing Invalid Login (POST /auth/login):"
INVALID_LOGIN_RESPONSE=$(curl -s -X POST ${BASE_URL}/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "invalid@inventsight.com",
    "password": "wrongpassword"
  }' || echo "ERROR: Server not running")
echo "Response: $INVALID_LOGIN_RESPONSE"
echo ""

echo "✅ InventSight Authentication Test Complete"
echo "🔗 All endpoints tested: /auth/login, /auth/register, /auth/refresh, /auth/me, /auth/logout"