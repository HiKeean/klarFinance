package com.klarfinance.app.presentation.loan

/** Same grouping approach as HomeScreen.formatRupiah - kept as a small local duplicate rather
 * than a shared core util, consistent with how this codebase treats trivial one-off formatters. */
fun formatRupiah(amount: Long): String {
    val grouped = amount.toString().reversed().chunked(3).joinToString(".").reversed()
    return "Rp $grouped"
}

/** Kompak buat kartu preset kecil (referensi desain: "Rp16.0k") - >=1jt dibulatkan 1 desimal
 * jutaan ("Rp1.5jt"), sisanya ribuan ("Rp250rb"). */
fun formatRupiahCompact(amount: Long): String = when {
    amount >= 1_000_000 -> "Rp${"%.1f".format(amount / 1_000_000.0)}jt"
    amount >= 1_000 -> "Rp${amount / 1_000}rb"
    else -> "Rp$amount"
}
