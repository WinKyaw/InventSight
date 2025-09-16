#!/bin/bash

DB_NAME="inventsight_db"
DB_USER="leonwin"
# If you need a password, export PGPASSWORD or use .pgpass for authentication.

echo "Starting batch execution of all migration and schema SQL scripts..."

psql -U "$DB_USER" -d "$DB_NAME" <<'EOF'
\i migration-multi-tenancy.sql
\i migration-uuid-native-type.sql
\i migration-uuid-primary-keys.sql
\i migration-uuid-support.sql
\i schema.sql
\i uuid-fix-demonstration.sql
EOF

echo "✅ All SQL scripts executed successfully!"