package com.mhyddnn.mubarokappmobile.feature.sale.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SaleDtoTest {

    @Test
    fun toDomain_parsesMoneyStringsAndSumsRemaining() {
        val sale = SaleDto(
            id = 1,
            invoice = "INV-1",
            typeTransaction = "KREDIT",
            groupSaleId = 1,
            total = "1000000.00",
            salesKanvasPayment = listOf(SalePaymentDto("250000"), SalePaymentDto(null), SalePaymentDto("50000.50")),
        ).toDomain()
        assertEquals(1_000_000L, sale.total)
        assertEquals(300_000L, sale.paymentRemaining)
    }

    @Test
    fun toDomain_invalidMoneyIsZero() {
        assertEquals(0L, SaleDto(1, "INV-1", "TUNAI", 1, total = "abc").toDomain().total)
    }
}
