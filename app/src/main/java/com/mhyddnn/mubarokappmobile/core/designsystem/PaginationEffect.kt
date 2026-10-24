package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Memicu [onLoadMore] saat scroll mendekati ujung daftar ([buffer] item sebelum akhir). Reusable
 * untuk infinite-scroll pagination pada `LazyColumn` mana pun. Pemanggil yang men-debounce lewat
 * state (mis. abaikan bila sedang memuat / sudah halaman terakhir).
 */
@Composable
fun PaginationEffect(
    listState: LazyListState,
    buffer: Int = 3,
    onLoadMore: () -> Unit,
) {
    val shouldLoadMore by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && lastVisible >= total - 1 - buffer
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .collect { reached -> if (reached) onLoadMore() }
    }
}
