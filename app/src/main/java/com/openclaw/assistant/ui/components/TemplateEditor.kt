package com.openclaw.assistant.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

private val PLACEHOLDER_REGEX = Regex("""\{\{(\w+)\}\}""")

class PlaceholderHighlightTransformation(
    private val highlightColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        PLACEHOLDER_REGEX.findAll(text.text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

private data class PlaceholderInfo(val key: String, val description: String)

private val AVAILABLE_PLACEHOLDERS = listOf(
    PlaceholderInfo("query", "The spoken or typed user message"),
    PlaceholderInfo("session_id", "Current conversation session identifier"),
    PlaceholderInfo("local_time", "Device local time in ISO 8601 format"),
    PlaceholderInfo("stop_token", "Token the LLM uses to signal end of conversation")
)

@Composable
fun TemplatePlaceholderInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
        title = { Text("Available Placeholders") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Use these in your JSON template. Each is replaced with its value at request time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AVAILABLE_PLACEHOLDERS.forEach { p ->
                    Column {
                        Text(
                            text = "{{${p.key}}}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = p.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}
