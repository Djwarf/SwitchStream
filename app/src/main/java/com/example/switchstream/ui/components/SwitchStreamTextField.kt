package com.example.switchstream.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.tv.material3.MaterialTheme
import com.example.switchstream.ui.theme.AccentBlue
import com.example.switchstream.ui.theme.Divider
import com.example.switchstream.ui.theme.GlassSurface
import com.example.switchstream.ui.theme.TextPrimary
import com.example.switchstream.ui.theme.TextSecondary
import com.example.switchstream.ui.theme.TextTertiary

@Composable
fun SwitchStreamTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        modifier = modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextSecondary,
            cursorColor = AccentBlue,
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = Divider,
            focusedLabelColor = AccentBlue,
            unfocusedLabelColor = TextTertiary,
            focusedContainerColor = GlassSurface,
            unfocusedContainerColor = GlassSurface
        )
    )
}
