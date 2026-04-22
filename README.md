# Certificate Control Plane API

## Overview

Certificate Control Plane API is a Spring Boot service for embedded device certificate lifecycle automation. It manages certificate inventory, renewal workflow state, tenant-aware access, and related operational metadata through a REST API.

The current scope centers on certificate lifecycle workflows:

- certificate metadata
- certificate-to-asset bindings
- ownership and renewal workflow fields
- expiring-soon and attention-needed query APIs

## Does not own

- certificate technical validation
- TLS handshake evaluation
- finding generation
- report logic

## Technology Stack

- Java 21
- Spring Boot
- Postgres
- Flyway
- Spring Validation
- Testcontainers
- Docker Compose

## Quick Start

1. Copy the local template files and replace every `REPLACE_ME_...` placeholder with local-only values.
2. Start the supporting services with Docker Compose.
3. Start the API with Gradle.
4. Fetch a local Keycloak token and call the API or Swagger UI.

```bash
cp .env.example .env
docker compose up -d postgres kafka keycloak
./gradlew bootRun
```

Optional tooling:

```bash
docker compose --profile tools up -d kafka-ui
```

## Local Development Notes

- Docker Compose publishes ports on `127.0.0.1` by default so the stack stays local-only.
- `kafka-ui` is behind the `tools` profile and is not started unless requested.
- The committed `.env.example` and Postman environment template are intentionally sanitized. They are templates, not working credentials.
- Compose credentials and host port bindings can be overridden through `.env`.

## Local Keycloak

Keycloak is configured for local development on `http://localhost:9000` with a realm import at startup.

- Realm: `combotto`
- Admin username: `${KEYCLOAK_ADMIN_USERNAME}`
- Admin password: `${KEYCLOAK_ADMIN_PASSWORD}`
- Demo read client id: `${KEYCLOAK_DEMO_READ_CLIENT_ID}`
- Demo read client secret: `${KEYCLOAK_DEMO_READ_CLIENT_SECRET}`
- Demo read username: `${KEYCLOAK_DEMO_READ_USERNAME}`
- Demo read password: `${KEYCLOAK_DEMO_READ_PASSWORD}`
- Demo write client id: `${KEYCLOAK_DEMO_WRITE_CLIENT_ID}`
- Demo write client secret: `${KEYCLOAK_DEMO_WRITE_CLIENT_SECRET}`
- Demo write username: `${KEYCLOAK_DEMO_WRITE_USERNAME}`
- Demo write password: `${KEYCLOAK_DEMO_WRITE_PASSWORD}`
- Demo write-no-admin username: `${KEYCLOAK_DEMO_WRITE_NO_ADMIN_USERNAME}`
- Demo write-no-admin password: `${KEYCLOAK_DEMO_WRITE_NO_ADMIN_PASSWORD}`
- Demo tenant id: `${KEYCLOAK_DEMO_TENANT_ID}`
- Other read username: `${KEYCLOAK_OTHER_READ_USERNAME}`
- Other read password: `${KEYCLOAK_OTHER_READ_PASSWORD}`
- Other write username: `${KEYCLOAK_OTHER_WRITE_USERNAME}`
- Other write password: `${KEYCLOAK_OTHER_WRITE_PASSWORD}`
- Other tenant id: `${KEYCLOAK_OTHER_TENANT_ID}`

The realm file is now a template at [combotto-realm.template.json](/Users/thomaswintherbonderup/Development/combotto-control-plane-api/keycloak/import/combotto-realm.template.json). `docker compose` passes the Keycloak values into the container, a small startup script renders the final realm JSON, and then Keycloak imports that rendered file. This is necessary because Keycloak does not interpolate environment variables directly inside realm import JSON.

Each local Keycloak user has a `tenant_id` user attribute. Keycloak maps that attribute into the JWT as the `tenantId` claim, and the API uses that claim as the tenant boundary for certificates, assets, and bindings.

The local realm seed assigns the `ADMIN` realm role to `${KEYCLOAK_DEMO_WRITE_USERNAME}` and `${KEYCLOAK_OTHER_WRITE_USERNAME}`. It also creates `${KEYCLOAK_DEMO_WRITE_NO_ADMIN_USERNAME}` as a demo-tenant principal that still gets write scope from the write client but has no `ADMIN` role, so you can manually verify delete enforcement.

