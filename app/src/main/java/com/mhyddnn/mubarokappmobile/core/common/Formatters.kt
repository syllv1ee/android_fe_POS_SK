package com.mhyddnn.mubarokappmobile.core.common

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Helper format khusus layer UI. Logika/penyimpanan tetap memakai angka rupiah mentah
 * dan tanggal string `YYYY-MM-DD` (sesuai kontrak backend di doc.md).
 */
object Formatters {

    private val rupiah: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
            maximumFractionDigits = 0
        }

    /** API mengirim tanggal sebagai string `YYYY-MM-DD`. */
    val apiDate: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val displayDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("in", "ID"))

    /** 983000 -> "Rp 983.000". Menerima Long agar aman dari pembulatan. */
    fun rupiah(amount: Long): String = rupiah.format(amount)

    /** "983000" -> "Rp 983.000"; nilai non-numerik dikembalikan apa adanya. */
    fun rupiah(amount: String): String =
        amount.toLongOrNull()?.let(::rupiah) ?: amount

    /** "2026-06-12" -> "12 Jun 2026". */
    fun displayDate(isoDate: String): String =
        runCatching { LocalDate.parse(isoDate, apiDate).format(displayDate) }
            .getOrDefault(isoDate)

    /**
     * "2026-06-02T00:00:00.000Z" -> "02 Jun 2026". Menerima tanggal ISO maupun date-time
     * (mengambil bagian tanggalnya saja). Aman juga untuk input `YYYY-MM-DD`.
     */
    fun displayIsoDate(iso: String): String = displayDate(iso.take(10))

    /** LocalDate -> "2026-06-12" untuk dikirim ke API. */
    fun toApiDate(date: LocalDate): String = date.format(apiDate)
}
