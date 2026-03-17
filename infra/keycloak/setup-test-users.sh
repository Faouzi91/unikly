#!/usr/bin/env bash
# =============================================================================
# Unikly — Test User Setup Script
# =============================================================================
# Creates 3 test users in Keycloak via the Admin REST API.
#
# Prerequisites:
#   - Keycloak running at http://localhost:8180
#   - Realm "unikly" imported (via --import-realm)
#   - Admin credentials: admin / admin
#
# Usage:
#   chmod +x setup-test-users.sh
#   ./setup-test-users.sh
#
# Users created:
#   client1@test.com      / password123  → ROLE_CLIENT
#   freelancer1@test.com  / password123  → ROLE_FREELANCER
#   admin@test.com        / password123  → ROLE_ADMIN
# =============================================================================

set -euo pipefail

KEYCLOAK_URL="http://localhost:8180"
REALM="unikly"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"

# ── Get admin access token ───────────────────────────────────────────────────

echo "Obtaining admin token..."
ADMIN_TOKEN=$(curl -sf -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

if [ -z "${ADMIN_TOKEN}" ]; then
  echo "ERROR: Failed to obtain admin token. Is Keycloak running?"
  exit 1
fi
echo "Admin token obtained."

# ── Helper functions ─────────────────────────────────────────────────────────

create_user() {
  local email="$1"
  local first_name="$2"
  local last_name="$3"
  local password="$4"
  local role="$5"

  echo ""
  echo "Creating user: ${email} with role ${role}..."

  # 1. Create the user
  curl -sf -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/users" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
      \"username\": \"${email}\",
      \"email\": \"${email}\",
      \"firstName\": \"${first_name}\",
      \"lastName\": \"${last_name}\",
      \"enabled\": true,
      \"emailVerified\": true
    }"

  # 2. Get the user ID
  local user_id
  user_id=$(curl -sf "${KEYCLOAK_URL}/admin/realms/${REALM}/users?email=${email}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['id'])")

  if [ -z "${user_id}" ]; then
    echo "ERROR: Failed to get user ID for ${email}"
    return 1
  fi
  echo "  User ID: ${user_id}"

  # 3. Set the password
  curl -sf -X PUT "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${user_id}/reset-password" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
      \"type\": \"password\",
      \"value\": \"${password}\",
      \"temporary\": false
    }"
  echo "  Password set."

  # 4. Get the role representation
  local role_json
  role_json=$(curl -sf "${KEYCLOAK_URL}/admin/realms/${REALM}/roles/${role}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")

  # 5. Assign the role
  curl -sf -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${user_id}/role-mappings/realm" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "[${role_json}]"
  echo "  Role ${role} assigned."

  echo "  ✔ User ${email} created successfully."
}

# ── Create test users ────────────────────────────────────────────────────────

create_user "client1@test.com" "Test" "Client" "password123" "ROLE_CLIENT"
create_user "freelancer1@test.com" "Test" "Freelancer" "password123" "ROLE_FREELANCER"
create_user "admin@test.com" "Test" "Admin" "password123" "ROLE_ADMIN"

echo ""
echo "============================================="
echo "  All test users created successfully!"
echo "============================================="
