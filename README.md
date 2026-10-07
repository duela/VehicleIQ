# VehicleIQ

VehicleIQ is a local-first used-vehicle intelligence MVP. It provides a dealer workspace, a platform admin area, an explainable rules-based assessment, and a partner API sandbox. The system is deliberately clear about its data limits: the sample build does **not** connect to Copart, other auction companies, marketplaces, or a licensed vehicle-history provider.

The approved [technology architecture and system design](./vehicleiq-technology-architecture.md) remains the broader target. This repository contains its first runnable product slice.

## Start locally

Requirements: Java 21, Maven 3.9+, Node.js 20+, and npm. Docker is optional for the local H2 profile.

```bash
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Select **Dealer workspace** or **Platform admin** on the local demo sign-in page. The gateway runs on port 4000 and the Spring Boot core on port 8080. The local profile uses an H2 file database and local filesystem evidence storage, so the app works without PostgreSQL, Redis, MinIO, or Docker.

If Maven should use a repository-local dependency cache, set `MAVEN_REPO=.cache/maven`; the default in the npm scripts uses that ignored cache path. You can instead point `MAVEN_REPO` to an existing Maven repository.

The local development token is signed with the `JWT_SECRET` in `.env.example`. For a shared development machine, copy `.env.example` to `.env` and set a unique secret before starting. Demo credentials are not suitable for a publicly reachable environment.

## Run the container stack

```bash
cp .env.example .env
docker compose up --build
```

Open [http://localhost:8088](http://localhost:8088). Compose starts PostgreSQL, Redis Streams, MinIO, Spring Boot, the Node gateway/integration worker, and the React application. Evidence goes to the S3-compatible MinIO bucket. Named volumes hold local development data; `docker compose down -v` deletes those volumes.

The bundled Compose configuration is a development environment, not a production deployment. It uses local demo identity, default-only service credentials unless `.env` is changed, and no production TLS, cloud network policy, OIDC identity provider, payment processor, or contracted data provider.

## Product areas

- **Dealer workspace:** inventory overview, vehicle intake, acquisition costs, comparable prices, condition notes, evidence upload, assessment report, CSV import, and support tickets.
- **Assessment engine:** versioned deterministic rules, transparent cost calculation, user-provided comparable range, risk/evidence prompts, condition notes, and verification availability status. The report states the source and limitations.
- **Admin area:** demo tenant overview, vehicle records across tenants, support queue/status, connector health, platform counts, and recent audit events.
- **Partner API sandbox:** static local API key, per-minute edge limit, asynchronous assessment request and status retrieval. OAuth bearer tokens are passed through when the core is configured for an OIDC issuer.

## Architecture and services

| Part | Location | Responsibility |
|---|---|---|
| React/JavaScript | `web/` | Dealer and admin application |
| Node.js/JavaScript | `gateway/` | API edge, local sign-in, API key check, OAuth bearer pass-through, Redis stream consumer |
| Java 21 / Spring Boot | `core/` | Tenant-scoped domain API, analysis, persistence, support, audit and outbox |
| PostgreSQL | Compose profile | Persistent system of record |
| H2 | Local profile | Docker-free local development database |
| Redis Streams | Compose profile | Outbox event delivery to the Node worker |
| MinIO / S3 API | Compose profile | Evidence object storage; local profile uses `data/uploads/` |

The Spring core is a modular monolith. Creating an assessment writes the analysis and an outbox event. In the local profile, a scheduled core dispatcher completes the deterministic assessment. In Compose, the dispatcher publishes the outbox event to `vehicleiq:events`; the Node worker consumes it and calls the protected internal completion route. Both paths are idempotent at analysis completion.

### Domain data

The MVP persists vehicles, analyses, comparable inputs, evidence metadata, support tickets, audit events, and outbox events. Every tenant-owned record has a tenant identifier. Tenant scope comes from a verified signed token, never a request-body tenant field. Platform admins can inspect all demo tenants. Provider observations, license data, API client lifecycle, and payment records are not populated with invented data.

### Event topics

Current stream: `vehicleiq:events` with versioned `analysis.requested.v1` events. The dispatcher uses a PostgreSQL outbox and retries unprocessed rows. The Node worker uses a Redis consumer group and acknowledges only after the Spring core accepts completion. Future event contracts such as `source.data.received.v1`, `usage.recorded.v1`, `support.ticket.created.v1`, and `webhook.delivery.requested.v1` are listed in the architecture document but are not yet emitted by this slice.

### Data, source rights and model governance

Manual input and CSV imports are labelled as tenant-supplied. CSV headers: `registration,make,model,year,mileage,purchasePrice,buyerFees,transport,repairEstimate,comparablePrices`; comparable prices may be separated with `|` or `;`. Evidence uploads accept JPG, PNG, WebP, and PDF up to 12 MB. No scraped listings or third-party datasets are used. No machine-learning model is included; the rules engine records version `rules-1.0.0`.

## API quick reference

Browser routes are sent through the Node edge under `/api/v1` and rewritten to the core `/v1` API.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/dashboard` | Tenant dashboard |
| `GET`, `POST` | `/api/v1/vehicles` | List or create vehicles |
| `POST` | `/api/v1/vehicles/import` | Import up to 250 validated rows |
| `POST` | `/api/v1/vehicles/{id}/analyses` | Queue an assessment (`202 Accepted`) |
| `GET` | `/api/v1/analyses/{id}` | Read status and explained result |
| `POST`, `GET` | `/api/v1/support/tickets` | Create/list scoped support tickets |
| `GET` | `/api/v1/admin/dashboard`, `/api/v1/admin/audit` | Admin-only operations and audit |
| `POST`, `GET` | `/partner/v1/vehicle-assessments` | Partner sandbox request/status |

