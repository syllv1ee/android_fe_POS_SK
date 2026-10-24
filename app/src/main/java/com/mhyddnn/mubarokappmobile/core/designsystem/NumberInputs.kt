package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import java.text.NumberFormat
import java.util.Locale

private val grouping: NumberFormat = NumberFormat.getIntegerInstance(Locale("in", "ID"))

/**
 * Input nominal rupiah. Menampilkan pemisah ribuan ("1.000.000") dan prefix "Rp",
 * namun mengeluarkan nilai [Long] mentah. Hanya menerima digit.
 */
@Composable
fun MoneyInputField(
    label: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val textStyle = AppTheme.typography.bodyLarge.copy(color = colors.onSurface)

    FieldShell(label = label, modifier = modifier.fillMaxWidth(), errorText = errorText, enabled = enabled) {
        AppText(text = "Rp ", style = textStyle, color = colors.onSurfaceVariant)
        Box(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = if (value == 0L) "" else grouping.format(value),
                onValueChange = { raw ->
                    val digits = raw.filter(Char::isDigit).take(15)
                    onValueChange(digits.toLongOrNull() ?: 0L)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}

/**
 * Input kuantitas (bilangan bulat ≥ 0). Mengeluarkan nilai [Int]; hanya menerima digit.
 */
@Composable
fun QuantityInputField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val textStyle = AppTheme.typography.bodyLarge.copy(color = colors.onSurface)

    FieldShell(label = label, modifier = modifier.fillMaxWidth(), errorText = errorText, enabled = enabled) {
        Box(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = if (value == 0) "" else value.toString(),
                onValueChange = { raw ->
                    val digits = raw.filter(Char::isDigit).take(9)
                    onValueChange(digits.toIntOrNull() ?: 0)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}
