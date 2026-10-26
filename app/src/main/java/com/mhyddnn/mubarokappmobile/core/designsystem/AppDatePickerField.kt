package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composeunstyled.DialogProperties
import com.composeunstyled.UnstyledDialog
import com.composeunstyled.UnstyledDialogPanel
import com.composeunstyled.UnstyledScrim
import com.composeunstyled.rememberDialogState
import com.mhyddnn.mubarokappmobile.core.common.Formatters
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val Weekdays = listOf("Mg", "Sn", "Sl", "Rb", "Km", "Jm", "Sb")
private val IdLocale = Locale("in", "ID")

/**
 * Field tanggal yang membuka kalender custom (grid bulan) dibungkus dialog Compose Unstyled.
 * Nilai keluar/masuk berupa string `YYYY-MM-DD`. Ditampilkan ke
 * user sebagai "dd MMM yyyy".
 */
@Composable
fun AppDatePickerField(
    label: String,
    /** Tanggal terpilih dalam format `YYYY-MM-DD`, atau null bila belum dipilih. */
    value: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    enabled: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }
    val colors = AppTheme.colors

    Box(modifier = modifier.fillMaxWidth()) {
        FieldShell(
            label = label,
            errorText = errorText,
            enabled = enabled,
            trailingIcon = {
                AppIcon(imageVector = Icons.Default.DateRange, contentDescription = "Pilih tanggal", tint = colors.onSurfaceVariant)
            },
        ) {
            AppText(
                text = value?.let(Formatters::displayDate) ?: "",
                style = AppTheme.typography.bodyLarge,
                color = colors.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { showDialog = true },
        )
    }

    if (showDialog) {
        val initialDate = value
            ?.let { runCatching { LocalDate.parse(it, Formatters.apiDate) }.getOrNull() }
            ?: LocalDate.now()
        var displayedMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
        var selectedDate by remember { mutableStateOf(initialDate) }
        val dialogState = rememberDialogState(initiallyVisible = true)

        fun dismiss() {
            showDialog = false
        }

        UnstyledDialog(
            state = dialogState,
            properties = DialogProperties(),
            onDismiss = ::dismiss,
        ) {
            UnstyledScrim()
            UnstyledDialogPanel(
                shape = CardShape,
                backgroundColor = colors.surface,
                contentColor = colors.onSurface,
                contentPadding = PaddingValues(16.dp),
            ) {
                Column(modifier = Modifier.width(320.dp)) {
                    MonthHeader(
                        displayedMonth = displayedMonth,
                        onPrevious = { displayedMonth = displayedMonth.minusMonths(1) },
                        onNext = { displayedMonth = displayedMonth.plusMonths(1) },
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Weekdays.forEach { day ->
                            AppText(
                                text = day,
                                modifier = Modifier.weight(1f),
                                style = AppTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    MonthGrid(
                        displayedMonth = displayedMonth,
                        selectedDate = selectedDate,
                        onSelect = { selectedDate = it },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        AppTextButton(text = "Batal", onClick = ::dismiss)
                        AppTextButton(
                            text = "OK",
                            onClick = {
                                onValueChange(Formatters.toApiDate(selectedDate))
                                dismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(displayedMonth: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppIconButton(onClick = onPrevious) {
            AppIcon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
        }
        val monthName = displayedMonth.month.getDisplayName(TextStyle.FULL, IdLocale)
        AppText(text = "$monthName ${displayedMonth.year}", style = AppTheme.typography.titleMedium)
        AppIconButton(onClick = onNext) {
            AppIcon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
        }
    }
}

@Composable
private fun MonthGrid(displayedMonth: YearMonth, selectedDate: LocalDate, onSelect: (LocalDate) -> Unit) {
    val daysInMonth = displayedMonth.lengthOfMonth()
    // Grid mulai dari Minggu: DayOfWeek ISO (Senin=1..Minggu=7) → offset Minggu=0..Sabtu=6.
    val startOffset = displayedMonth.atDay(1).dayOfWeek.value % 7
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = row * 7 + col - startOffset + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day in 1..daysInMonth) {
                            DayCell(date = displayedMonth.atDay(day), selected = displayedMonth.atDay(day) == selectedDate, onSelect = onSelect)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: Boolean, onSelect: (LocalDate) -> Unit) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .let { if (selected) it.surface(color = colors.primary, shape = CircleShape) else it }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onSelect(date) },
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = date.dayOfMonth.toString(),
            style = AppTheme.typography.bodyMedium,
            color = if (selected) colors.onPrimary else colors.onSurface,
        )
    }
}
