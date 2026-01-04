#!/bin/bash

set -e

PROJECT_ROOT=$(cd "$(dirname "$0")/.." && pwd)
CHANGELOG_DIR="$PROJECT_ROOT/src/main/resources/db/changelog/generated"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
CHANGELOG_FILE="db.changelog-$TIMESTAMP.xml"

echo "📁 Ensuring changelog directory exists..."
mkdir -p "$CHANGELOG_DIR"

echo "📝 Generating Liquibase changelog: $CHANGELOG_FILE"

cd "$PROJECT_ROOT"

./mvnw liquibase:generateChangeLog \
  -Dliquibase.outputChangeLogFile="$CHANGELOG_DIR/$CHANGELOG_FILE" \
  -Dliquibase.propertyFile="src/main/resources/liquibase.properties"

echo "✅ Changelog created at:"
echo "   $CHANGELOG_DIR/$CHANGELOG_FILE"
