package com.mhyddnn.mubarokappmobile.feature.plan.ui

import com.google.gson.Gson
import com.mhyddnn.mubarokappmobile.feature.plan.data.toRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanFormTest {

    @Test
    fun toNewPlan_missingRequired_errorsUseBackendPaths() {
        val (plan, errors) = PlanFormUiState(kasbon = listOf(KasbonRow())).toNewPlan()
        assertNull(plan)
        assertEquals(
            setOf(
                "tgl_pergi_kanvas", "market_area_id", "vehicle_id",
                "salesKanvasPlanPersonil.0.employee_id", "salesKanvasPlanDetail.0.product_id",
                "salesKanvasKasbonMaterial.0.kasbon_material_type_id", "salesKanvasKasbonMaterial.0.price",
            ),
            errors.keys,
        )
    }

    @Test
    fun toNewPlan_complete_serializesToBackendBody() {
        val state = PlanFormUiState(
            invoiceDate = "2026-10-25",
            departDate = "2026-10-26",
            marketAreaId = 1,
            vehicleId = 2,
            note = "  ",
            personnel = listOf(PersonnelRow(employeeId = 3, position = "Sopir")),
            products = listOf(ProductRow(productId = 5, qty = 10)),
            kasbon = listOf(KasbonRow(typeId = 2, price = 50_000, qty = 1)),
        )
        val (plan, errors) = state.toNewPlan()
        assertEquals(emptyMap<String, String>(), errors)
        assertEquals(
            """{"tgl_invoice":"2026-10-25","tgl_pergi_kanvas":"2026-10-26","market_area_id":1,"vehicle_id":2,""" +
                """"salesKanvasPlanPersonil":[{"employee_id":3,"position":"Sopir"}],""" +
                """"salesKanvasPlanDetail":[{"product_id":5,"qty":10}],""" +
                """"salesKanvasKasbonMaterial":[{"kasbon_material_type_id":2,"price":50000,"qty":1}]}""",
            Gson().toJson(plan!!.toRequest()),
        )
    }
}
