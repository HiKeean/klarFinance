package com.klarfinance.app.core.scan

import android.content.Context
import android.content.pm.PackageManager

/**
 * Device app scoring (BRD/lending-flow.md - referensi pinjol OJK) - dipakai di DUA tempat:
 * registrasi (EngineScoringDomain.applyPinjolAdjustment) dan pengajuan pinjaman >30% dari
 * plafond (LoanReviewRequest, backend LoanInterestPolicy.exceedsReviewThreshold). Sebelum ini
 * ditambahkan, field `pinjolApps`/`bankApps` di request manapun SELALU kosong - app Kotlin ini
 * tidak pernah benar-benar men-scan apapun.
 *
 * PENTING - package name di [PINJOL_PACKAGES]/[BANK_PACKAGES] adalah BEST-EFFORT dari
 * pengetahuan training, BELUM DIVERIFIKASI ke Play Store asli (sesi ini tidak punya akses
 * internet). Sebelum rilis produksi, verifikasi ulang tiap nama package terhadap APK/listing
 * Play Store yang sebenarnya - beberapa kemungkinan sudah berubah atau tidak akurat.
 *
 * Pakai `<queries>` di AndroidManifest.xml (bukan `QUERY_ALL_PACKAGES`) - permission itu kena
 * scrutiny lebih ketat di Play Store review untuk app kategori fintech konsumen biasa (App
 * mirip alasan project ini menghindari `SCHEDULE_EXACT_ALARM` untuk LocationScheduler, lihat
 * kotlin-location-capture-worker.md).
 */
object InstalledAppsScanner {

    private val PINJOL_PACKAGES = mapOf(
        "com.kredivo.app" to "Kredivo",
        "com.akulaku.app" to "Akulaku",
        "com.adakami.id" to "AdaKami",
        "com.kredit.pintar" to "Kredit Pintar",
        "com.julofinance.juloapp" to "JULO",
        "cash.easy.id" to "Easycash",
        "com.indodana.app" to "Indodana",
        "com.rupiahcepat.app" to "RupiahCepat",
        "com.pinjamango.app" to "PinjamanGo",
        "com.maucash.app" to "Maucash",
        "id.uangme.app" to "UangMe",
    )

    private val BANK_PACKAGES = mapOf(
        "com.bca.mobile" to "BCA Mobile",
        "id.co.bri.brimo" to "BRImo",
        "id.co.bankmandiri.livin" to "Livin by Mandiri",
        "src.com.bni" to "BNI Mobile Banking",
        "com.btpn.dc" to "Jenius",
        "com.bankpermata.mobile" to "PermataMobile",
    )

    data class ScanResult(val pinjolApps: List<String>, val bankApps: List<String>)

    /** Cuma cek ADA/TIDAKnya tiap package di [PINJOL_PACKAGES]/[BANK_PACKAGES] lewat
     * `getPackageInfo` (butuh `<queries>` manifest supaya visible di Android 11+/API 30+) -
     * tidak butuh permission runtime apapun, jadi bisa dipanggil langsung tanpa dialog izin. */
    fun scan(context: Context): ScanResult {
        val packageManager = context.packageManager
        return ScanResult(
            pinjolApps = detectInstalled(packageManager, PINJOL_PACKAGES),
            bankApps = detectInstalled(packageManager, BANK_PACKAGES),
        )
    }

    private fun detectInstalled(packageManager: PackageManager, candidates: Map<String, String>): List<String> {
        return candidates.mapNotNull { (packageName, label) ->
            try {
                packageManager.getPackageInfo(packageName, 0)
                label
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
    }
}
