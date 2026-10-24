package com.mhyddnn.mubarokappmobile.feature.sale.ui

import com.mhyddnn.mubarokappmobile.core.designsystem.ChipSpec
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone

/**
 * Pemetaan nilai domain penjualan (raw dari backend) ke [ChipSpec] untuk UI. Murni & tanpa
 * Compose agar mudah diuji. Lihat dokumen API backend untuk nilai yang mungkin.
 */
object SaleStatusUi {

    /** `type_transaction`: "TUNAI" | "KREDIT". */
    fun transaction(type: String): ChipSpec = when (type.uppercase()) {
        "TUNAI" -> ChipSpec("Tunai", StatusTone.Success)
        "KREDIT" -> ChipSpec("Kredit", StatusTone.Warning)
        else -> ChipSpec(type, StatusTone.Neutral)
    }

    /** `group_sale_id`: 1 Jual, 2 Sumbangan, 3 Sample. */
    fun groupSale(groupSaleId: Int): ChipSpec = when (groupSaleId) {
        1 -> ChipSpec("Jual", StatusTone.Info)
        2 -> ChipSpec("Sumbangan", StatusTone.Neutral)
        3 -> ChipSpec("Sample", StatusTone.Neutral)
        else -> ChipSpec("Group $groupSaleId", StatusTone.Neutral)
    }

    /**
     * Status pelunasan diturunkan dari sisa pembayaran (`payment_remaining`). Hanya relevan
     * untuk KREDIT + Jual; pemanggil yang memutuskan kapan menampilkannya.
     */
    fun paymentStatus(remaining: Long): ChipSpec =
        if (remaining <= 0L) ChipSpec("Lunas", StatusTone.Success)
        else ChipSpec("Belum lunas", StatusTone.Danger)
}
