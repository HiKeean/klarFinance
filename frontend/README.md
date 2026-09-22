# checker-bm

Web dashboard untuk **Checker** dan **BM (Branch Manager)** — antarmuka internal untuk
verifikasi pengajuan (KYC review) dan persetujuan limit/pinjaman nasabah KlarFinance.

## Tech Stack

- **Angular 22** (standalone components), SSR-ready (`serve:ssr:checker-bm`)
- **Angular CDK**
- REST + JWT ke [KlarFinance API](../backend/klarfinance/README.md)
- WebSocket (WSS) untuk realtime assignment/status checker

## Peran dalam sistem

Salah satu dari tiga klien yang bicara ke satu backend Spring Boot yang sama (lihat
[backend README](../backend/klarfinance/README.md)):

- **Auth**: JWT access + refresh token, disimpan lewat `core/interceptors`.
- **Realtime**: koneksi WebSocket ke `{baseUrl}/ws` (lihat `checker-realtime.service.ts`) untuk
  status assignment checker — satu pengajuan cuma dipegang satu checker dalam satu waktu
  (lock 2 jam, event-driven di backend).
- **Fitur utama**: `features/auth`, `features/dashboard`, `features/inquiry` (cek/verifikasi
  pengajuan), `features/approval` (approval BM).

## Getting Started

```bash
npm install
ng serve
```

Buka `http://localhost:4200/`.

### Konfigurasi backend

Base URL API diatur di `src/environments/environment.ts` / `environment.development.ts`
(`environment.api.baseUrl`). Untuk dev lokal, arahkan ke instance backend lokal
(`http://localhost:8080/api/v1`); default production mengarah ke
`https://api.klarfinance.hizkialb.xyz/api/v1`.

## Struktur

```
src/app/
  core/         # config, guards, interceptors, layouts, services (termasuk realtime WS)
  features/
    auth/       # login
    dashboard/
    inquiry/    # verifikasi pengajuan (checker)
    approval/   # approval limit (BM)
  shared/       # components & models bersama
```

## Scripts

| Command | Fungsi |
|---|---|
| `ng serve` | dev server, hot reload |
| `ng build` | build production ke `dist/` |
| `ng test` | unit test (Vitest) |
| `npm run serve:ssr:checker-bm` | jalankan build SSR (`dist/checker-bm/server/server.mjs`) |

## Deploy

`.github/workflows/frontend.yml` — build lewat Vercel CLI, deploy preview otomatis tiap PR,
production tiap push ke `main`.
