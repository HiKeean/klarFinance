# Business Requirements Document (BRD) - KLarFinance

**Project Name:** KLarFinance – Website and Android Platform[cite: 4]
**Initiative:** BCAF ITDP Bootcamp 2026[cite: 4]
**Document Version:** 2.0[cite: 4]
**Date:** August 2026[cite: 4]

---

## 1. Executive Summary
KLarFinance adalah sebuah inisiatif platform digital terpadu yang dirancang untuk menyediakan layanan keuangan dan pinjaman yang transparan, mudah diakses, dan terpusat dalam satu aplikasi[cite: 4]. Di tengah ekosistem fintech yang terfragmentasi, produk ini bertujuan untuk memberikan kemudahan bagi masyarakat modern dengan menyatukan layanan pinjaman tunai dan pembiayaan transaksi harian (termasuk fitur Pay Later dan QRIS) tanpa biaya yang disembunyikan[cite: 4].

## 2. Business Problem & Background
Saat ini, lanskap penyedia pinjaman online (pinjol) dan fintech memiliki beberapa kelemahan mendasar yang merugikan konsumen[cite: 4]:
*   **Fragmentasi Ekosistem:** Pengguna harus menggunakan berbagai aplikasi berbeda untuk kebutuhan yang berbeda (misalnya, satu untuk Pay Later, satu untuk pinjaman tunai, dan satu untuk transfer)[cite: 4].
*   **Kurangnya Transparansi:** Layanan yang ada di pasaran sering kali membingungkan pengguna dengan biaya administrasi yang tidak jelas dan struktur bunga yang tersembunyi[cite: 4].
*   **Minimnya Visibilitas Finansial:** Pengguna kesulitan melacak arus keluar-masuk uang dan kewajiban pelunasan secara real-time, yang sering kali berujung pada kemacetan pembayaran[cite: 4].

## 3. Business Objectives
Inisiatif pembuatan aplikasi KLarFinance ini memiliki beberapa tujuan strategis utama[cite: 4]:
*   **Integrasi Layanan:** Menghadirkan solusi pinjaman tunai dan pembiayaan transaksi (pembayaran belanja, QRIS, pemesanan tiket) di dalam satu platform yang intuitif[cite: 4].
*   **Transparansi Penuh:** Menampilkan rincian suku bunga, simulasi pinjaman, dan tanggal jatuh tempo secara jujur sejak awal, mengeliminasi praktik hidden fees[cite: 4].
*   **Pemberdayaan Finansial Pengguna:** Menyediakan fitur pelacakan arus kas dan sisa pinjaman secara real-time untuk membantu pengguna mengelola keuangan dengan lebih baik[cite: 4].

## 4. Target Market / Stakeholders
*   **Target Pengguna Utama:** Masyarakat urban dan pekerja muda di area perkotaan yang memiliki kebutuhan finansial dinamis serta ritme kerja yang padat[cite: 4].
*   **Stakeholder Internal:** Tim Operasional / Admin (untuk review dan approval manual), serta manajemen eksekutif yang memantau performa dashboard analytics[cite: 4].

## 5. High-Level Business Scope (MVP)
Untuk memastikan peluncuran yang tepat waktu, ruang lingkup inisiatif dibatasi pada fase Minimum Viable Product (MVP)[cite: 4]:

### In-Scope
*   Aplikasi Android untuk pengguna akhir (End User)[cite: 4].
*   Web Frontend / Dashboard untuk Superadmin dan operasional internal yang didukung sistem Role-Based Access Control (RBAC) dinamis[cite: 4].
*   Sistem registrasi, simulasi pinjaman transparan, pengajuan, pencairan, dan pembayaran via QRIS[cite: 4].
*   Proses review kelayakan kredit dan verifikasi pengguna (KYC) yang dilakukan secara manual oleh tim internal[cite: 4].

### Out-of-Scope
*   Pengembangan aplikasi versi iOS[cite: 4].
*   Integrasi otomatis untuk e-KYC pihak ketiga dan mesin credit scoring berbasis AI (seperti API Vida)[cite: 4].
*   Disbursement / pencairan otomatis menggunakan Payment Gateway eksternal[cite: 4].

## 6. Technical Stack
Sistem KLarFinance akan dikembangkan menggunakan teknologi berikut untuk memastikan skalabilitas dan performa tinggi[cite: 4]:
*   **Backend:** Java Spring Boot[cite: 4]
*   **Web Frontend / Dashboard:** Angular[cite: 4]
*   **Mobile (Android):** Kotlin Jetpack Compose[cite: 4]

## 7. Success Metrics & KPIs
Kesuksesan produk secara bisnis akan diukur melalui indikator-indikator berikut yang dipantau melalui dashboard operasional dan analitik[cite: 4]:

| Metrik | Deskripsi | Target / Indikator Kunci |
|---|---|---|
| **User Activation Rate** | Persentase pengguna terdaftar yang menyelesaikan verifikasi (KYC) dan melakukan transaksi/pinjaman pertama[cite: 4]. | Mengukur seberapa intuitif alur onboarding aplikasi[cite: 4]. |
| **Transaction Completion Rate** | Rasio keberhasilan alur transaksi (pinjaman tunai / Pay Later) dari pengajuan hingga dana cair atau pembayaran terverifikasi[cite: 4]. | Indikator keandalan alur UX dan stabilitas sistem[cite: 4]. |
| **Retention Rate (30/60/90 Days)** | Persentase pengguna yang kembali melakukan transaksi atau pengecekan alokasi dana secara berkala[cite: 4]. | Menunjukkan tingkat kebergunaan (utility) produk bagi pengguna[cite: 4]. |
| **Transaction Error & Drop-off Rate** | Persentase kegagalan transaksi teknis dan pengguna yang keluar di tengah proses pengajuan[cite: 4]. | Menandai area friction dalam aplikasi yang perlu diperbaiki[cite: 4]. |
| **CSAT & Transparency Index** | Skor kepuasan pengguna terhadap kejelasan informasi simulasi bunga dan pelacakan dana[cite: 4]. | Menilai apakah transparansi yang ditawarkan berhasil membangun rasa percaya pengguna[cite: 4]. |
| **NPL (Non-Performing Loan) Rate** | Persentase kredit macet di atas 90 hari (TWP90)[cite: 4]. | Menjaga keseimbangan antara kemudahan akses dan kualitas manajemen risiko kredit[cite: 4]. |