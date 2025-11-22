#!/bin/bash

###############################################################################
# Flyway Repair Script for InventSight
# 
# This script helps repair Flyway migration checksum mismatches.
# Use this when migration files have been modified after being applied
# to the database.
#
# Usage:
#   ./scripts/repair-flyway.sh [environment]
#
# Arguments:
#   environment - Optional: dev, prod, or custom profile (default: dev)
#
# Examples:
#   ./scripts/repair-flyway.sh          # Repair using dev profile
#   ./scripts/repair-flyway.sh prod     # Repair using prod profile
#   ./scripts/repair-flyway.sh postgres # Repair using postgres profile
#
# Current Date and Time (UTC): 2025-11-22 09:59:12
# Current User: WinKyaw
# Repository: WinKyaw/InventSight
###############################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default environment
ENVIRONMENT="${1:-dev}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Flyway Repair Script for InventSight${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Display warning for production
if [ "$ENVIRONMENT" = "prod" ] || [ "$ENVIRONMENT" = "production" ]; then
    echo -e "${RED}⚠️  WARNING: Production Environment Detected${NC}"
    echo -e "${RED}This will modify the flyway_schema_history table in production!${NC}"
    echo ""
    echo -e "${YELLOW}Before proceeding, ensure:${NC}"
    echo -e "${YELLOW}1. You have reviewed all migration file changes${NC}"
    echo -e "${YELLOW}2. You have a database backup${NC}"
    echo -e "${YELLOW}3. You understand the implications of repairing checksums${NC}"
    echo ""
    read -p "Are you absolutely sure you want to proceed? (type 'yes' to continue): " -r
    echo
    if [[ ! $REPLY = "yes" ]]; then
        echo -e "${RED}Repair cancelled.${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}Environment: $ENVIRONMENT${NC}"
echo ""

# Check if Maven is available
if ! command -v mvn &> /dev/null && [ ! -f "./mvnw" ]; then
    echo -e "${RED}Error: Maven not found. Please install Maven or use ./mvnw${NC}"
    exit 1
fi

# Use Maven wrapper if available, otherwise use system Maven
MVN_CMD="./mvnw"
if [ ! -f "./mvnw" ]; then
    MVN_CMD="mvn"
fi

echo -e "${BLUE}Step 1: Checking Flyway plugin configuration...${NC}"
if ! grep -q "flyway-maven-plugin" pom.xml 2>/dev/null; then
    echo -e "${YELLOW}Note: Flyway Maven plugin not explicitly configured in pom.xml${NC}"
    echo -e "${YELLOW}Using Spring Boot's built-in Flyway support${NC}"
fi
echo ""

echo -e "${BLUE}Step 2: Running Flyway repair...${NC}"
echo -e "${YELLOW}This will:${NC}"
echo -e "${YELLOW}- Update checksums in flyway_schema_history to match current files${NC}"
echo -e "${YELLOW}- Remove failed migration entries${NC}"
echo -e "${YELLOW}- Realign migration history with current files${NC}"
echo ""

# Run Flyway repair using Spring Boot with the specified profile
echo -e "${GREEN}Executing: $MVN_CMD spring-boot:run -Dspring-boot.run.arguments=--flyway.repair=true -Dspring-boot.run.profiles=$ENVIRONMENT${NC}"
echo ""

# Actually run the repair
if $MVN_CMD spring-boot:run \
    -Dspring-boot.run.arguments="--spring.flyway.repair=true" \
    -Dspring-boot.run.profiles="$ENVIRONMENT" \
    -DskipTests; then
    
    echo ""
    echo -e "${GREEN}✅ Flyway repair completed successfully!${NC}"
    echo ""
else
    echo ""
    echo -e "${RED}❌ Flyway repair failed. Check the output above for errors.${NC}"
    echo ""
    echo -e "${YELLOW}Common issues:${NC}"
    echo -e "${YELLOW}1. Database connection failure - check credentials and connectivity${NC}"
    echo -e "${YELLOW}2. Migration files are missing or corrupted${NC}"
    echo -e "${YELLOW}3. Database schema is in an inconsistent state${NC}"
    echo ""
    echo -e "${YELLOW}For manual repair, you can:${NC}"
    echo -e "${YELLOW}1. Connect to the database directly${NC}"
    echo -e "${YELLOW}2. Check the flyway_schema_history table${NC}"
    echo -e "${YELLOW}3. Update checksums manually if needed${NC}"
    echo ""
    exit 1
fi

echo -e "${BLUE}Step 3: Verification${NC}"
echo -e "${GREEN}To verify the repair:${NC}"
echo -e "${GREEN}1. Check application logs for Flyway validation success${NC}"
echo -e "${GREEN}2. Query the database:${NC}"
echo -e "   ${BLUE}SELECT version, description, checksum, success, installed_on${NC}"
echo -e "   ${BLUE}FROM flyway_schema_history${NC}"
echo -e "   ${BLUE}ORDER BY installed_rank;${NC}"
echo -e "${GREEN}3. Ensure all migrations show success = true${NC}"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Repair process completed!${NC}"
echo -e "${BLUE}========================================${NC}"

# Additional information
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "1. Start your application: ${BLUE}$MVN_CMD spring-boot:run -Dspring-boot.run.profiles=$ENVIRONMENT${NC}"
echo -e "2. Monitor logs for any Flyway-related errors"
echo -e "3. Test your application functionality"
echo ""

if [ "$ENVIRONMENT" = "prod" ] || [ "$ENVIRONMENT" = "production" ]; then
    echo -e "${RED}⚠️  Production Reminder:${NC}"
    echo -e "${YELLOW}Document this repair in your change log${NC}"
    echo -e "${YELLOW}Notify your team about the checksum update${NC}"
    echo -e "${YELLOW}Consider updating your deployment pipeline to prevent future mismatches${NC}"
    echo ""
fi

exit 0