Local-dev auth note: the imported Keycloak realm is intentionally optimized for local testing, not as a production template. In particular, the seeded users, direct password grant flow, and `fullScopeAllowed: true` on the local write client are convenience choices so admin roles are emitted in local JWTs and method-level authorization can be exercised easily. Treat those settings as local-only and prefer tighter role scope mappings for non-local environments.

Get a local demo-tenant read token:

```bash
curl -s \
  -X POST http://localhost:9000/realms/combotto/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d "client_id=$KEYCLOAK_DEMO_READ_CLIENT_ID" \
  -d "client_secret=$KEYCLOAK_DEMO_READ_CLIENT_SECRET" \
  -d "username=$KEYCLOAK_DEMO_READ_USERNAME" \
  -d "password=$KEYCLOAK_DEMO_READ_PASSWORD"
```

Get a local demo-tenant write token:

```bash
curl -s \
  -X POST http://localhost:9000/realms/combotto/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d "client_id=$KEYCLOAK_DEMO_WRITE_CLIENT_ID" \
  -d "client_secret=$KEYCLOAK_DEMO_WRITE_CLIENT_SECRET" \
  -d "username=$KEYCLOAK_DEMO_WRITE_USERNAME" \
  -d "password=$KEYCLOAK_DEMO_WRITE_PASSWORD"
```

Get a local other-tenant read token:

```bash
curl -s \
  -X POST http://localhost:9000/realms/combotto/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d "client_id=$KEYCLOAK_DEMO_READ_CLIENT_ID" \
  -d "client_secret=$KEYCLOAK_DEMO_READ_CLIENT_SECRET" \
  -d "username=$KEYCLOAK_OTHER_READ_USERNAME" \
  -d "password=$KEYCLOAK_OTHER_READ_PASSWORD"
```

Use a demo-tenant read token against the API:

```bash
READ_TOKEN="$(curl -s \
  -X POST http://localhost:9000/realms/combotto/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d "client_id=$KEYCLOAK_DEMO_READ_CLIENT_ID" \
  -d "client_secret=$KEYCLOAK_DEMO_READ_CLIENT_SECRET" \
  -d "username=$KEYCLOAK_DEMO_READ_USERNAME" \
  -d "password=$KEYCLOAK_DEMO_READ_PASSWORD" | jq -r '.access_token')"

curl -H "Authorization: Bearer $READ_TOKEN" \
  http://localhost:8080/api/certificates
```

Inspect the tenant claim in the token:

```bash
echo "$READ_TOKEN" | cut -d '.' -f2 | base64 -d 2>/dev/null | jq '.tenantId, .scope, .preferred_username'
```

Use a demo-tenant write token against the API:

```bash
WRITE_TOKEN="$(curl -s \
  -X POST http://localhost:9000/realms/combotto/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d "client_id=$KEYCLOAK_DEMO_WRITE_CLIENT_ID" \
  -d "client_secret=$KEYCLOAK_DEMO_WRITE_CLIENT_SECRET" \
  -d "username=$KEYCLOAK_DEMO_WRITE_USERNAME" \
  -d "password=$KEYCLOAK_DEMO_WRITE_PASSWORD" | jq -r '.access_token')"

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

Tenant enforcement rules:

- The API requires the JWT `tenantId` claim on protected endpoints.
- Create requests still include `tenantId` in the body for now, but it must match the authenticated token tenant.
- Cross-tenant resource access returns `404`.
- A mismatched request `tenantId` returns `400`.

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

## OpenAPI

The API publishes an OpenAPI document and Swagger UI in local development:

- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

Swagger UI uses the same bearer token flow as the API. Click `Authorize`, paste a bearer token without the `Bearer ` prefix, and then call protected endpoints.

Recommended local flow:

1. Start the stack with `docker compose up -d postgres kafka keycloak`
2. Start the API
3. Fetch a read or write token from Keycloak
4. Open Swagger UI and authorize with that token

Pagination and sorting endpoints follow Spring Data conventions:

- `page`: zero-based page number
- `size`: page size
- `sort`: `field,direction`

Examples:

```text
sort=createdAt,desc
sort=notAfter,asc
```

Swagger previously rendered an invalid placeholder sort value for some pageable endpoints. The API now ignores malformed sort values and falls back to the endpoint default sort, but the intended values are still the explicit `field,direction` forms above.

Common certificate endpoint examples:

```bash
curl -H "Authorization: Bearer $READ_TOKEN" \
  "http://localhost:8080/api/certificates?tenantId=demo-tenant&sort=createdAt,desc"