See [OpenAPI contract](./docs/openapi.yaml) for request and response schemas. API timestamps are UTC ISO-8601; vehicle money fields are GBP in the UK MVP. The partner API accepts `x-api-key: viq_demo_local_only_7f0c3e91` in local mode. Never use this public demo value with real data.

### OIDC / OAuth configuration

For an external OIDC provider, set `OIDC_ENABLED=true` and `OIDC_ISSUER` for the Spring core. The provider must issue signed JWTs containing `tenant_id` and a `roles` array (`DEALER`, `ADMIN`, or `PARTNER`). Send the access token as `Authorization: Bearer …` to the partner routes. The gateway forwards the bearer token and the Spring resource server validates its issuer/signature. A production provider, realm/client setup, user lifecycle, consent, and key rotation policy are not included. Local browser login instead issues a short-lived HMAC-signed development token.

## Roadmap

- **MVP:** dealer intake and reports; CSV and manual inputs; local roles/tenant scope; support; admin audit; local API sandbox; explicit source limitations.
- **Year 1:** secure hosted OIDC, commercial plan entitlements, first licensed source if contracted, private API pilot, improved source-quality operations.
- **Year 2:** production OAuth client credentials, credential lifecycle, usage ledger and billing integration, first auction/marketplace adapter after signed rights and technical access, production webhooks.
- **Year 3:** regional data adapters, market-specific rules/models, enterprise integrations, service tiers, and stronger regional disaster recovery.

Do not present future partners or integrations as active commitments.

## Security and operational notes

- Demo role selection is enabled by default for local preview. Set `ENABLE_DEMO_AUTH=false` on a hosted environment and configure OIDC before making it reachable.
- Spring checks JWT roles and tenant scope. The demo API key is compared using a SHA-256 digest and constant-time comparison, but is a shared static credential without rotation or per-client persistence.
- Evidence is checked by MIME type and size. Malware scanning and short-lived download URLs are not included in this first version.
- Actuator health is available at `/actuator/health`; the Node health endpoint is `/health`; gateway counters are exposed at `/metrics`.
- Local PostgreSQL/Redis/MinIO are not backed up automatically. Production backup/RPO/RTO and cloud deployment requirements remain in the architecture document.

## Build checks

```bash
npm run build
MAVEN_REPO=/path/to/maven-cache mvn -f core/pom.xml test
node scripts/smoke.mjs
```

The smoke script expects the local core and gateway to be running. It checks tenant isolation, assessment completion and provenance, evidence upload, support, admin audit access, and partner API retrieval. The browser checklist is to sign in as a dealer, create a vehicle with comparables, review the report limitations, inspect support in the admin workspace, and use the API sandbox page.
