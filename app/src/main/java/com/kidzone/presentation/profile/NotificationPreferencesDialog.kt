package com.kidzone.presentation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class NotificationPrefs(
    val newReviewOnMyPlace: Boolean = true,
    val newBadgeEarned: Boolean = true,
    val newPhotoOnMyPlace: Boolean = true
)

@Composable
fun NotificationPreferencesDialog(
    currentPrefs: NotificationPrefs,
    onSave: (NotificationPrefs) -> Unit,
    onDismiss: () -> Unit
) {
    var prefs by remember(currentPrefs) { mutableStateOf(currentPrefs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Powiadomienia push") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Wybierz, o czym chcesz być powiadamiany:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                NotificationToggle(
                    title = "Nowa opinia o moim miejscu",
                    description = "Kto\u015B wystawi\u0142 opini\u0119 o miejscu, kt\u00F3re doda\u0142e\u015B",
                    checked = prefs.newReviewOnMyPlace,
                    onCheckedChange = { prefs = prefs.copy(newReviewOnMyPlace = it) }
                )
                NotificationToggle(
                    title = "Nowa odznaka",
                    description = "Zdoby\u0142e\u015B lub straci\u0142e\u015B odznak\u0119",
                    checked = prefs.newBadgeEarned,
                    onCheckedChange = { prefs = prefs.copy(newBadgeEarned = it) }
                )
                NotificationToggle(
                    title = "Nowe zdj\u0119cie do mojego miejsca",
                    description = "Kto\u015B doda\u0142 zdj\u0119cie do miejsca, kt\u00F3re doda\u0142e\u015B",
                    checked = prefs.newPhotoOnMyPlace,
                    onCheckedChange = { prefs = prefs.copy(newPhotoOnMyPlace = it) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(prefs) }) { Text("Zapisz") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}

@Composable
private fun NotificationToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
