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
