# Combotto Control Plane API

## Purpose

The control plane managed operational state around the Combotto audit platform.

## First Module

Certificate inventory.

## Owns

- certificate metadata
- asset bindings
- ownership
- renewal status
- expiry visibility
- query APIs

## Does not own

- certificate technical validation
- TLS handshake evaluation
- finding generation
- report logic

## Success for v1

V1 is done when I can:

- register a certificate
- bind it to an asset
- search/filter certificates
- see which ones expire soon
- update renewal status
- persist everything reliably
- cover core flows with tests

## Technology Stack

- Java 21
- Spring Boot
- Postgres
- Flyway
- Spring Validation
- Testcontainers
- Docker Compose

## Local Docker defaults

- Docker Compose publishes ports on `127.0.0.1` by default so the stack stays local-only.
- `kafka-ui` is behind the `tools` profile and is not started unless requested.
- Compose credentials and host port bindings can be overridden through `.env`.

```bash
cp .env.example .env
docker compose up -d postgres kafka keycloak
docker compose --profile tools up -d kafka-ui
```

## Local Keycloak

Keycloak is configured for local development on `http://localhost:9000` with a realm import at startup.

- Realm: `combotto`
- Admin username: `${KEYCLOAK_ADMIN_USERNAME}`
- Admin password: `${KEYCLOAK_ADMIN_PASSWORD}`
- Token client id: `control-plane-api-cli`
- Token client secret: `${KEYCLOAK_DEV_CLIENT_SECRET}`
- Test username: `local-dev`
- Test password: `${KEYCLOAK_DEV_USER_PASSWORD}`

Set those values in your local `.env` file by copying `.env.example`.

Get a local access token:

```bash
curl -s \
  -X POST http://localhost:9000/realms/combotto/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=control-plane-api-cli' \
  -d "client_secret=$KEYCLOAK_DEV_CLIENT_SECRET" \
  -d 'username=local-dev' \
  -d "password=$KEYCLOAK_DEV_USER_PASSWORD"
```

Use the token against the API:

```bash
TOKEN="$(curl -s \
  -X POST http://localhost:9000/realms/combotto/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=control-plane-api-cli' \
  -d "client_secret=$KEYCLOAK_DEV_CLIENT_SECRET" \
  -d 'username=local-dev' \
  -d "password=$KEYCLOAK_DEV_USER_PASSWORD" | jq -r '.access_token')"

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/certificates
```

## Postman Setup

The Postman collection reads login details from environment variables instead of storing them in the collection.

1. Import:
   - [certificate-api.postman_collection.json](/Users/thomaswintherbonderup/Development/combotto-control-plane-api/postman/certificate-api.postman_collection.json)
   - [local-dev.postman_environment.json](/Users/thomaswintherbonderup/Development/combotto-control-plane-api/postman/local-dev.postman_environment.json)
2. Select the `Combotto Local Dev` environment in Postman.
3. Set these environment values:
   - `clientSecret`
   - `password`
4. Run `Get Access Token` in the collection.
5. Run the API requests normally.

Expected environment variable names in Postman:

- `baseUrl`
- `keycloakBaseUrl`
- `realm`
- `clientId`
- `clientSecret`
- `username`
- `password`

## Features

### Slice 1

Certificate CRUD

### Slide 2

Asset CRUD

### Slide 3

Certificate-to-asset binding

### Slide 4

Expiring soon filter + summary endpoint

### Slide 5

Renewal status + owner assignment

### Slide 6

JWT auth
