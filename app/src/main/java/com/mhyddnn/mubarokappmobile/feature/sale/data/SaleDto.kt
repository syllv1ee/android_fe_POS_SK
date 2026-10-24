package com.mhyddnn.mubarokappmobile.feature.sale.data

import com.google.gson.annotations.SerializedName
import com.mhyddnn.mubarokappmobile.feature.sale.domain.Sale

/**
 * Item `GET /api/sales-kanvas/sale`. Hanya field yang dipakai layar daftar — relasi detail
 * (customer, produk, retur) lihat `SaleResponseDto` di proyek asal MubarokApp. Nilai uang
 * dikirim STRING (bisa berdesimal, mis. "50000.00"); diparse ke Long di mapper.
 */
data class SaleDto(
    val id: Int,
    val invoice: String,
    @SerializedName("type_transaction") val typeTransaction: String,
    @SerializedName("group_sale_id") val groupSaleId: Int,
    @SerializedName("tgl_invoice") val tglInvoice: String? = null,
    val total: String? = null,
    val note: String? = null,
    val salesKanvasPayment: List<SalePaymentDto> = emptyList(),
)

data class SalePaymentDto(
    @SerializedName("payment_remaining") val paymentRemaining: String? = null,
)

internal fun String?.toMoney(): Long = this?.toBigDecimalOrNull()?.toLong() ?: 0L

fun SaleDto.toDomain(): Sale = Sale(
    id = id,
    invoice = invoice,
    typeTransaction = typeTransaction,
    groupSaleId = groupSaleId,
    total = total.toMoney(),
    note = note,
    invoiceDate = tglInvoice,
    paymentRemaining = salesKanvasPayment.sumOf { it.paymentRemaining.toMoney() },
)
