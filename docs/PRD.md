# Product Requirement Document
**KLarFinance**
Website and Android Platform
Version 1.0.0
Created at 13 Agustus, 2026

**BCAF ITDP Bootcamp 2026**

| Role | Name |
|---|---|
| Target Release | |
| Driver | |
| Approver | |
| Contributors | |
| Informed | |

## Document Status

| Document Version | Date | Version | Document Changes | Created By | Approved By |
|---|---|---|---|---|---|
| | | | | | |
| | | | | | |
| | | | | | |

## Introduction
KlarFinance adalah sebuah platform digital yang memungkinkan pengguna untuk mengakses layanan keuangan dan pinjaman secara terintegrasi, transparan, serta mudah dikelola dalam satu aplikasi.

### Latar Belakang Produk
Saat ini, pasar penyedia pinjaman online (pinjol) dan fintech sangat terfragmentasi dan rumit. Mayoritas aplikasi yang ada di pasaran memiliki fungsi yang sangat terbatas dan kaku—biasanya hanya fokus pada satu fitur utama, seperti opsi pembayaran belanja (Pay Later), transfer dana, atau sekadar pinjaman tunai tanpa fitur pengelolaan uang yang memadai. Layanan yang ada sering kali membingungkan pengguna dengan struktur bunga yang tersembunyi, biaya administrasi yang tidak jelas, serta alur pelacakan pengembalian yang rumit.

Aplikasi ini hadir untuk menjawab frustrasi tersebut dengan menyasar masyarakat modern yang membutuhkan kenyamanan, kemudahan akses, dan transparansi penuh. Kami mengintegrasikan seluruh kebutuhan transaksi dan pembiayaan dalam satu ekosistem yang intuitif, di mana setiap alur keluar-masuk uang dan perhitungan bunga dapat dipantau secara real time.

### Keuntungan Utama Produk
* **Transparansi Tanpa Biaya Tersembunyi:** Seluruh simulasi pinjaman, rincian suku bunga, dan tanggal jatuh tempo ditampilkan secara jujur dan mudah dipahami sejak awal tanpa ada kejutan biaya di kemudian hari.
* **Kemudahan dan Kenyamanan dalam Satu Platform:** Menggabungkan fleksibilitas pembayaran bulanan, transfer dana cepat, pembayaran qris, pemesanan tiket bus TJ dan Kereta, sampai pengelolaan pinjaman tanpa perlu berpindah-pindah antar-aplikasi.

## Tujuan & Success Metrics
Tujuan utama dari produk ini adalah menghadirkan satu platform finansial terpadu yang menyederhanakan akses dana tunai dan transaksi harian (Pay Later) melalui transparansi penuh atas kalkulasi bunga dan arus kas.

Dalam hal ini, pengguna diharapkan dapat:
1. Memahami secara akurat seluruh struktur biaya, suku bunga, dan jadwal pelunasan tanpa adanya hidden fees.
2. Mengakses solusi pinjaman tunai dan pembiayaan transaksi harian hanya dalam satu platform terpadu.
3. Melacak posisi arus kas dan sisa kewajiban pinjaman secara real-time dan intuitif.

### Metrik Keberhasilan (Success Metrics)
| Metrik | Deskripsi | Target/Indikator Kunci |
|---|---|---|
| User Activation Rate | Persentase pengguna terdaftar yang menyelesaikan verifikasi (KYC) dan melakukan transaksi/pinjaman pertama. | Mengukur seberapa intuitif alur onboarding aplikasi. |
| Transaction Completion Rate | Rasio keberhasilan alur transaksi (pinjaman tunai / Pay Later) dari pengajuan hingga dana cair atau pembayaran terverifikasi. | Indikator keandalan alur UX dan stabilitas sistem. |
| Retention Rate (30/60/90 Days) | Persentase pengguna yang kembali melakukan transaksi atau pengecekan alokasi dana secara berkala. | Menunjukkan tingkat kebergunaan (utility) produk bagi pengguna. |
| Transaction Error & Drop-off Rate | Persentase kegagalan transaksi teknis dan pengguna yang keluar di tengah proses pengajuan. | Menandai area friction dalam aplikasi yang perlu diperbaiki. |
| CSAT & Transparency Index | Skor kepuasan pengguna terhadap kejelasan informasi simulasi bunga dan pelacakan dana. | Menilai apakah transparansi yang ditawarkan berhasil membangun rasa percaya pengguna. |
| NPL (Non-Performing Loan) Rate | Persentase kredit macet di atas 90 hari (TWP90). | Menjaga keseimbangan antara kemudahan akses dan kualitas manajemen risiko kredit. |

## Scope/Assumption

### A. In-Scope (Fitur Masuk dalam MVP)
**1. Mobile App (Android - End User)**
*   **Autentikasi & Akun:** Penginputan / Pendaftaran Akun (Register User), Masuk Akun (Login User), Pemulihan Akses (Lupa / Reset Kata Sandi), Fitur Penelusuran Awal (Masuk Homepage Tanpa Login), Profil Pengguna (Profil User).
*   **Fitur Pinjaman & Informasi:** Penayangan daftar limit kredit (Daftar Plafond), Formulir Pengajuan Pinjaman Dana/Pembiayaan (Pengajuan Pinjaman), Pemantauan status persetujuan (Status Pengajuan Pinjaman), Fitur Pemberitahuan (Notifikasi / Push Notification).

