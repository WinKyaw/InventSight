#!/bin/bash

# InventSight Registration API Test Script
# This script demonstrates testing all the registration endpoints

BASE_URL="http://localhost:8080/api/auth"

echo "🚀 InventSight Registration API Test Suite"
echo "============================================"

# Test 1: Check email availability
echo ""
echo "📧 Test 1: Check Email Availability"
echo "GET $BASE_URL/check-email?email=test@inventsight.com"

curl -X GET "$BASE_URL/check-email?email=test@inventsight.com" \
  -H "Content-Type: application/json" \
  -w "\nStatus: %{http_code}\n\n"

# Test 2: Validate password strength
echo "🔐 Test 2: Validate Password Strength (Weak Password)"
echo "POST $BASE_URL/validate-password"

curl -X POST "$BASE_URL/validate-password" \
  -H "Content-Type: application/json" \
  -d '{"password": "weak"}' \
  -w "\nStatus: %{http_code}\n\n"

echo "🔐 Test 3: Validate Password Strength (Strong Password)"
echo "POST $BASE_URL/validate-password"

curl -X POST "$BASE_URL/validate-password" \
  -H "Content-Type: application/json" \
  -d '{"password": "SecurePass123!"}' \
  -w "\nStatus: %{http_code}\n\n"

# Test 4: User Registration with weak password (should fail)
echo "👤 Test 4: User Registration (Weak Password - Should Fail)"
echo "POST $BASE_URL/register"

curl -X POST "$BASE_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "email": "test1@inventsight.com",
    "password": "weak",
    "firstName": "Test",
    "lastName": "User"
  }' \
  -w "\nStatus: %{http_code}\n\n"

# Test 5: User Registration with strong password (should succeed)
echo "👤 Test 5: User Registration (Strong Password - Should Succeed)"
echo "POST $BASE_URL/register"

curl -X POST "$BASE_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser2",
    "email": "test2@inventsight.com",
    "password": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User"
  }' \
  -w "\nStatus: %{http_code}\n\n"

# Test 6: Duplicate email registration (should fail)
echo "👤 Test 6: Duplicate Email Registration (Should Fail)"
echo "POST $BASE_URL/register"

curl -X POST "$BASE_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser3",
    "email": "test2@inventsight.com",
    "password": "AnotherSecure123!",
    "firstName": "Another",
    "lastName": "User"
  }' \
  -w "\nStatus: %{http_code}\n\n"

# Test 7: Resend verification email
echo "📧 Test 7: Resend Verification Email"
echo "POST $BASE_URL/resend-verification"

curl -X POST "$BASE_URL/resend-verification" \
  -H "Content-Type: application/json" \
  -d '{"email": "test2@inventsight.com"}' \
  -w "\nStatus: %{http_code}\n\n"

# Test 8: Email verification (with invalid token)
echo "✅ Test 8: Email Verification (Invalid Token)"
echo "POST $BASE_URL/verify-email"

curl -X POST "$BASE_URL/verify-email" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "INVALID_TOKEN_123456789012345678901234",
    "email": "test2@inventsight.com"
  }' \
  -w "\nStatus: %{http_code}\n\n"

echo "✅ Test Suite Complete!"
echo ""
echo "📝 Notes:"
echo "- Tests 1-4 should work without a running server (will show connection errors)"
echo "- To run full tests, start the InventSight server first:"
echo "  mvn spring-boot:run"
echo "- Check server logs for verification tokens to test email verification"
echo "- Rate limiting will trigger after multiple attempts from same IP"