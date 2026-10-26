package com.mhyddnn.mubarokappmobile.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhyddnn.mubarokappmobile.core.datastore.UserSession
import com.mhyddnn.mubarokappmobile.core.datastore.has
import com.mhyddnn.mubarokappmobile.core.sync.OutboxStatus
import com.mhyddnn.mubarokappmobile.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Ringkasan singkat untuk kartu di dashboard (build ini tidak hit data apa pun). */
data class DashboardSummary(
    val tripCount: Int? = null,
    val saleCount: Int? = null,
)

/**
 * State Beranda: sesi, menu terfilter permission, dan status sinkron. Semua turunan Flow sesi/outbox,
 * tidak ada request jaringan. `WhileSubscribed(5_000)`: berhenti mengamati 5 dtk setelah layar
 * tak terlihat, tapi selamat dari rotasi layar tanpa memulai ulang.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    outboxStatus: OutboxStatus,
) : ViewModel() {

    /** null sesaat di awal (sebelum DataStore terbaca), lalu sesi aktif. */
    val session: StateFlow<UserSession?> = authRepository.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Entri [dashboardMenus] yang boleh tampil untuk sesi saat ini ([has], hanya untuk tampilan —
     * server tetap memeriksa izin). Kosong selama sesi belum terbaca.
     */
    val menus: StateFlow<List<DashboardMenu>> = authRepository.session
        .map { session ->
            dashboardMenus.filter { menu ->
                menu.requiredPermission == null || session?.has(menu.requiredPermission) == true
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Token tak bisa diperbarui → tampilkan ajakan login ulang (app tetap bisa dipakai). */
    val needsRelogin: StateFlow<Boolean> = authRepository.needsRelogin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Data buatan HP yang belum terkirim ke server (semua fitur, termasuk yang FAILED). */
    val unsyncedCount: StateFlow<Int> = outboxStatus.unsyncedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Statis — build ini tidak punya modul lain untuk dihitung. */
    val summary: StateFlow<DashboardSummary> = MutableStateFlow(DashboardSummary())
}
