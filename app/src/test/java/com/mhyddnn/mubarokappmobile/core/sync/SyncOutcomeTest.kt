package com.mhyddnn.mubarokappmobile.core.sync

import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.core.sync.SyncWorker.SendOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Data antrean sinkron hanya boleh dihapus saat 2xx; gangguan sementara wajib diulang, bukan dibuang. */
class SyncOutcomeTest {

    @Test
    fun outcomeOf() {
        val expected = mapOf(
            200 to SendOutcome.DONE,
            201 to SendOutcome.DONE,
            401 to SendOutcome.AUTH,
            408 to SendOutcome.RETRY,
            429 to SendOutcome.RETRY,
            500 to SendOutcome.RETRY,
            503 to SendOutcome.RETRY,
            400 to SendOutcome.REJECTED,
            403 to SendOutcome.REJECTED,
            409 to SendOutcome.REJECTED,
        )
        expected.forEach { (code, outcome) -> assertEquals("kode $code", outcome, SyncWorker.outcomeOf(code)) }
    }

    @Test
    fun isTransient_queuesOnlyWhenFailureIsNotTheData() {
        // Sinyal / server / sesi → antrekan; isi ditolak → tampil di form untuk dibetulkan.
        listOf(null, 401, 408, 429, 500, 503).forEach { code ->
            assertTrue("kode $code", Result.Error("x", code).isTransient())
        }
        listOf(400, 403, 404, 409).forEach { code ->
            assertFalse("kode $code", Result.Error("x", code).isTransient())
        }
    }
}
