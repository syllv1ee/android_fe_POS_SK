package com.mhyddnn.mubarokappmobile.feature.sale.data

import com.google.gson.annotations.SerializedName

/**
 * Body `POST /sales-kanvas/sale` (doc_integrasi_andorid.md §3.2). Backend `.strict()` → jangan
 * tambah field asing. Field null tidak ikut terkirim (Gson default tanpa serializeNulls).
 */
data class CreateSaleRequest(
    @SerializedName("sales_kanvas_plan_id") val salesKanvasPlanId: Int,
    @SerializedName("customer_id") val customerId: Int,
    /** "TUNAI" | "KREDIT" */
    @SerializedName("type_transaction") val typeTransaction: String,
    /** 1 Jual, 2 Sumbangan, 3 Sample */
    @SerializedName("group_sale_id") val groupSaleId: Int,
    @SerializedName("term_of_payment") val termOfPayment: Int,
    /** "yyyy-MM-dd" */
    @SerializedName("tgl_invoice") val tglInvoice: String,
    val total: Long,
    val note: String? = null,
    val salesKanvasDetail: List<CreateSaleDetail>,
    val salesKanvasPayment: List<CreateSalePayment>? = null,
    val salesKanvasRetur: CreateSaleRetur? = null,
)

data class CreateSaleDetail(
    @SerializedName("product_id") val productId: Int,
    val qty: Int,
    val price: Long,
    val batch: String? = null,
    @SerializedName("tgl_production") val tglProduction: String? = null,
    @SerializedName("tgl_expired") val tglExpired: String? = null,
)

data class CreateSalePayment(
    @SerializedName("total_discount") val totalDiscount: Long,
    @SerializedName("total_retur") val totalRetur: Long,
    val total: Long,
    @SerializedName("grand_total") val grandTotal: Long,
    /** "CASH" | "TRANSFER" | "QRIS" | "KARTU" */
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("payment_date") val paymentDate: String? = null,
    @SerializedName("amount_received") val amountReceived: Long,
    @SerializedName("payment_remaining") val paymentRemaining: Long,
    @SerializedName("bank_id") val bankId: Int? = null,
    @SerializedName("bank_type") val bankType: String? = null,
    @SerializedName("atas_nama") val atasNama: String? = null,
    @SerializedName("payment_reference") val paymentReference: String? = null,
    val note: String? = null,
)

data class CreateSaleRetur(
    @SerializedName("customer_id") val customerId: Int,
    @SerializedName("tgl_invoice") val tglInvoice: String,
    val total: Long,
    val salesKanvasReturDetail: List<CreateSaleReturDetail>,
)

data class CreateSaleReturDetail(
    @SerializedName("product_id") val productId: Int,
    val qty: Int,
    val price: Long,
    val batch: String,
    @SerializedName("retur_condition") val returCondition: String,
    @SerializedName("tgl_expired") val tglExpired: String? = null,
    val note: String? = null,
)