**2. Web Frontend / Dashboard (Superadmin & Internal Admin)**
*   **Halaman Depan:** Landing Page informasi produk/aplikasi, Dashboard Analytics (Data Umum Dashboard / Metrik Transaksi Utama).
*   **Modul Web Superadmin & Pengelolaan Akses (RBAC):**
    *   **Master Data User (RBAC):** Pengelolaan akun internal/admin dan penugasan peran.
    *   **Master Data Role (RBAC):** Pembuatan, pembaruan, dan hapus Role/Hak Akses internal secara dinamis.
    *   **Master Data Menu (RBAC):** Penambahan, pengaturan hierarki, dan konfigurasi navigasi menu aplikasi.
    *   **Master Data Access (RBAC):** Pemetaan izin akses (Create, Read, Update, Delete) per role terhadap menu.
    *   **Manajemen Akun & Reset Password:** Fitur bagi Superadmin untuk melakukan pengubahan/reset password akun pengguna maupun admin internal.
*   **Modul Operasional Pinjaman (Admin Operational/Reviewer):** Daftar antrean pengajuan (List Request Pinjaman), Penilaian & Pengesahan (Review & Approval Pinjaman), Eksekusi Pencairan Dana (Pencairan Pinjaman).

**3. Backend Service & Architecture**
*   Layanan Manajemen Akun: Register, Login, Lupa/Reset Password, dan Profil.
*   Engine RBAC (Role-Based Access Control) Dinamis untuk pengelolaan hak akses menu dan API.
*   Modul Akses Terbuka (Public Homepage Access tanpa sesi login).
*   Notification Engine (Integrasi Layanan Notifikasi Email/Push Notif).
*   Engine Manajemen Kredit: Daftar Plafond, Pengajuan Pinjaman, Review Pinjaman, Approval Pinjaman, dan Pencairan Pinjaman.
*   Pembuatan & Publikasi Dokumentasi API terintegrasi (API Documentation / Swagger).

### B. Out-of-Scope (Tidak Masuk dalam MVP)
*   **Integrasi API KYC Pihak Ketiga:** Integrasi otomatis dengan penyedia verifikasi identitas / E-KYC eksternal (proses pengecekan dan validasi dokumen pengguna pada MVP masih dilakukan secara manual/internal).
*   **Integrasi API Vida (Credit Scoring):** Integrasi sistem penilaian kelayakan kredit otomatis menggunakan API Vida maupun credit scoring engine berbasis AI/ML eksternal (penilaian approval pinjaman pada MVP sepenuhnya diproses melalui review manual oleh Admin).
*   **Aplikasi iOS:** Pengembangan versi iOS (Apple Store) ditunda dan difokuskan pada platform Android & Web Dashboard terlebih dahulu.
*   **Payment Gateway Automatic Disbursement:** Integrasi otomatis disbursement / pencairan dana instan dan skema Pay Later merchant otomatis pihak ketiga.

## Problem Statement & Empathy Map

### Problem Statement
Masyarakat urban dan pekerja muda saat ini sering kali menghadapi kebutuhan finansial yang dinamis, baik untuk transaksi pembiayaan harian (Pay Later) maupun kebutuhan dana tunai mendesak. Namun, ekosistem layanan finansial digital yang ada saat ini masih sangat terfragmentasi dan membingungkan:
*   **Fragmentasi Layanan:** Pengguna terpaksa mengunduh dan mengelola banyak aplikasi terpisah untuk kebutuhan yang berbeda—satu aplikasi khusus Pay Later, satu aplikasi untuk transfer uang, dan aplikasi lain untuk pinjaman tunai.
*   **Kurangnya Transparansi Bunga & Biaya:** Mayoritas platform Online Lending (Pinjaman Online) menyembunyikan rincian kalkulasi biaya layanan, potongan awal, atau skema bunga harian. Hal ini menyulitkan pengguna untuk memantau ke mana uang mereka mengalir dan berapa persisnya kewajiban pelunasan yang harus dibayar.
*   **Pengalaman Pengguna yang Rumit:** Tidak tersedianya pelacakan arus kas (money tracking) secara real-time di dalam platform pinjaman membuat pengguna rentan mengalami kemacetan pembayaran akibat minimnya visibilitas finansial.

### Empathy Map
[Silakan disiapkan]

## Research Questions, User Persona & Customer Journey Map

### Research Objective
Memahami perilaku, pola transaksi, serta kendala utama yang dihadapi oleh pekerja kantoran dan masyarakat perkotaan dalam mengakses pinjaman tunai maupun pembiayaan transaksi harian, guna merancang platform finansial terpadu yang cepat, simpel, transparan, dan dapat dilacak dengan mudah.

