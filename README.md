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
- Read client id: `${KEYCLOAK_READ_CLIENT_ID}`
- Read client secret: `${KEYCLOAK_READ_CLIENT_SECRET}`
- Read test username: `${KEYCLOAK_READ_USERNAME}`
- Read test password: `${KEYCLOAK_READ_PASSWORD}`
- Write client id: `${KEYCLOAK_WRITE_CLIENT_ID}`
- Write client secret: `${KEYCLOAK_WRITE_CLIENT_SECRET}`
- Write test username: `${KEYCLOAK_WRITE_USERNAME}`
- Write test password: `${KEYCLOAK_WRITE_PASSWORD}`

The realm file is now a template at [combotto-realm.template.json](/Users/thomaswintherbonderup/Development/combotto-control-plane-api/keycloak/import/combotto-realm.template.json). `docker compose` passes the Keycloak values into the container, a small startup script renders the final realm JSON, and then Keycloak imports that rendered file. This is necessary because Keycloak does not interpolate environment variables directly inside realm import JSON.

Get a local read token:

```bash
curl -s \
  -X POST http://localhost:9000/realms/combotto/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d "client_id=$KEYCLOAK_READ_CLIENT_ID" \
  -d "client_secret=$KEYCLOAK_READ_CLIENT_SECRET" \
  -d "username=$KEYCLOAK_READ_USERNAME" \
  -d "password=$KEYCLOAK_READ_PASSWORD"
```

Get a local write token:

```bash
curl -s \
  -X POST http://localhost:9000/realms/combotto/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d "client_id=$KEYCLOAK_WRITE_CLIENT_ID" \
  -d "client_secret=$KEYCLOAK_WRITE_CLIENT_SECRET" \
  -d "username=$KEYCLOAK_WRITE_USERNAME" \
  -d "password=$KEYCLOAK_WRITE_PASSWORD"
```

Use the read token against the API:

```bash
READ_TOKEN="$(curl -s \
  -X POST http://localhost:9000/realms/combotto/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d "client_id=$KEYCLOAK_READ_CLIENT_ID" \
  -d "client_secret=$KEYCLOAK_READ_CLIENT_SECRET" \
  -d "username=$KEYCLOAK_READ_USERNAME" \
  -d "password=$KEYCLOAK_READ_PASSWORD" | jq -r '.access_token')"

curl -H "Authorization: Bearer $READ_TOKEN" \
  http://localhost:8080/api/certificates
```

Use the write token against the API:

```bash
WRITE_TOKEN="$(curl -s \
  -X POST http://localhost:9000/realms/combotto/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d "client_id=$KEYCLOAK_WRITE_CLIENT_ID" \
  -d "client_secret=$KEYCLOAK_WRITE_CLIENT_SECRET" \
  -d "username=$KEYCLOAK_WRITE_USERNAME" \
  -d "password=$KEYCLOAK_WRITE_PASSWORD" | jq -r '.access_token')"

curl -X POST \
  -H "Authorization: Bearer $WRITE_TOKEN" \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/certificates \
  -d '{
    "tenantId": "demo-tenant",
    "name": "Writer Demo Certificate",
    "commonName": "writer.example.com",
    "issuer": "Combotto CA",
    "serialNumber": "writer-demo-001",
    "sha256Fingerprint": "AA:BB:CC:DD",
    "notBefore": "2026-04-01T00:00:00Z",
    "notAfter": "2026-07-01T00:00:00Z",
    "status": "ACTIVE",
    "renewalStatus": "NOT_STATUS",
    "owner": "local-writer",
    "notes": "Write token demo"
  }'
```

## Observability

The service exposes Spring Boot Actuator endpoints on `http://localhost:8080/actuator`.

- `/actuator/health` is public and returns basic health status
- `/actuator/info` is public
- `/actuator/metrics` requires a JWT with `controlplane.read`
- `/actuator/metrics/{name}` requires a JWT with `controlplane.read`
- `/actuator/prometheus` requires a JWT with `controlplane.read`

Example with a read token:

```bash
curl -H "Authorization: Bearer $READ_TOKEN" \
  http://localhost:8080/actuator/metrics

curl -H "Authorization: Bearer $READ_TOKEN" \
  http://localhost:8080/actuator/metrics/jvm.memory.used

curl -H "Authorization: Bearer $READ_TOKEN" \
  http://localhost:8080/actuator/prometheus
```

## Postman Setup

The Postman collection reads login details from environment variables instead of storing them in the collection.

1. Import:
   - [certificate-api.postman_collection.json](/Users/thomaswintherbonderup/Development/combotto-control-plane-api/postman/certificate-api.postman_collection.json)
   - [local-dev.postman_environment.json](/Users/thomaswintherbonderup/Development/combotto-control-plane-api/postman/local-dev.postman_environment.json)
2. Select the `Combotto Local Dev` environment in Postman.
3. Run `Authorization Demo - List Certificates Without Token` to see `401`.
4. Run `Get Read Token`, then run:
   - `List Certificates` and expect `200`
   - `Authorization Demo - Read Token Cannot Create Certificate` and expect `403`
   - `Observability / Metrics` and expect `200`
   - `Observability / Metric By Name - JVM Memory Used` and expect `200`
   - `Observability / Prometheus` and expect `200`
5. Run `Get Write Token`, then run:
   - `Create Certificate` and expect `201`
   - `Authorization Demo - Write Token Cannot List Certificates` and expect `403`
   - `Observability / Metrics` and expect `403` if the write token is still active

The collection also includes:

- `Observability / Health` and `Observability / Info` as public actuator checks
- `Observability / Metrics Without Token` and `Observability / Prometheus Without Token` to verify `401`

Expected environment variable names in Postman:

- `baseUrl`
- `keycloakBaseUrl`
- `realm`
- `readClientId`
- `readClientSecret`
- `readUsername`
- `readPassword`
- `writeClientId`
- `writeClientSecret`
- `writeUsername`
- `writePassword`

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
