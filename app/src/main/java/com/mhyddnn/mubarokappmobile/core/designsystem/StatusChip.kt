package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import com.mhyddnn.mubarokappmobile.ui.theme.DarkStatusPalette
import com.mhyddnn.mubarokappmobile.ui.theme.LightStatusPalette
import com.mhyddnn.mubarokappmobile.ui.theme.StatusToneColors

/**
 * Warna untuk [StatusChip]. Domain-agnostik: layer fitur yang memetakan
 * status (mis. plan DISETUJUI, transaksi KREDIT) ke salah satu nada ini.
 */
enum class StatusTone {
    Neutral,
    Info,
    Success,
    Warning,
    Danger,
}

/** Resolusi warna nada dari palet terpusat (lihat theme), menyesuaikan light/dark. */
@Composable
internal fun StatusTone.colors(): StatusToneColors {
    val isDark = AppTheme.colors.surface.luminance() < 0.5f
    val palette = if (isDark) DarkStatusPalette else LightStatusPalette
    return when (this) {
        StatusTone.Neutral -> palette.neutral
        StatusTone.Info -> palette.info
        StatusTone.Success -> palette.success
        StatusTone.Warning -> palette.warning
        StatusTone.Danger -> palette.danger
    }
}

/**
 * Spesifikasi chip murni (tanpa Compose) agar mapper domain di layer fitur bisa mengembalikannya
 * dan mudah diuji. Render lewat overload [StatusChip] yang menerima [ChipSpec].
 */
data class ChipSpec(
    val text: String,
    val tone: StatusTone = StatusTone.Neutral,
)

/** Render [ChipSpec] hasil mapper domain. */
@Composable
fun StatusChip(spec: ChipSpec, modifier: Modifier = Modifier) =
    StatusChip(text = spec.text, modifier = modifier, tone = spec.tone)

/**
 * Badge berwarna kecil untuk menandai status/kategori (status plan, tipe transaksi, group sale).
 * Dipakai di kartu list maupun layar detail.
 */
@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Neutral,
) {
    val (container, content) = tone.colors()
    AppText(
        text = text,
        modifier = modifier
            .surface(color = container, shape = RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = AppTheme.typography.labelSmall,
        color = content,
    )
}
