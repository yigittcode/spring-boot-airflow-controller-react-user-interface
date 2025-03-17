#!/bin/bash

# Keycloak connection information
KEYCLOAK_URL="http://localhost:8082"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin123"
REALM_NAME="airflow-realm"
CLIENT_ID="airflow-client"
CLIENT_SECRET="${KEYCLOAK_CLIENT_SECRET}"
USER_USERNAME="airflow-user"
USER_PASSWORD="Password123"
USER_FIRSTNAME="Airflow"
USER_LASTNAME="User"
USER_EMAIL="airflow@example.com"
ROLE_NAME="airflow-user"

echo "Starting Keycloak configuration..."

# Get admin token
echo "Getting admin token..."
ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" == "null" ]; then
  echo "Admin token could not be retrieved. Please ensure Keycloak is running and credentials are correct."
  exit 1
fi

echo "Admin token successfully retrieved."

# Create realm
echo "Creating realm: ${REALM_NAME}..."
curl -s -X POST "${KEYCLOAK_URL}/admin/realms" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"realm\":\"${REALM_NAME}\",\"enabled\":true}"

echo "Realm created."

# Create client
echo "Creating client: ${CLIENT_ID}..."
curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/clients" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"clientId\":\"${CLIENT_ID}\",\"enabled\":true,\"clientAuthenticatorType\":\"client-secret\",\"redirectUris\":[\"http://localhost:5173/*\"],\"webOrigins\":[\"http://localhost:5173\"],\"publicClient\":false,\"protocol\":\"openid-connect\",\"serviceAccountsEnabled\":true}"

echo "Client created."

# Get Client ID
echo "Getting Client ID..."
CLIENT_UUID=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/clients" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" | jq -r ".[] | select(.clientId==\"${CLIENT_ID}\") | .id")

echo "Client ID: ${CLIENT_UUID}"

# Get Client Secret
echo "Getting Client Secret..."
CLIENT_SECRET=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/clients/${CLIENT_UUID}/client-secret" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" | jq -r '.value')

echo "Client Secret: ${CLIENT_SECRET}"

# Create role
echo "Creating role: ${ROLE_NAME}..."
curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/roles" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"${ROLE_NAME}\",\"description\":\"Airflow kullanıcı rolü\"}"

echo "Role created."

# Create user
echo "Creating user: ${USER_USERNAME}..."
curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USER_USERNAME}\",\"enabled\":true,\"emailVerified\":true,\"firstName\":\"${USER_FIRSTNAME}\",\"lastName\":\"${USER_LASTNAME}\",\"email\":\"${USER_EMAIL}\"}"

echo "User created."

# Get User ID
echo "Getting User ID..."
USER_ID=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users?username=${USER_USERNAME}" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" | jq -r '.[0].id')

echo "User ID: ${USER_ID}"

# Set user password
echo "Setting user password..."
curl -s -X PUT "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users/${USER_ID}/reset-password" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"type\":\"password\",\"value\":\"${USER_PASSWORD}\",\"temporary\":false}"

echo "User password set."

# Assign role to user
echo "Assigning role to user..."
ROLE_JSON=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/roles/${ROLE_NAME}" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json")

curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users/${USER_ID}/role-mappings/realm" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "[${ROLE_JSON}]"

echo "Role assigned to user."

# Update application.yml
echo "Updating application.yml file..."

# Change client-secret using sed
sed -i "s/secret: your-client-secret/secret: ${CLIENT_SECRET}/" spring-airflow-controller-main/src/main/resources/application.yml

echo "application.yml update completed."

echo "Keycloak configuration completed!"
echo "Client Secret: ${CLIENT_SECRET}"
echo "---"
echo "User username: ${USER_USERNAME}"
echo "Password: ${USER_PASSWORD}"
echo "---"
echo "Setup completed. You can start Spring Boot and React applications."