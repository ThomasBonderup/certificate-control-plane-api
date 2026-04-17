#!/bin/sh

set -eu

TEMPLATE="/opt/keycloak/data/codex/import/combotto-realm.template.json"
OUTPUT_DIR="/opt/keycloak/data/import"
OUTPUT_FILE="${OUTPUT_DIR}/combotto-realm.json"

mkdir -p "${OUTPUT_DIR}"
cp "${TEMPLATE}" "${OUTPUT_FILE}"

escape_replacement() {
  printf '%s' "$1" | sed -e 's/[\/&]/\\&/g'
}

replace_var() {
  key="$1"
  value="$(printenv "$key")"
  escaped_value="$(escape_replacement "$value")"
  sed -i "s|\${${key}}|${escaped_value}|g" "${OUTPUT_FILE}"
}

replace_var "KEYCLOAK_READ_CLIENT_ID"
replace_var "KEYCLOAK_READ_CLIENT_SECRET"
replace_var "KEYCLOAK_WRITE_CLIENT_ID"
replace_var "KEYCLOAK_WRITE_CLIENT_SECRET"
replace_var "KEYCLOAK_READ_USERNAME"
replace_var "KEYCLOAK_READ_PASSWORD"
replace_var "KEYCLOAK_READ_EMAIL"
replace_var "KEYCLOAK_WRITE_USERNAME"
replace_var "KEYCLOAK_WRITE_PASSWORD"
replace_var "KEYCLOAK_WRITE_EMAIL"
