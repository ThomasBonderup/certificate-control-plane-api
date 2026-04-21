# Security Policy

## Supported Use

This repository is currently intended for local development and review environments. Production deployment hardening, hosted operations, and secret management are outside the scope of the checked-in defaults.

## Reporting a Vulnerability

Please do not report security vulnerabilities in public GitHub issues.

Send a private report to the repository maintainers with:

- a short description of the issue
- affected endpoints or files
- reproduction steps or proof-of-concept details
- any suggested mitigation if you have one

The maintainers will acknowledge receipt, assess severity, and follow up privately.

## Secret Handling

- Do not commit `.env` files, real Postman environments, bearer tokens, private keys, or production credentials.
- The committed `.env.example` and Postman environment template are sanitized placeholders only.
- Replace every `REPLACE_ME_...` value with local-only secrets outside version control.
