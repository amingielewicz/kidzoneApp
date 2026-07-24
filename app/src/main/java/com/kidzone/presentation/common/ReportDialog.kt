@file:Suppress("LongParameterList", "LongMethod", "FunctionNaming")

package com.kidzone.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kidzone.R

/**
 * 🎯 Odpowiedzialności:
 * - Ustandaryzowany dialog zgłaszania naruszeń (miejsca, opinie, zdjęcia).
 */
@Composable
fun KidZoneReportDialog(
    title: String,
    reasons: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, comment: String) -> Unit,
    isOffline: Boolean,
    modifier: Modifier = Modifier,
    initialReason: String = reasons.firstOrNull()?.first.orEmpty(),
    description: String? = null
) {
    var selectedReason by remember { mutableStateOf(initialReason) }
    var comment by remember { mutableStateOf("") }

    KidZoneActionDialog(
        title = title,
        icon = Icons.Filled.Flag,
        iconTint = MaterialTheme.colorScheme.error,
        onDismiss = onDismiss,
        modifier = modifier,
        confirmButton = {
            OfflineAwareSubmitButton(
                label = stringResource(R.string.report_submit),
                onClick = { onSubmit(selectedReason, comment.trim()) },
                isOffline = isOffline
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
            }
            
            Text(
                text = stringResource(R.string.report_choose_reason),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            
            reasons.forEach { (code, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedReason = code }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedReason == code,
                        onClick = { selectedReason = code }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text(stringResource(R.string.report_comment_label)) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
