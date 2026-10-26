package com.mhyddnn.mubarokappmobile.feature.plan.data

import com.google.gson.Gson
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.core.sync.OutboxEntity
import com.mhyddnn.mubarokappmobile.feature.plan.domain.NewPlan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PendingPlanStatus
import com.mhyddnn.mubarokappmobile.feature.plan.domain.deliveredStatuses
import com.mhyddnn.mubarokappmobile.feature.plan.domain.displayStatus
import com.mhyddnn.mubarokappmobile.feature.plan.domain.withDelivered
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanDtoTest {

    @Test
    fun toDomain_parsesBackendJson() {
        val json = """
            {"id":12,"invoice":"SKS-1","status":"DRAFT","tgl_pergi_kanvas":"2026-10-26T00:00:00.000Z",
             "vehicle":{"name":"Truk 1","nopol":"K 1234 AB"},"marketArea":{"name":"Pati"},
             "salesKanvasPersonil":[{"position":"Sopir","employee":{"name":"Budi"}}],
             "salesKanvasPlanDetail":[{"qty":10,"product":{"code":"JNG-01","name":"Jenang"}}],
             "salesKanvasKasbonMaterial":[{"price":"50000.00","qty":2,"kasbonMaterialType":{"name":"Solar"}}]}
        """.trimIndent()
        val plan = Gson().fromJson(json, PlanDto::class.java).toDomain()
        assertEquals("Truk 1 · K 1234 AB", plan.vehicle)
        assertEquals("Budi", plan.personnel.single().name)
        assertEquals(10.0, plan.products.single().qty, 0.0)
        assertEquals(50_000L, plan.kasbon.single().price)
        assertEquals(0, plan.revision)
    }

    @Test
    fun toDomain_missingListsAreEmpty() {
        val plan = Gson().fromJson("""{"id":1,"invoice":"X","status":"DRAFT"}""", PlanDto::class.java).toDomain()
        assertTrue(plan.personnel.isEmpty() && plan.products.isEmpty() && plan.kasbon.isEmpty())
    }

    @Test
    fun pendingStatus_overridesUntilFailed() {
        val plan = Gson().fromJson("""{"id":12,"invoice":"X","status":"DRAFT"}""", PlanDto::class.java).toDomain()
        val outbox = OutboxEntity(
            id = "u1", owner = "a", kind = "plan-status", path = "plan/12/update-status",
            body = """{"status":"LOADING"}""", method = OutboxEntity.METHOD_PATCH,
        )
        val pending = outbox.toPendingPlanStatus()
        assertEquals(12, pending.planId)
        assertEquals("LOADING", plan.displayStatus(listOf(pending)))
        val failed = outbox.copy(status = OutboxEntity.STATUS_FAILED, error = "ditolak").toPendingPlanStatus()
        assertEquals("DRAFT", plan.displayStatus(listOf(failed)))
    }

    @Test
    fun deliveredStatus_keepsNewStatusAfterLeavingQueue() {
        // Bug: setelah sinkron, item antrean hilang → status kembali ke cache lama sampai refresh manual.
        val plan = Gson().fromJson("""{"id":12,"invoice":"X","status":"DRAFT"}""", PlanDto::class.java).toDomain()
        val sent = PendingPlanStatus(id = "u1", planId = 12, status = "LOADING", failedMessage = null)
        val rejected = PendingPlanStatus(id = "u2", planId = 12, status = "BATAL", failedMessage = "ditolak")

        val delivered = deliveredStatuses(before = listOf(sent, rejected), after = emptyList())
        assertEquals(listOf(sent), delivered) // yang ditolak lalu dibuang tidak dianggap terkirim
        assertEquals("LOADING", plan.withDelivered(delivered).status)
        assertEquals("DRAFT", plan.withDelivered(deliveredStatuses(listOf(sent), listOf(sent))).status)
    }

    @Test
    fun createPlan_queuesOnlyWhenFailureIsNotTheData() {
        // Sinyal / server / sesi → antrekan; isi ditolak → tampil di form untuk dibetulkan.
        listOf(null, 401, 408, 429, 500, 503).forEach { code ->
            assertTrue("kode $code", Result.Error("x", code).isTransient())
        }
        listOf(400, 403, 404, 409).forEach { code ->
            assertFalse("kode $code", Result.Error("x", code).isTransient())
        }
    }

    @Test
    fun pendingPlan_readsQueuedRequest_withLabelsFromCachedMaster() {
        val request = NewPlan(
            invoiceDate = "2026-10-25", departDate = "2026-10-26", returnDate = null,
            marketAreaId = 3, vehicleId = 7, note = "pagi",
            personnel = emptyList(), products = emptyList(), kasbon = emptyList(),
        ).toRequest()
        val outbox = OutboxEntity(id = "k1", owner = "a", kind = "plan-create", path = "plan", body = Gson().toJson(request))
        val master = PlanMasterCacheDto(
            marketAreas = listOf(MasterOptionDto(3, name = "Pati")),
            vehicles = listOf(MasterOptionDto(7, name = "Truk 1", nopol = "K 1")),
        ).toDomain()

        val pending = outbox.toPendingPlan(master)
        assertEquals("2026-10-26", pending.departDate)
        assertEquals("Pati", pending.marketArea)
        assertEquals("Truk 1 · K 1", pending.vehicle)
        assertEquals(null, pending.failedMessage)
        // Tanpa cache master tetap tampil, hanya tanpa label.
        assertEquals(null, outbox.toPendingPlan(null).marketArea)
    }
}
