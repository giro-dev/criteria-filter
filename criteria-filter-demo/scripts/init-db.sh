#!/bin/bash
# ============================================================
# Initialize PostgreSQL database for criteria-filter demo
# ============================================================

set -e

DB_NAME="criteriafilter"
DB_USER="demo"
DB_PASSWORD="secret"
DB_HOST="localhost"
DB_PORT="5432"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SQL_FILE="$PROJECT_ROOT/src/main/resources/data.sql"

echo "🚀 Initializing PostgreSQL for criteria-filter demo..."

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo "❌ psql not found. Please install PostgreSQL client."
    exit 1
fi

# Check if database exists
echo "🔍 Checking database connection..."
if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c '\q' 2>/dev/null; then
    echo "✅ Database exists."
else
    echo "⚠️  Database '$DB_NAME' not found. Creating it..."
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;" || true
fi

# Create extension if needed (for JSONB support - already built-in in PostgreSQL)
echo "🔧 Ensuring database is ready..."
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT version();" > /dev/null

# Import data
echo "📥 Importing sample data from $SQL_FILE..."
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SQL_FILE"

echo "✅ Database initialized successfully!"
echo ""
echo "📝 Tables created:"
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "\dt" 2>/dev/null || true
echo ""
echo "🌐 You can now start the demo app with: mvn spring-boot:run"
