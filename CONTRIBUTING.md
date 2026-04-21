# Contributing

## Development Setup

1. Copy `.env.example` to `.env`.
2. Replace every `REPLACE_ME_...` placeholder with local-only values.
3. Start dependencies with `docker compose up -d postgres kafka keycloak`.
4. Run the app with `./gradlew bootRun`.

## Before Opening a Pull Request

- run `./gradlew test`
- keep local secrets out of git
- update docs or examples when behavior changes
- prefer small, reviewable changes

## Scope Notes

This repo currently focuses on certificate inventory, certificate-to-asset bindings, tenant-aware access control, and renewal workflow state. If you are proposing a broader platform change, please open an issue first so the direction is clear.
