# KLarFinance - Project Context & Rules

## 1. Tech Stack & Core Architecture
*   **Backend:** Java Spring Boot (JPA, Hibernate, Redis).
*   **Frontend Mobile:** Kotlin Jetpack Compose (Android).
*   **Database:** PostgreSQL (Schema: `los`).

## 2. Onboarding & Registration Flow
*   **Step 1 (OTP Request):** Nasabah input nomor HP. Backend menembak API OTP via `POST https://api.kirimi.id/v1/send-message` dengan payload `{user_code, secret, device_id, phone, message}`. Simpan OTP di Redis Cache.
*   **Step 2 (OTP Validation):** Nasabah input OTP ke backend untuk divalidasi dengan data di Redis.
*   **Step 3 (User Data Form):** Jika valid, nasabah mengisi formulir: password, NIK, NPWP, nama, alamat, dan email.
*   **Step 4 (KYC Upload):** Nasabah mengunggah foto KTP dan foto selfie (KYC).
*   **Step 5 (Device App Scoring):** Aplikasi Android akan memindai *package* aplikasi yang terinstal menggunakan `PackageManager`. Kirim *payload* `EngineScoring { pinjol: [], Bank: [] }` ke backend. 
    *   *Referensi Pinjol OJK:* Deteksi aplikasi seperti Kredivo, RupiahCepat, Akulaku, PinjamanGo, Kredit Pintar, Maucash, AdaKami, Indodana, JULO, Easycash, Uangme, dll.
*   **Step 6 (Dummy API Integrations):** Backend menerima data pendaftaran dan melakukan hit ke layanan *dummy* pihak ketiga (Vida untuk KTP/Face Score dan Pefindo untuk Credit Score).

## 3. Reference Entities (Schema: los)
Gunakan struktur JPA Entity berikut sebagai standar untuk integrasi layanan pihak ketiga:

**A. Vida Entity (`dbh_vida`)**
*   Tabel menyimpan hasil verifikasi KYC dan skor wajah.
*   Kolom: `id`, `user_id` (ManyToOne), `kyc_status`, `kyc_face_score`, `kyc_vendor_trx_id`, `income_verification_status`, `income_verification_source`, `income_is_employed`, `income_verified_monthly`, `raw_response`, `createdAt`.

**B. Pefindo Inquiry Entity (`dbh_pefindo_inquiries`)**
*   Tabel menyimpan hasil skor kredit dari Pefindo.
*   Kolom: `id`, `score`, `pdf_path_file`, `raw_response`, `createdAt`.

## 4. Engine Scoring & Plafond Rules
Setelah data registrasi dan foto KTP masuk, backend akan menjalankan *Engine Scoring* berdasarkan integrasi *dummy* Vida dan Pefindo. Gunakan logika *Decision Engine* berikut untuk menentukan Plafond:

### A. Validasi Vida (KYC & KTP)
*   **KTP/KYC Tidak Jelas (Unclear):** Lempar *response error* atau status `RETAKE_PHOTO`. Minta *front-end* untuk menyuruh *user* foto ulang.
*   **Vida Buruk (Rejected):** *Reject* pengajuan secara langsung. Kembalikan *user* ke halaman awal (Tidak mendapat *plafond*).
*   **Vida Bagus (Approved):** Lanjut ke evaluasi Pefindo.

### B. Evaluasi Pefindo (Jika Vida Bagus)
Penentuan *Plafond* (Limit Kredit) didasarkan pada tingkat Kolektibilitas (COL) dan jangka waktu (umur historis COL):
1.  **Pefindo Bagus (Lancar / Tidak ada tunggakan):** 
    *   Plafond = Rp 20.000.000
2.  **Pefindo Medium (Kasus 1):** 
    *   Syarat: Ada riwayat COL (maksimal COL 4) DAN riwayat tersebut sudah lebih dari 2 tahun lalu.
    *   Plafond Maksimal = Rp 10.000.000
3.  **Pefindo Medium (Kasus 2):** 
    *   Syarat: Ada riwayat COL di bawah 3 (COL 1 atau 2) DAN riwayat tersebut belum mencapai 2 tahun.
    *   Plafond = Rp 10.000.000
4.  **Pefindo High Risk / Macet (Kasus 3):** 
    *   Syarat: Ada riwayat COL 5 (Macet) TETAPI riwayat tersebut sudah berlalu lebih dari 2 tahun.
    *   Plafond = Rp 5.000.000

## 5. Review & Approval Flow (Backoffice / Webadmin)
Setelah *Engine Scoring* selesai dan memberikan rekomendasi limit (plafond), status pengajuan masuk ke antrean operasional internal.

### A. Tahap 1: Checker (Review)
*   **Tugas Checker:** Mengecek validitas foto KTP, serta menganalisa hasil *raw/score* dari Vida dan Pefindo (sistem akan menampilkan visual *score* sesuai desain Figma).
*   **Aksi Checker:** Menentukan/menyetujui *plafond* hasil analisa awal. Jika disetujui, pengajuan diteruskan ke antrean BM (Branch Manager).
*   **Penolakan:** Jika Checker me-*reject* pengajuan, proses berhenti dan status langsung menjadi `REJECTED`.

### B. Tahap 2: BM (Approval)
*   **Tugas BM:** Melakukan evaluasi final terhadap pengajuan yang sudah di-*review* oleh Checker.
*   **Aksi BM:** BM berhak melakukan *Approve* sesuai rekomendasi Checker, atau melakukan *Adjust* (penyesuaian/penurunan) nominal plafond sebelum di-*approve*.
*   **Penolakan:** Jika BM me-*reject* pengajuan, status menjadi `REJECTED`.

### C. Notifikasi (Notification Engine)
*   Jika pengajuan di-*reject* (baik oleh Checker maupun BM), sistem otomatis mengirimkan notifikasi penolakan ke aplikasi Android Nasabah.
*   Jika di-*approve* oleh BM, sistem mengirimkan notifikasi persetujuan beserta nilai *plafond* final yang didapatkan.