# Klar Finance Webadmin

Web admin internal (RBAC) untuk KlarFinance — pengelolaan superadmin: manajemen user, region,
kunci wilayah, dan kontrol risiko (skor NPL). PWA dengan integrasi Firebase (App,
Authentication, Firestore, Storage) untuk kebutuhan admin-side di luar API utama.

## Tech Stack

- **Angular 21** (standalone), PWA (service worker)
- REST + **JWT** + **HMAC-SHA256 request signing** ke [KlarFinance API](../backend/klarfinance/README.md)
- Firebase Authentication / Firestore / Storage (terpisah dari Firebase Cloud Messaging yang
  dipakai backend untuk push notif nasabah)

## Peran dalam sistem

Klien dengan lapis keamanan paling ketat dari tiga klien yang ada — tiap request ke backend
ditandatangani HMAC-SHA256 (method + path + timestamp + body + API key) selain bearer JWT
biasa. Lihat `service/api` dan `core/interceptors` untuk implementasi signing-nya.

> **Catatan keamanan**: `src/environments/environment.ts` saat ini menyimpan `apiKey` dan
> `secretKey` HMAC sebagai string biasa yang ikut ke-bundle ke browser. `secretKey` ini sama
> dengan `application.security.jwt.secret-key` di backend — perlu dipindah ke skema yang tidak
> expose secret ke client-side sebelum production sungguhan.

## Getting Started

```bash
npm install
ng serve
```

Buka `http://localhost:4200/`.

### Konfigurasi backend

`src/environments/environment.ts` (production) / `environment.development.ts` (dev):

```ts
api: {
  baseUrl: 'http://localhost:8080/api/v1', // atau URL backend production
  apiKey: '...',
  secretKey: '...', // HMAC key, harus match backend
}
```

### Firebase setup

1. Buat project di [Firebase Console](https://console.firebase.google.com/), aktifkan
   Authentication, Firestore Database, dan Storage.
2. Salin konfigurasi Web App Firebase ke `environment.development.ts` (lokal) dan
   `environment.ts` (production).
3. Untuk Firebase Emulator Suite, set `useEmulators: true`.

Firebase Web config memang terlihat di browser — keamanan data tetap diatur lewat Firebase
Authentication dan Security Rules, bukan lewat menyembunyikan config.

## Struktur

```
src/app/
  core/            # config, guards, interceptors, layouts, services
  service/api/     # HTTP client + HMAC signing ke backend
  service/auth/    # auth state
  features/        # auth, dashboard, (landingpage)
  module/          # dashboard, login (module lama — cek duplikasi dgn features/)
  shared/          # components & models bersama
```

## Scripts

| Command | Fungsi |
|---|---|
| `ng serve` | dev server |
| `ng build` | build production (service worker aktif; PWA butuh HTTPS kecuali di localhost) |
| `ng test` | unit test (Karma) |
| `npm run serve:ssr:webadmin` | jalankan build SSR |

## Deploy

`.github/workflows/webadmin.yml` — build & deploy via **Vercel CLI** (preview tiap PR,
production tiap push ke `main`). `firebase.json` di repo ini hanya dipakai untuk konfigurasi
Firebase project (Auth/Firestore/Storage), bukan untuk hosting.
