package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val RefreshThreshold = 72.dp
private val MaxPullDistance = 110.dp

/**
 * Pembungkus pull-to-refresh reusable, ditulis sendiri di atas `NestedScrollConnection`
 * Compose Foundation (M3 pull-to-refresh eksperimental sudah ikut dilepas bersama Material3).
 * Status refresh dimiliki pemanggil ([isRefreshing] dari ViewModel); [onRefresh] dipicu saat
 * user menarik melewati ambang [RefreshThreshold]. Indikator otomatis tertutup saat
 * [isRefreshing] kembali false.
 */
@Composable
fun RefreshableBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { RefreshThreshold.toPx() }
    val maxPx = with(density) { MaxPullDistance.toPx() }
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // User scroll ke atas (available.y < 0) saat indikator masih terbuka → tutup dulu.
                if (available.y < 0f && offsetY.value > 0f) {
                    val consumedY = -(offsetY.value.coerceAtMost(-available.y))
                    scope.launch { offsetY.snapTo((offsetY.value + consumedY).coerceAtLeast(0f)) }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Tarik ke bawah (available.y > 0) hanya diserap saat list sudah di posisi paling
                // atas (tidak ada yang dikonsumsi list itu sendiri) dan tidak sedang refresh.
                if (available.y > 0f && !isRefreshing) {
                    scope.launch {
                        offsetY.snapTo((offsetY.value + available.y * 0.5f).coerceIn(0f, maxPx))
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offsetY.value > 0f && !isRefreshing) {
                    if (offsetY.value >= thresholdPx) {
                        offsetY.animateTo(thresholdPx)
                        onRefresh()
                    } else {
                        offsetY.animateTo(0f)
                    }
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) offsetY.animateTo(0f)
    }

    // Hanya boolean yang dibaca saat komposisi; posisi dibaca di fase layout (lambda offset)
    // → menarik layar tidak me-recompose tiap frame.
    val pulled by remember { derivedStateOf { offsetY.value > 0f } }

    Box(modifier = modifier.nestedScroll(connection)) {
        content()
        if (pulled || isRefreshing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (offsetY.value - MaxPullDistance.toPx() / 2).roundToInt()) }
                    .surface(color = AppTheme.colors.surface, shape = CircleShape, elevation = 4.dp)
                    .size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppSpinner(color = AppTheme.colors.primary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
    }
}
