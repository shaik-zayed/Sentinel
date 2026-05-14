# Sentinel - Vulnerability Assessment Platform

A distributed, microservices-based vulnerability assessment platform that allows security professionals and developers
to run Nmap network scans, enrich results with CVE findings, and generate detailed reports, all through a secure,
authenticated REST API.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?style=flat-square)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2025-6DB33F?style=flat-square)
![Kafka](https://img.shields.io/badge/Kafka-KRaft-231F20?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?style=flat-square)
![MinIO](https://img.shields.io/badge/MinIO-S3-C72E49?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

---

## Overview

Users register, verify their email, and log in. Once authenticated, they configure a network scan choosing the target,
scan type, port range, OS detection, and service version detection. The scan request is submitted through the API
Gateway and processed **asynchronously**: a Kafka pipeline decouples the request intake from the actual scan execution,
allowing multiple users to run concurrent scans without blocking.

A dedicated Nmap service consumes scan jobs from Kafka, dynamically spins up an ephemeral Docker container running Nmap,
executes the scan, captures the output, and publishes results back through Kafka. The scan service persists results to
MySQL and immediately triggers **CVE enrichment** -- querying the NVD API to match detected service versions against
known vulnerabilities and storing findings with severity scores.

Users can then download a report for any completed scan in their chosen format (PDF, HTML, JSON, or DOCX), generated
on-demand and cached in MinIO object storage.

---

## Architecture

![Architecture Diagram](./assets/architecture.svg)

## Flow Diagram

![Flow Diagram](./assets/flow.svg)

---

## Services

| Service          | Port | Responsibility                                                                                                         |
|------------------|------|------------------------------------------------------------------------------------------------------------------------|
| `api-gateway`    | 7777 | Single entry point: JWT validation, load-balanced routing via Eureka, request ID tracing                               |
| `auth-service`   | 8083 | User registration, email verification, JWT access + refresh tokens, token blacklisting, logout                         |
| `scan-service`   | 8081 | Accepts scan requests, publishes to Kafka, persists results, triggers CVE enrichment via NVD API, exposes findings API |
| `nmap-service`   | 8082 | Consumes scan jobs from Kafka, builds Nmap command, spins up ephemeral Docker container, publishes results             |
| `report-service` | 8085 | Fetches scan results + CVE findings, generates PDF/HTML/JSON/DOCX reports, caches in MinIO, serves downloads           |
| `eureka-server`  | 8761 | Netflix Eureka service registry for service discovery                                                                  |

---

## Tech Stack

| Category             | Technology                                                         |
|----------------------|--------------------------------------------------------------------|
| Language             | Java 21 (source compatibility: Java 17)                            |
| Framework            | Spring Boot 4.0, Spring Cloud 2025                                 |
| API Gateway          | Spring Cloud Gateway (WebFlux reactive)                            |
| Auth                 | Spring Security + JJWT 0.12.6 with access/refresh token lifecycle  |
| Messaging            | Apache Kafka (KRaft mode, no Zookeeper)                            |
| Container Management | docker-java 3.7.0 for programmatic Docker container lifecycle      |
| Database             | MySQL 8, schema initialised via `init.sql`                         |
| Object Storage       | MinIO (S3-compatible)                                              |
| CVE Intelligence     | NVD API with CPE + keyword lookup, rate-limited, async enrichment  |
| Service Discovery    | Netflix Eureka                                                     |
| Infrastructure       | Docker + Docker Compose with health checks and dependency ordering |
| Dev Email            | smtp4dev, local SMTP server + web UI                               |

---

## Design Decisions

**Why Kafka instead of direct HTTP between scan-service and nmap-service?**

Nmap scans are slow; a full port scan can take minutes. A synchronous HTTP call would block the scan-service thread for
the entire duration, making the system unable to handle concurrent users. Kafka decouples the two services completely:
scan-service publishes a job and immediately returns a scan ID to the user. The nmap-service processes jobs
independently, at its own pace, with a thread pool handling up to 20 concurrent scans. Results flow back through a
separate Kafka topic.

**Why does the API Gateway handle JWT validation instead of each service?**

Centralised enforcement. If each service independently validated tokens, every service would need the JWT secret,
validation logic, and blacklist access. The gateway is the single entry point; only verified, non-blacklisted requests
pass through. Downstream services trust the gateway completely.

**Why ephemeral Docker containers for Nmap?**

Nmap requires raw network access (`CAP_NET_RAW`) and running it in an isolated container per scan prevents any scan from
affecting the host or other concurrent scans. Containers are created, used, and deleted with no state bleeding between
runs.

**Why MinIO instead of saving reports to disk?**

Saving files to a service's local filesystem breaks in a containerised environment because restarts lose data. MinIO
provides
persistent, S3-compatible object storage that survives container restarts and scales independently of the application
services.

**Why async CVE enrichment instead of blocking on the NVD API?**

The NVD public API is rate-limited (5 requests per 30 seconds unauthenticated) and can be slow. Enrichment runs in a
dedicated thread pool after scan results arrive, so the scan status reaches `FINISHED` immediately and clients can poll
`/findings` independently. Enrichment status transitions from `PENDING → IN_PROGRESS → COMPLETED` (or `PARTIAL` /
`FAILED` / `NOT_APPLICABLE` if no versioned services were detected).

---

## API Reference

All endpoints (except registration, login, and email verification) require an `Authorization: Bearer <accessToken>`
header and go through the API Gateway at `http://localhost:7777`.

### Auth - `/api/v1/auth`

| Method | Endpoint               | Description                                   |
|--------|------------------------|-----------------------------------------------|
| `POST` | `/register`            | Register a new user                           |
| `POST` | `/verify-email`        | Verify email address with token               |
| `POST` | `/resend-verification` | Resend the verification email                 |
| `POST` | `/login`               | Login, returns access + refresh tokens        |
| `POST` | `/refresh`             | Exchange refresh token for a new access token |
| `POST` | `/logout`              | Invalidate current access token (blacklist)   |
| `POST` | `/logout-all-devices`  | Revoke all refresh tokens across all devices  |
| `GET`  | `/validate`            | Validate a token                              |
| `POST` | `/forgot-password`     | Initiate password reset, sends reset email    |
| `POST` | `/reset-password`      | Complete password reset with token            |

### Users - `/api/v1/users`

| Method   | Endpoint              | Description                                    |
|----------|-----------------------|------------------------------------------------|
| `GET`    | `/me`                 | Get the currently authenticated user's profile |
| `PUT`    | `/me/change-password` | Change the current user's password             |
| `GET`    | `/{id}`               | Get a user profile by ID                       |
| `GET`    | `/getAllUsers`        | List all users *(admin)*                       |
| `DELETE` | `/{id}`               | Delete a user *(admin)*                        |
| `POST`   | `/{id}/unlock`        | Unlock a locked user account *(admin)*         |

### Scans - `/api/v1/scan`

| Method   | Endpoint                | Description                                                                                                   |
|----------|-------------------------|---------------------------------------------------------------------------------------------------------------|
| `POST`   | `/submit`               | Submit a new scan, returns `202 Accepted` with `scanId`                                                       |
| `GET`    | `/{scanId}`             | Get full scan details                                                                                         |
| `GET`    | `/{scanId}/status`      | Poll scan status (`ACCEPTED` -> `QUEUED` -> `STARTED` -> `FINISHED` / `FAILED`)                               |
| `GET`    | `/{scanId}/result`      | Get the raw Nmap XML output for a completed scan                                                              |
| `GET`    | `/{scanId}/findings`    | Get CVE findings, returns enrichment status + list of findings. Returns `202` while enrichment is in progress |
| `GET`    | `/list?limit=20&page=0` | Paginated list of the authenticated user's scans                                                              |
| `DELETE` | `/{scanId}`             | Delete a scan record                                                                                          |

#### Enrichment Status Values

| Status           | Meaning                                                                         |
|------------------|---------------------------------------------------------------------------------|
| `PENDING`        | Enrichment not yet started                                                      |
| `IN_PROGRESS`    | NVD API calls are running                                                       |
| `COMPLETED`      | All CVEs fetched and saved, findings list is final                              |
| `PARTIAL`        | Some services fetched; at least one NVD call failed, list may be incomplete     |
| `FAILED`         | Enrichment failed after all retries, no findings saved                          |
| `NOT_APPLICABLE` | Scan detected no services with identifiable product/version, nothing to look up |

### Reports - `/api/v1/report`

| Method | Endpoint                        | Description                                                                                   |
|--------|---------------------------------|-----------------------------------------------------------------------------------------------|
| `GET`  | `/{scanId}/download?format=PDF` | Generate (or serve cached) report and stream the file. Formats: `PDF`, `HTML`, `JSON`, `DOCX` |
| `GET`  | `/{scanId}/formats`             | List which formats are already cached in MinIO for this scan                                  |

---

## Running Locally

### Prerequisites

- Docker Desktop (or Docker Engine + Compose)
- Java 17+
- Maven 3.8+

### Setup

**1. Clone the repository**

```bash
git clone https://github.com/shaik-zayed/Sentinel.git
cd Sentinel
```

**2. Configure Docker Desktop (Windows/macOS only)**

The `nmap-service` communicates with Docker using the Docker TCP socket.

Open Docker Desktop → Settings → General

Enable:

- `Expose daemon on tcp://localhost:2375 without TLS`

Then click **Apply & Restart**.

> This setting is required to run the project locally on Docker Desktop (Windows/macOS), because the application uses `docker-java` to create temporary Nmap containers dynamically.


**3. Configure environment**

```bash
cp .env.example .env
```

Open `.env` and set `JWT_SECRET_KEY` to a Base64-encoded 256-bit key:

```bash
openssl rand -base64 32
```

**4. Build all services**

```bash
mvn clean package -DskipTests
```

**5. Start the full stack**

```bash
docker compose up --build
```

Services start in dependency order: MySQL and Kafka → Eureka → API Gateway → application services. First run takes ~2–3
minutes for image pulls.

### Verify Everything Is Up

| Service             | URL                                   | Credentials                |
|---------------------|---------------------------------------|----------------------------|
| Eureka dashboard    | http://localhost:8761                 | -                          |
| MinIO console       | http://localhost:9001                 | `sentinel` / `sentinel123` |
| Email UI (smtp4dev) | http://localhost:5000                 | -                          |
| Gateway health      | http://localhost:7777/actuator/health | -                          |

### Quick Test Flow

```
1. Register    POST  /api/v1/auth/register
2. Verify      Open  http://localhost:5000  →  click the verification link
3. Login       POST  /api/v1/auth/login     →  copy the accessToken
4. Scan        POST  /api/v1/scan/submit            Authorization: Bearer <token>
5. Poll        GET   /api/v1/scan/{scanId}/status   (until FINISHED)
6. Findings    GET   /api/v1/scan/{scanId}/findings (poll until enrichmentStatus is COMPLETED)
7. Download    GET   /api/v1/report/{scanId}/download?format=PDF
```

HTTP request files for IntelliJ / VS Code are in `/http/`.

### Scan Request Payload

| Field                  | Type      | Values                                                    |
|------------------------|-----------|-----------------------------------------------------------|
| `target`               | `string`  | IP address or hostname, e.g. `192.168.1.1`, `example.com` |
| `scanMode`             | `string`  | `LIGHT` or `DEEP`                                         |
| `protocol`             | `string`  | `TCP` or `UDP`                                            |
| `portMode`             | `string`  | `COMMON` or `LIST`                                        |
| `portValue`            | `string`  | `top-100`, `top-1000`, or custom e.g. `80,443,8080`       |
| `detectOs`             | `boolean` | `true` or `false`                                         |
| `detectServiceVersion` | `boolean` | `true` or `false`                                         |

---

## Project Structure

```
Sentinel/
├── api-gateway/          Spring Cloud Gateway + JWT validation filter
├── auth-service/         Registration, login, JWT lifecycle, email verification, password reset
├── scan-service/         Scan request intake, Kafka producer/consumer, CVE enrichment via NVD, findings API
├── nmap-service/         Kafka consumer, Docker container orchestration, Nmap execution
├── report-service/       Report generation (PDF/HTML/JSON/DOCX) + MinIO upload and download
├── eureka-server/        Netflix Eureka service registry
├── mysql-init/           Database schema initialisation (init.sql)
├── infra/                Docker Compose files for full-stack and dev environments
├── assets/               Architecture and flow diagrams (.mmd source + .svg renders)
├── http/                 IntelliJ HTTP client request files for manual testing
├── docker-compose.yml    Full stack orchestration with health checks
├── Dockerfile            Multi-service layered build (SERVICE_NAME build arg)
└── pom.xml               Parent Maven POM
```

---

## Known Limitations

- No frontend UI -- API only (IntelliJ HTTP request files provided for testing)
- Limited automated test coverage -- manual testing via HTTP files
- Single Kafka broker with replication factor 1 -- not production-grade for high availability
- Docker socket mount required for Nmap containers -- review security implications before deploying to production
- NVD API used without an API key -- rate limited to 5 requests per 30 seconds; enrichment may be slow for scans with
  many detected services
- Prometheus / Grafana metrics integration stubbed but not enabled

---

## Security & Legal Notice

- This project is intended for educational, research, and authorized security testing purposes only.

- Only scan systems, networks, or IP addresses that you own or have explicit permission to test. Unauthorized network scanning may violate applicable laws, regulations, or organizational policies.

- The author is not responsible for any misuse, damage, legal consequences, or unauthorized activities performed using this software. By using this project, you agree that you are solely responsible for your actions and for complying with all applicable laws and regulations.

---

## Author

**Shaik Zayed** · [LinkedIn](https://linkedin.com/in/shaik-zayed) · [Portfolio](https://shaiks.vercel.app)