#!/bin/bash
# Always resolve to project root regardless of where you run from
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

WATCH_PATHS=(
  "$PROJECT_ROOT/src/main/resources/data/enums"
  "$PROJECT_ROOT/src/main/resources/data/errors.json"
  "$PROJECT_ROOT/src/main/resources/data/routes.json"
  "$PROJECT_ROOT/src/main/resources/rules/update-rules/animal_update_rules.json"
  "$PROJECT_ROOT/src/main/resources/rules/update-rules/user_update_rules.json"
)

fswatch -o "${WATCH_PATHS[@]}" | while read -r change; do
  echo "🔧 Change detected: $change"

  echo "🔁 Running enum generator..."
  node "$PROJECT_ROOT/scripts/generateEnums.js"

  echo "🔁 Running route generator..."
  node "$PROJECT_ROOT/scripts/generateRoutes.js"

  echo "🔁 Running animal update generator..."
  node "$PROJECT_ROOT/scripts/generateAnimalUpdateTests.js"

  echo "🔁 Running user update generator..."
  node "$PROJECT_ROOT/scripts/generateUserUpdateTests.js"

  echo "🔁 Running error generator..."
  node "$PROJECT_ROOT/scripts/generateErrorCodes.js"

done