curl -H "Authorization: Bearer $READ_TOKEN" \
  "http://localhost:8080/api/certificates/expiring-soon?days=30&sort=notAfter,asc"

curl -H "Authorization: Bearer $READ_TOKEN" \
  "http://localhost:8080/api/certificates/attention-needed?days=30&sort=notAfter,asc"
```

Auth expectations:

- `/v3/api-docs`, `/swagger-ui.html`, and `/swagger-ui/**` are public in local development
- `GET /api/**` requires `controlplane.read`
- `POST`, `PATCH`, and `DELETE` on `/api/**` require `controlplane.write`
- `DELETE /api/certificates/{id}` and `DELETE /api/assets/{id}` also require the `ADMIN` role at the service layer
- Create requests that include `tenantId` must still match the authenticated token tenant

## Postman Setup

The Postman collection is committed without working credentials. Import the sanitized environment template, duplicate it locally if you want, and fill in your own local-only values before requesting tokens.

1. Import:
   - [certificate-api.postman_collection.json](/Users/thomaswintherbonderup/Development/combotto-control-plane-api/postman/certificate-api.postman_collection.json)
   - [local-dev.postman_environment.template.json](/Users/thomaswintherbonderup/Development/combotto-control-plane-api/postman/local-dev.postman_environment.template.json)
2. Select the `Combotto Local Dev` environment in Postman.
3. Run `Authorization Demo - List Certificates Without Token` to see `401`.
4. Run `Get Demo Read Token`, then run:
   - `List Certificates` and expect `200`
   - `Authorization Demo - Read Token Cannot Create Certificate` and expect `403`
   - `Observability / Metrics` and expect `200`
   - `Observability / Metric By Name - JVM Memory Used` and expect `200`
   - `Observability / Prometheus` and expect `200`
5. Run `Get Demo Write Token`, then run:
   - `Create Certificate` and expect `201`
   - `Authorization Demo - Write Token Cannot List Certificates` and expect `403`
   - `Delete Certificate` still requires an admin role, even with write scope
   - `Observability / Metrics` and expect `403` if the write token is still active
6. Run `Get Demo Write Token (No Admin)`, then run:
   - `Delete Certificate` and expect `403`
   - inspect `accessTokenRoles` in the collection variables or Postman console and confirm `ADMIN` is absent
7. Run `Get Demo Admin Token`, then run:
   - `Delete Certificate` and expect `204`
   - inspect `accessTokenRoles` in the collection variables or Postman console to confirm `ADMIN` is present on the write user token
8. Run `Get Other Read Token`, `Get Other Write Token`, or `Get Other Admin Token` when you want to switch the collection to the `other-tenant` identity. The token requests store the decoded `tenantId` claim in `accessTokenTenantId`.

The collection also includes:

- `Observability / Health` and `Observability / Info` as public actuator checks
- `Observability / Metrics Without Token` and `Observability / Prometheus Without Token` to verify `401`
- tenant-bound create examples where the request body `tenantId` must match the JWT claim

Expected environment variable names in Postman:

- `baseUrl`
- `keycloakBaseUrl`
- `realm`
- `demoReadClientId`
- `demoReadClientSecret`
- `demoReadUsername`
- `demoReadPassword`
- `demoWriteClientId`
- `demoWriteClientSecret`
- `demoWriteUsername`
- `demoWritePassword`
- `demoWriteNoAdminUsername`
- `demoWriteNoAdminPassword`
- `demoTenantId`
- `otherReadUsername`
- `otherReadPassword`
- `otherWriteUsername`
- `otherWritePassword`
- `otherTenantId`

## Security

Please do not open public issues for suspected vulnerabilities. Use the process in [SECURITY.md](/Users/thomaswintherbonderup/Development/combotto-control-plane-api/SECURITY.md) instead.

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
