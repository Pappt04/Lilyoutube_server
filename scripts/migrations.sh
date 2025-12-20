#!/bin/bash

# --- Usage ---
# ./migrations.sh [path_to_env_file]
# Example: ./migrations.sh .env
# Or with default: ./migrations.sh

# --- Load environment file if provided ---
ENV_FILE="${1:-.env}"

if [ -f "$ENV_FILE" ]; then
    echo "📄 Loading configuration from: $ENV_FILE"
    set -a  # automatically export all variables
    source "$ENV_FILE"
    set +a
else
    echo "⚠️  No environment file found at: $ENV_FILE"
    echo "   Using environment variables or defaults..."
fi

# --- Configuration ---
# Read from environment variables with defaults
DB_URL="${DATABASE_URL:-jdbc:postgresql://localhost:5432/lilyoutube-v1}"
DB_USER="${DATABASE_USERNAME:-postgres}"
DB_PASS="${DATABASE_PASSWORD:-root}"
ENTITY_PACKAGE="${ENTITY_PACKAGE:-com.group17.lilyoutube_server.model}"
CHANGELOG_DIR="src/main/resources/changelog"
MASTER_FILE="$CHANGELOG_DIR/db.changelog-master.xml"
TIMESTAMP=$(date +%Y%m%d%H%M%S)
NEW_FILE="db.changelog-$TIMESTAMP.sql"

# Validate required environment variables
if [ -z "$DB_PASS" ]; then
    echo "❌ Error: DB_PASS is not set!"
    echo ""
    echo "Usage options:"
    echo "  1. Create a .env file with your credentials:"
    echo "     cp .env.example .env"
    echo "     # Edit .env with your password"
    echo "     ./migrations.sh"
    echo ""
    echo "  2. Use a custom env file:"
    echo "     ./migrations.sh /path/to/my.env"
    echo ""
    echo "  3. Set environment variable directly:"
    echo "     export DB_PASS='your_password'"
    echo "     ./migrations.sh"
    echo ""
    exit 1
fi

echo "🚀 Starting migration process..."
echo "   Database: $DB_URL"
echo "   User: $DB_USER"

# 1. Compile the project to ensure entities are up to date
echo "📦 Compiling project..."
mvn compile -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi

# 2. Create changelog directory if it doesn't exist
mkdir -p "$CHANGELOG_DIR"

# 3. Create master file if it doesn't exist
if [ ! -f "$MASTER_FILE" ]; then
    echo "📝 Creating master changelog file..."
    cat > "$MASTER_FILE" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd">

    <!-- Migration files will be added here automatically -->

</databaseChangeLog>
EOF
fi

# 4. Generate the diff (Current Code vs Live DB)
echo "🔍 Comparing Entities to Database..."
mvn liquibase:diff \
    -Dliquibase.url="$DB_URL" \
    -Dliquibase.username="$DB_USER" \
    -Dliquibase.password="$DB_PASS" \
    -Dliquibase.referenceUrl="hibernate:spring:$ENTITY_PACKAGE?dialect=org.hibernate.dialect.PostgreSQLDialect" \
    -Dliquibase.changeLogFile="$MASTER_FILE" \
    -Dliquibase.diffChangeLogFile="$CHANGELOG_DIR/$NEW_FILE"

if [ $? -ne 0 ]; then
    echo "❌ Diff generation failed!"
    exit 1
fi

# 5. Check if file was created and is not empty
if [ -f "$CHANGELOG_DIR/$NEW_FILE" ] && [ -s "$CHANGELOG_DIR/$NEW_FILE" ]; then
    echo "✅ Migration file created: $NEW_FILE"

    # 6. Add the new changelog to the master file
    echo "📝 Adding $NEW_FILE to master changelog..."

    # Use sed to insert before the closing tag (use relative path without 'changelog/')
    sed -i "s|</databaseChangeLog>|    <include file=\"$NEW_FILE\" relativeToChangelogFile=\"true\"/>\n\n</databaseChangeLog>|" "$MASTER_FILE"

    echo "✅ Master changelog updated"

    # 7. Apply the migration to the database
    echo "💾 Running migration to PostgreSQL..."
    mvn liquibase:update \
        -Dliquibase.url="$DB_URL" \
        -Dliquibase.username="$DB_USER" \
        -Dliquibase.password="$DB_PASS" \
        -Dliquibase.changeLogFile="$MASTER_FILE"

    if [ $? -ne 0 ]; then
        echo "❌ Migration update failed!"
        exit 1
    fi
else
    echo "⚠️  No changes detected or migration file is empty."
    echo "    This means your entities match the database schema."
    # Remove empty file if it exists
    [ -f "$CHANGELOG_DIR/$NEW_FILE" ] && rm "$CHANGELOG_DIR/$NEW_FILE"
fi

echo "🎉 Database is now in sync!"