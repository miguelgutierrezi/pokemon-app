# Quickstart validation: Setup (project skeleton)

**Feature**: `001-setup` | **Date**: 2026-10-15 (revised)

Use this guide to prove the setup end-to-end after implementation.
Contracts: [health-api.yaml](./contracts/health-api.yaml),
[env-config.md](./contracts/env-config.md).

## Prerequisites

- Docker + Docker Compose v2
- JDK 21 and Maven 3.9+ (local backend verify)
- Node 24 LTS and npm (local frontend checks)

## 1. Configure environment

```bash
cp .env.example .env
```

Expected: the three `POSTGRES_*` keys are present with placeholders.
Removing one and running `docker compose config` fails with a message naming
it.

## 2. Start the stack (S1, SC-001)

```bash
docker compose up --build -d
docker compose ps
```

Expected: `postgres`, `backend`, `frontend` running and healthy; backend logs
show Flyway applied version `1`.

## 3. Health check (S1.2, SC-002)

```bash
curl -s -w "\n%{http_code}\n" http://localhost:8080/actuator/health
```

Expected: `200` and JSON with `"status":"UP"` and `components.db.status` UP.
After `docker compose stop postgres`, the same call returns `503`/`DOWN`
(restart Postgres afterwards).

## 4. Frontend (S1.3, SC-003)

Open `http://localhost:3000`. Expected: placeholder renders; browser console
has zero warnings.

## 5. API proxy (S1.4)

```bash
curl -s -i http://localhost:3000/api/ping
```

Expected: a JSON 404 produced by the backend (Spring error body, not the
Nginx HTML page), proving the path reached the backend unchanged.

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3000/actuator/health
```

Expected: not the health JSON (served by the SPA fallback), proving actuator
is not exposed through the UI origin.

## 6. Backend verify (S2, SC-004)

```bash
cd backend && mvn -B verify
```

Expected: Spotless check, ArchUnit, tests (incl. Testcontainers health/Flyway
IT) and JaCoCo ≥ 80% all pass.

## 7. Frontend quality gates (S3, SC-005)

```bash
cd frontend && npm ci && npm run lint && npm run typecheck && npm test && npm run build
```

Expected: all exit 0.

## 8. CI/CD (S4, SC-006)

Push `001-setup` and open one PR into `main`. Expected: `changes`, `backend`,
and `frontend` jobs green (backend and frontend in parallel, each building its
image) and `publish` skipped. Merge with a merge commit; on `main`, `publish`
runs and both images appear in GHCR
(`ghcr.io/<owner>/pokemonapp-backend` and `-frontend`, tags `<sha>`, `latest`).

## 9. Docs (S5, SC-007)

README includes overview, link to `docs/requirements.md`, architecture
placeholder, local run, Docker run, tests.

## Tear down

```bash
docker compose down -v
```
