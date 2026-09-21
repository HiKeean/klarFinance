# KlarFinance — Features & Security

Katalog fitur dan mekanisme security yang benar-benar ada di kode (backend Spring Boot, Android/Kotlin, frontend "checker-bm", webadmin). Disusun dari hasil audit codebase, bukan dari asumsi dokumen PRD/BRD.

---

## 1. Features

### 1.1 Backend (core business logic)

| Fitur | Deskripsi | Lokasi utama |
|---|---|---|
| Onboarding nasabah | OTP WhatsApp, upload KTP/KYC, scoring mock Pefindo & Vida | `auth/controller/AuthController.java`, `auth/service/AuthService.java` |
| LOS (Loan Origination System) | Pengajuan limit → antrian Checker (real-time WebSocket) → keputusan BM | `los/controller/LosController.java`, `los/service/LosApprovalService.java`, `CheckerAssignmentService.java` |
| Pinjaman (cash loan) | Tarik dana dari limit aktif; auto-approve ≤30% limit, >30% ke review BM | `fin/controller/LoanController.java`, `fin/service/LoanService.java` |
| Review pinjaman besar (>30% plafond) | Endpoint keputusan terpisah untuk BM | `fin/controller/LoanReviewController.java`, `LoanReviewService.java` |
| Repayment ("Bayar") | Bayar fleksibel, dialokasikan FIFO ke cicilan, limit balik proporsional; status histori dihitung on-the-fly dari due date (bukan cron job) — ini yang paling dekat dengan "auto update status bill" | `LoanService.repay` / `getMyLoanHistory` |
| QRIS scan & pay | Scan QR merchant → token konfirmasi sementara (5 menit) → bayar pakai limit | `qris/controller/QrisTransactionController.java`, `qris/service/QrisService.java` |
| QRIS merchant generator | Endpoint publik tanpa auth untuk bikin QR merchant demo | `qris/controller/QrisMerchantController.java` |
| TransJakarta | Beli tiket pakai limit; transaksi sebulan digabung jadi 1 tagihan bulanan (jatuh tempo tgl 25) | `transjakarta/controller/TransjakartaTicketController.java`, `TransjakartaTicketService.java` |
| Referral program | Kode referral saat registrasi, reward per pinjaman | `referral/controller/ReferralController.java` |
| NPL Report API | Data NPL per cabang (severity) buat peta di webadmin | `dbo/controller/NplReportController.java`, `NplReportService.java` |
| Manajemen cabang & wilayah | CRUD cabang, territory mapping provinsi/kabupaten | `dbo/controller/BranchController.java`, `BranchTerritoryController.java` |
| Location tracking nasabah | Opt-in tracking + inferensi "rumah/rutinitas" untuk cross-check alamat KTP | `geo/controller/LocationTrackingController.java`, `geo/service/RoutineInferenceService.java` |
| Push notification (FCM) | Notif approve/reject pinjaman ke nasabah | `global/PushNotificationService.java`, `config/FirebaseConfig.java` |
| Superadmin: user/role/menu | CRUD staff, role, menu, role-menu mapping (RBAC dinamis) | `auth/controller/AuthenticationSuperadminController.java` |
| Forced password reset | User request → SUPERADMIN approve/reject → password baru random dikirim WhatsApp | `auth/service/PasswordResetRequestService.java` |

### 1.2 Android app (Kotlin)

Layar utama di bawah `kotlin/app/src/main/java/com/klarfinance/app/presentation/`:

- Login (password/OTP), register (+ selfie, scan KTP via OCR, verifikasi), splash
- Home, Loan (+ amount, bank account), QRIS (+ amount, scan), TransJakarta (+ home, confirm)
- Bills (gabungan tagihan — konsumsi endpoint unified loan-history), History, Account, Referral
- Biometric unlock (BiometricPrompt) untuk redeem refresh token saat buka app
- Scan aplikasi terinstall (deteksi app pinjol ilegal) → input scoring backend
- KTP OCR parser untuk prefill data registrasi

### 1.3 Frontend "checker-bm" (Angular — app internal Checker & BM)

- Login (+ tawaran reset password setelah 3x gagal), reauth saat sesi expired
- Dashboard ringkasan: jumlah butuh review, total pinjaman region, badge NPL
- Antrian approval: Checker auto-routed ke aplikasi yang di-assign (via WebSocket), BM lihat tabel dengan lock/"take a break"
- Approval detail limit (BM bisa negosiasi naik s.d. Rp2.000.000) & loan review detail (>30% plafond)
- Inquiry/search aplikasi berdasarkan status

### 1.4 Webadmin (Angular — panel superadmin)

- Landing page publik
- Manajemen user staff, role, menu, role-menu mapping
- Manajemen cabang & territory (assign wilayah ke cabang)
- **NPL Report — peta choropleth (Leaflet + OpenStreetMap)** per kabupaten/kota Indonesia, diwarnai hijau/kuning/merah sesuai severity NPL cabang; drill-down ke detail pinjaman per cabang
- Review request reset password staff (approve → password baru dikirim WhatsApp, reject + alasan)

