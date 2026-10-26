package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/**
 * Field teks: label, placeholder, error message, dan dukungan single-line.
 * [errorText] non-null akan menandai field merah + menampilkan pesan di bawahnya.
 * Saat [isPassword] true, teks disembunyikan dan muncul ikon mata untuk toggle tampil/sembunyi.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val hideText = isPassword && !passwordVisible
    val colors = AppTheme.colors
    val textStyle = AppTheme.typography.bodyLarge.copy(color = colors.onSurface)

    FieldShell(
        label = label,
        modifier = modifier.fillMaxWidth(),
        errorText = errorText,
        enabled = enabled,
        trailingIcon = if (isPassword) {
            {
                Box(
                    modifier = Modifier
                        .clickable { passwordVisible = !passwordVisible }
                        .padding(4.dp),
                ) {
                    AppIcon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                        modifier = Modifier.size(20.dp),
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        } else {
            null
        },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (value.isEmpty() && placeholder != null) {
                AppText(text = placeholder, style = textStyle, color = colors.onSurfaceVariant)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = singleLine,
                textStyle = textStyle,
                cursorBrush = SolidColor(colors.primary),
                visualTransformation = if (hideText) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                ),
            )
        }
    }
}
