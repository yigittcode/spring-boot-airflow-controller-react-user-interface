#!/bin/bash

# Keycloak connection details
KEYCLOAK_URL="http://localhost:8082"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin123"
REALM_NAME="airflow-realm"

# Users and roles to be created
ADMIN_ROLE_NAME="airflow-admin"
ADMIN_ROLE_DESC="Airflow admin role"
ADMIN_USERNAME="admin-user"
ADMIN_PASSWORD="Admin123"
ADMIN_FIRSTNAME="Admin"
ADMIN_LASTNAME="User"
ADMIN_EMAIL="admin@example.com"

NORMAL_USERNAME="normal-user"
NORMAL_PASSWORD="Normal123"
NORMAL_FIRSTNAME="Normal"
NORMAL_LASTNAME="User"
NORMAL_EMAIL="normal@example.com"

echo "Starting Keycloak new user creation process..."

# Get admin token - Using direct command execution
echo "Getting admin token..."
TOKEN_RESPONSE=$(curl -s -X POST "http://localhost:8082/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "grant_type=password")

# Extract token
ADMIN_TOKEN=$(echo $TOKEN_RESPONSE | grep -o '"access_token":"[^"]*"' | sed 's/"access_token":"//g' | sed 's/"//g')

if [ -z "$ADMIN_TOKEN" ]; then
  echo "Admin token could not be retrieved. Please ensure Keycloak is running and credentials are correct."
  echo "Error response: $TOKEN_RESPONSE"
  exit 1
fi

echo "Admin token successfully retrieved."

# Create admin role
echo "Creating admin role: ${ADMIN_ROLE_NAME}..."
ROLE_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/roles" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"${ADMIN_ROLE_NAME}\",\"description\":\"${ADMIN_ROLE_DESC}\"}")

if [[ $ROLE_RESPONSE == *"Conflict detected"* ]]; then
  echo "Admin role already exists."
else
  echo "Admin role created."
fi

# Create admin user
echo "Creating admin user: ${ADMIN_USERNAME}..."
USER_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${ADMIN_USERNAME}\",\"enabled\":true,\"emailVerified\":true,\"firstName\":\"${ADMIN_FIRSTNAME}\",\"lastName\":\"${ADMIN_LASTNAME}\",\"email\":\"${ADMIN_EMAIL}\"}")

if [[ $USER_RESPONSE == *"User exists with same username"* ]]; then
  echo "Admin user already exists."
else
  echo "Admin user created."
fi

sleep 2  # Short wait for the operation to complete

# Get admin user ID
echo "Getting admin user ID..."
USERS_RESPONSE=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users?username=${ADMIN_USERNAME}" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json")

ADMIN_USER_ID=$(echo $USERS_RESPONSE | grep -o '"id":"[^"]*"' | head -1 | sed 's/"id":"//g' | sed 's/"//g')

if [ -z "$ADMIN_USER_ID" ]; then
  echo "Admin user ID could not be retrieved."
  exit 1
fi

echo "Admin user ID: ${ADMIN_USER_ID}"

# Set admin user password
echo "Setting admin user password..."
curl -s -X PUT "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users/${ADMIN_USER_ID}/reset-password" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"type\":\"password\",\"value\":\"${ADMIN_PASSWORD}\",\"temporary\":false}"

echo "Admin user password set."

# Assign admin role to admin user
echo "Assigning admin role to admin user..."
ADMIN_ROLE_RESPONSE=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/roles/${ADMIN_ROLE_NAME}" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json")

curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users/${ADMIN_USER_ID}/role-mappings/realm" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "[${ADMIN_ROLE_RESPONSE}]"

echo "Admin role assigned to admin user."

# Also assign airflow-user role
echo "Also assigning airflow-user role to admin user..."
USER_ROLE_RESPONSE=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/roles/airflow-user" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json")

curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users/${ADMIN_USER_ID}/role-mappings/realm" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "[${USER_ROLE_RESPONSE}]"

echo "Airflow-user role assigned to admin user."

# Create normal user
echo "Creating normal user: ${NORMAL_USERNAME}..."
NORMAL_USER_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${NORMAL_USERNAME}\",\"enabled\":true,\"emailVerified\":true,\"firstName\":\"${NORMAL_FIRSTNAME}\",\"lastName\":\"${NORMAL_LASTNAME}\",\"email\":\"${NORMAL_EMAIL}\"}")

if [[ $NORMAL_USER_RESPONSE == *"User exists with same username"* ]]; then
  echo "Normal user already exists."
else
  echo "Normal user created."
fi

sleep 2  # Short wait for the operation to complete

# Get normal user ID
echo "Getting normal user ID..."
NORMAL_USERS_RESPONSE=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users?username=${NORMAL_USERNAME}" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json")

NORMAL_USER_ID=$(echo $NORMAL_USERS_RESPONSE | grep -o '"id":"[^"]*"' | head -1 | sed 's/"id":"//g' | sed 's/"//g')

if [ -z "$NORMAL_USER_ID" ]; then
  echo "Normal user ID could not be retrieved."
  exit 1
fi

echo "Normal user ID: ${NORMAL_USER_ID}"

# Set normal user password
echo "Setting normal user password..."
curl -s -X PUT "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users/${NORMAL_USER_ID}/reset-password" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"type\":\"password\",\"value\":\"${NORMAL_PASSWORD}\",\"temporary\":false}"

echo "Normal user password set."

# Assign only airflow-user role to normal user
echo "Assigning airflow-user role to normal user..."
curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users/${NORMAL_USER_ID}/role-mappings/realm" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "[${USER_ROLE_RESPONSE}]"

echo "Airflow-user role assigned to normal user."

echo "New users created successfully!"
echo "---"
echo "Admin User: ${ADMIN_USERNAME}"
echo "Admin Password: ${ADMIN_PASSWORD}"
echo "---"
echo "Normal User: ${NORMAL_USERNAME}"
echo "Normal Password: ${NORMAL_PASSWORD}"
echo "---"