### Research Questions
1.  **Perilaku Penggunaan Layanan Finansial:**
    *   Bagaimana pekerja kantoran di area perkotaan mengelola kebutuhan dana tunai mendesak dan pengeluaran harian mereka saat ini?
    *   Berapa banyak aplikasi finansial (Pinjol/Pay Later) yang aktif mereka gunakan secara bersamaan, dan apa alasannya?
2.  **Kendala & Poin Masalah (Pain Points):**
    *   Apa saja kendala utama yang sering memicu rasa frustrasi saat pekerja kantoran mengajukan pinjaman atau menggunakan fitur Pay Later?
    *   Sejauh mana kurangnya transparansi bunga dan biaya tersembunyi memengaruhi keputusan mereka dalam memilih platform pinjaman?
3.  **Kebutuhan & Ekpektasi Solusi:**
    *   Fitur pelacakan uang (money/interest tracking) seperti apa yang paling dibutuhkan oleh pekerja perkotaan agar merasa aman dan terkontrol secara finansial?
    *   Elemen desain antarmuka (UI/UX) seperti apa yang dikategorikan "simpel dan cepat" bagi orang kantoran dengan ritme kerja yang padat?

### User Persona
[Silakan disiapkan]

### Customer Journey Map
[Silakan disiapkan]

## User Stories

| Feature | User Story | User Acceptance Criteria | Definition of Done | Priority |
|---|---|---|---|---|
| Autentikasi | As a Nasabah, I want to login/register pakai nomor HP dan login menggunakan fingerprint, so saya bisa akses layanan dengan aman. | 1. Terdapat form input login dan registrasi.<br>2. Ada validasi format nomor HP.<br>3. Ada penggunaan fingerprint | API Auth terintegrasi, UI responsif, lulus testing fungsional tanpa error blocker. | High |
| Plafond & Simulasi | As a Nasabah, I want to melihat daftar plafond dan kalkulasi bunga transparan, so that saya tahu kewajiban pelunasan tanpa hidden fees. | 1. Tampil pilihan nominal pinjaman.<br>2. Tampil simulasi total pengembalian dan bunga harian/bulanan.<br>3. Rincian biaya admin (jika ada) tertera jelas | Kalkulasi matematis sesuai logika backend, data ditarik dari master plafond, UI sesuai desain. | High |
| Pengajuan Pinjaman | As a Nasabah, I want to mengisi form pengajuan dengan cepat, so that kebutuhan dana saya segera diproses. | 1. Form input data diri, KTP, dan rekening bank berfungsi.<br>2. Klik 'Submit' memunculkan notifikasi sukses.<br>3. Status berubah menjadi Submitted/Draft. | Data tersimpan di database, status ter-update, dan data masuk ke antrean Checker di backend. | High |
| Tracking Status | As a Nasabah, I want to memantau status pengajuan secara real-time, so that saya tahu proses pinjaman saya sampai mana. | 1. Halaman beranda menampilkan status terkini (Verifikasi/Disetujui/Cair).<br>2. Menerima Push Notification saat status berubah. | Integrasi Push Notification (misal: Firebase) sukses, perubahan status terefleksi maksimal delay 2 detik. | High |
| Pinjaman Menggunakan QRIS | As a Nasabah, saya ingin membayar bayar dengan paylater | 1. Halaman Pembayaran menggunakan QRIS<br>2. Dihalaman perlu diberikan informasi mengenai biaya admin | API Auth terintegrasi, UI responsif, lulus testing fungsional tanpa error blocker. | High |
| User Greeting & Profile | As a Nasabah, I want to melihat sapaan waktu dan mengakses profil saya via ikon di pojok kanan atas, so that saya tahu saya login di akun yang benar. | 1. Menampilkan teks sapaan (contoh: "Good Morning") sesuai jam lokal.<br>2. Klik ikon profil mengarah ke halaman Akun/Profil. | UI ter-render sempurna, fungsi routing ke halaman profil berjalan tanpa delay. | High |
| Tombol Ajukan Pinjaman | As a Nasabah, I want to bisa menekan tombol "Ajukan Pinjaman" langsung dari halaman utama, so that saya bisa memulai proses pinjam dana dengan cepat. | 1. Tombol "Ajukan Pinjaman" terlihat jelas di dalam kartu Plafond.<br>2. Saat diklik, user diarahkan ke form Pengajuan Pinjaman. | Tombol responsif (clickable), routing ke form pengajuan berjalan lancar. | High |

## Product Requirements

### 7.1. User flowchart
*(Merujuk pada gambar flowchart di dokumen asli)*

### 7.2. Wireframe/Prototype Links
Link to HiFi design & prototype : https://www.figma.com/design/alZQDwBcyLe8VQyCIwOt38/KlarFinance?t=9ApR5LSXPHKE6hqR-1

### 7.3. Product Roadmap
Link to Product Roadmap : link product

## Sign Off

| Team | Name | Sign Off (Y/N) |
|---|---|---|
| UIUX | Silakan diisi | Y |
| Product Manager | Silakan diisi | Y |

## Dokumentasi UX riset
[Silakan disiapkan]
Link to UX research documentation :

## Future Improvement Plan
[Silakan disiapkan]