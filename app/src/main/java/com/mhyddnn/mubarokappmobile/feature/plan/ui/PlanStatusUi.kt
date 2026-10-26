package com.mhyddnn.mubarokappmobile.feature.plan.ui

import com.mhyddnn.mubarokappmobile.core.designsystem.ChipSpec
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanStatus

/**
 * Status plan (raw backend) → [ChipSpec]. Case-insensitive; status baru dari BE yang belum dikenal
 * tetap tampil dengan teks aslinya (netral), tidak crash.
 */
object PlanStatusUi {
    fun status(status: String): ChipSpec = when (status.uppercase()) {
        PlanStatus.DRAFT -> ChipSpec("Draft", StatusTone.Neutral)
        PlanStatus.APPROVED -> ChipSpec("Disetujui", StatusTone.Info)
        PlanStatus.LOADING -> ChipSpec("Loading", StatusTone.Warning)
        PlanStatus.COMPLETE -> ChipSpec("Komplit", StatusTone.Success)
        PlanStatus.CANCELLED -> ChipSpec("Batal", StatusTone.Danger)
        else -> ChipSpec(status, StatusTone.Neutral)
    }
}
