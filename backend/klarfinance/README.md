# KlarFinance API

Backend Spring Boot untuk KlarFinance — platform pinjaman & Pay Later berbasis QRIS. Satu
modular monolith yang melayani tiga klien (Android nasabah, web checker/BM, web admin) lewat
satu REST + WebSocket API.

## Tech Stack

- **Java 21**, **Spring Boot 3.4.1**
- **Spring Security** — JWT (access + refresh token, role-based) + HMAC-SHA256 request signing
- **Spring Data JPA** — SQL Server (`mssql-jdbc`)
- **Spring Data Redis** — 3 instance terpisah (lihat [Redis](#redis-3-instance-terpisah))
- **Spring WebSocket** — realtime checker assignment (STOMP)
- **JJWT** — signing/parsing token
- Docker, deploy ke VPS self-hosted via GitHub Actions → GHCR

## Arsitektur singkat

Modular monolith, dipisah selaras skema database — antar-modul cuma boleh saling akses lewat
service, bukan repository langsung. Setiap top-level package (`auth`, `dbo`, `fin`, `los`, `geo`,
dst.) punya schema SQL Server sendiri:

| Package | Schema | Isi |
|---|---|---|
| `auth` | `auth` | Login, JWT, refresh token |
| `dbo` | `dbo` | Data referensi/param umum |
| `fin` | `fin` | Data finansial (plafon, transaksi) |
| `los` | `los` | Loan Origination System — pengajuan, scoring, approval |
| `geo` | `geo` | Location routine tracking (schema baru) |
| `user`, `admin`, `token`, `qris`, `referral`, `transjakarta` | — | fitur spesifik per domain |
| `global`, `config`, `annotation` | — | cross-cutting concerns |

### Decision Engine (LOS)

`EngineScoringService` memutuskan status pengajuan lewat dua tahap:

1. **KYC gate (Vida)** — `UNCLEAR` → minta foto ulang, `REJECTED` → tolak otomatis, hard stop
   (tidak lanjut ke Pefindo karena identitas sendiri belum lolos).
2. **Credit scoring (Pefindo)** — plafon ditentukan dari skor KOL.

> Vida dan Pefindo **masih mock** (`VidaResult`, `MockPefindoService`) — belum terhubung ke
> API pihak ketiga yang sesungguhnya.

### Redis (3 instance terpisah)

Sengaja dipisah per keperluan, bukan 1 instance dengan 3 prefix — supaya satu instance yang
lambat/penuh tidak mengganggu yang lain:

| Instance | Env var | Keperluan |
|---|---|---|
| Session/OTP | `REDIS_HOST` / `REDIS_PORT` / dst. (`app.redis.*`) | OTP, refresh token, checker-lock (2 jam, event-driven) |
| Cache | `CACHE_REDIS_URL` | Spring `@Cacheable` |
| Location-fail log | `LOCATION_FAILURE_LOG_REDIS_URL` | Telemetri "location not sent", isolated |

`CACHE_REDIS_URL` default fallback ke `LOCATION_FAILURE_LOG_REDIS_URL` kalau tidak di-set eksplisit.

### Integrasi eksternal

| Layanan | Fungsi | Status |
|---|---|---|
| Vida | KYC / liveness check | Mocked |
| Pefindo | Credit bureau inquiry | Mocked |
| Kirimi (`api.kirimi.id`) | Kirim OTP & notifikasi via WhatsApp | Aktif |
| Firebase Cloud Messaging | Push notif approve/reject pengajuan | Aktif (butuh service-account JSON) |

### Keamanan

Dua lapis di setiap request: **JWT** (access + refresh, role Nasabah/Checker/BM/Superadmin) dan
**HMAC-SHA256 request signing** (method + path + timestamp + body + API key, lihat
`HmacSignatureFilter`). Secret HMAC yang sama (`application.security.jwt.secret-key`) dipakai
bareng oleh Android app dan webadmin — lihat catatan di README masing-masing klien.

## Menjalankan lokal

Prasyarat: Java 21, akses ke instance SQL Server + 2–3 instance Redis (bisa yang sama untuk
cache & location-fail log saat dev).

```bash
cp .env.dev.example .env.dev   # kalau belum ada, lihat daftar env var di bawah
./mvnw spring-boot:run
```

Server jalan di `:8080` (di server produksi di-mapping ke `:1579` lewat `docker-compose.yaml`).

### Environment variables penting

```
DB_URL, DB_USERNAME, DB_PASSWORD
REDIS_HOST, REDIS_PORT, REDIS_USERNAME, REDIS_PASSWORD, REDIS_SSL_ENABLED, REDIS_TIMEOUT, REDIS_KEY_PREFIX
CACHE_REDIS_URL
LOCATION_FAILURE_LOG_REDIS_URL
KIRIMI_URL, KIRIMI_USER_CODE, KIRIMI_SECRET, KIRIMI_DEVICE_ID, KIRIMI_ENABLED
FIREBASE_SERVICE_ACCOUNT_PATH   # default firebase-service-account.json, JANGAN commit
CORS_ALLOWED_ORIGINS
```

`spring.jpa.hibernate.ddl-auto=update` — Hibernate auto-create schema baru (`hibernate.hbm2ddl.create_namespaces=true`) tapi tidak auto-migrate existing table secara destruktif.

## Deploy

`.github/workflows/backend.yml`: `mvn test` → build image → push ke
`ghcr.io/hikeean/klarfinance-backend` → scp `docker-compose.yaml` + SSH ke VPS → regenerate
`.env` dari GitHub Secrets → `docker compose up -d`. Tidak ada secret yang disimpan di server
selain lewat `.env` yang di-generate ulang tiap deploy.

## Catatan

- Multipart upload di-cap 10MB/file, 25MB/request (KTP + selfie dari kamera HP; Kotlin app
  sudah compress gambar ≤10MB sebelum kirim).
- Belum ada API real untuk Vida/Pefindo — prioritas berikutnya kalau mau lepas dari mock.