### 1.5 qris-generator (modul terpisah)

Static site (HTML/JS) untuk generate QR merchant demo, manggil endpoint publik backend `/api/v1/qris/merchants`, sengaja tanpa login/HMAC by design.

---

## 2. Security

### 2.1 Autentikasi & Token

- JWT (HS256), stateless, tapi token record disimpan di DB sehingga bisa direvoke server-side (tidak cuma andalkan expiry) — `config/JwtService.java`, `JwtAuthenticationFilter.java`
- Access token 1 hari, refresh token 7 hari. **Refresh flow ada di backend tapi tidak dipakai** oleh frontend/webadmin — kalau token expired, user diarahkan login ulang via `/reauth`
- Step-up auth (`POST /auth/verify-password`) sebelum konfirmasi transaksi pinjaman/QRIS kalau fingerprint Android tidak aktif
- Biometric app-lock (AndroidX BiometricPrompt) untuk unlock refresh token saat buka app

### 2.2 Password & OTP

- BCrypt untuk hashing password (`ApplicationConfig.java`)
- OTP 6-digit via WhatsApp (provider "Kirimi") untuk nasabah — TTL OTP 5 menit, status verified 15 menit, disimpan di Redis
- Reset password **bukan self-service**: request user → approval SUPERADMIN → password random (SecureRandom, 10 karakter) dikirim via WhatsApp

### 2.3 RBAC / Roles

- Role: `NASABAH`, `CHECKER`, `BM`, `SUPERADMIN` + sistem role→menu dinamis via DB, dikelola SUPERADMIN
- Enforcement di layer Spring Security cuma untuk `/api/v1/admin/**` (SUPERADMIN) dan `/api/v1/nasabah/**` (NASABAH). **`/api/v1/internal/**` (endpoint Checker/BM) cuma dicek "sudah login", tanpa role spesifik** — beda Checker vs BM dicek manual via string compare di service, bukan `@PreAuthorize`
- Web frontend juga tidak punya route guard berbasis role — gating role dilakukan di dalam komponen (bisa dibypass di sisi client, aman karena backend jadi source of truth, tapi tetap gap desain)

### 2.4 Request Signing (HMAC)

- Semua request (kecuali auth/swagger/ws) wajib bawa header `X-Signature` (HMAC-SHA256), `X-Timestamp` (anti-replay, window 5 menit), `X-Client-Type` — API key per client-type disimpan di DB, bukan satu secret global
- Diterapkan di Android (`HmacInterceptor.kt`) dan web (`api.service.ts`)

### 2.5 WebSocket "Checker"

- STOMP over WebSocket; JWT dikirim via query param `?token=` (keterbatasan handshake browser), divalidasi manual di `WebSocketAuthInterceptor.java`
- Antrian/lock assignment Checker dikelola di Redis: lock 2 jam per aplikasi yang dibuka, window 5 menit sebelum reassign, tombol "take a break" untuk release manual

### 2.6 Enkripsi & Data Sensitif

- Android: refresh token disimpan di `EncryptedSharedPreferences` (Android Keystore, AES256-GCM) — solid
- Web (checker-bm & webadmin): token disimpan di cookie, dienkripsi AES-GCM di client-side, **tapi key enkripsinya hardcoded di bundle JS** — jadi cuma menutupi dari inspeksi kasual, bukan proteksi sungguhan
- Backend: **NIK, NPWP, dan nomor rekening disimpan plaintext** di DB — tidak ada field-level encryption meski ada config `cryptoKey` yang tidak dipakai di mana pun

### 2.7 Push Notification & Integrasi Pihak Ketiga

- Firebase Cloud Messaging untuk notif approve/reject pinjaman — kredensial via service-account JSON (gitignored), startup tidak gagal kalau file belum ada
- Firebase di webadmin/frontend baru di-wire providernya, **belum benar-benar dipakai** (config masih placeholder)
- Kirimi (WhatsApp API) untuk kirim OTP & password baru

### 2.8 Temuan yang Perlu Diperhatikan

1. **JWT secret key hardcoded plaintext** di `application.yml` — beda sendiri dari kredensial lain (DB/Redis/Kirimi) yang sudah pakai env var
2. `HmacSignatureFilter.java` nge-log API key hasil resolve + full request body di level `INFO` pada setiap request — kebocoran data sensitif ke log aplikasi
3. Tidak ada rate limiting / lockout login di backend, padahal ada UI yang menyebut "3x salah password"
4. QRIS merchant endpoint sengaja publik tanpa auth (memang didesain sebagai mockup)
5. Tidak ada certificate pinning di Android; ada allowlist cleartext HTTP untuk host dev
6. Tidak ada field-level encryption untuk data pribadi sensitif (NIK/NPWP/no. rekening) di backend

---

*Dibuat dari audit langsung terhadap kode per 2026-09-21, bukan dari dokumen PRD/BRD (yang sifatnya masih boilerplate/aspirational). Update ulang bagian ini kalau ada perubahan besar di flow auth, RBAC, atau modul fitur baru.*
