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
    val newPlaceNearby: Boolean = true,
    val weeklyDigest: Boolean = true
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
                    title = "Nowa opinia na moim miejscu",
                    description = "Ktoś wystawił opinię na miejscu, które dodałeś",
                    checked = prefs.newReviewOnMyPlace,
                    onCheckedChange = { prefs = prefs.copy(newReviewOnMyPlace = it) }
                )
                NotificationToggle(
                    title = "Nowe miejsce w okolicy",
                    description = "Ktoś dodał nowe miejsce blisko Ciebie",
                    checked = prefs.newPlaceNearby,
                    onCheckedChange = { prefs = prefs.copy(newPlaceNearby = it) }
                )
                NotificationToggle(
                    title = "Podsumowanie tygodniowe",
                    description = "Co nowego w kidZone w Twojej okolicy",
                    checked = prefs.weeklyDigest,
                    onCheckedChange = { prefs = prefs.copy(weeklyDigest = it) }
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
