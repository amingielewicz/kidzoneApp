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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kidzone.R
import com.kidzone.presentation.common.ModalActionColor
import com.kidzone.presentation.common.ModalDialogShape
import com.kidzone.presentation.common.ModalTextButton
import com.kidzone.presentation.common.OfflineAwareSubmitButton

data class NotificationPrefs(
    val newReviewOnMyPlace: Boolean = true,
    val newBadgeEarned: Boolean = true,
    val newPhotoOnMyPlace: Boolean = true,
    val rankings: Boolean = true,
    val emailNotificationsEnabled: Boolean = true,
)

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun NotificationPreferencesDialog(
    currentPrefs: NotificationPrefs,
    isOffline: Boolean,
    onSave: (NotificationPrefs) -> Unit,
    onDismiss: () -> Unit,
) {
    var prefs by remember(currentPrefs) {
        mutableStateOf(currentPrefs)
    }

    val hasChanges = prefs != currentPrefs

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ModalDialogShape,
        title = {
            Text(
                text = stringResource(
                    R.string.notifications_title,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        R.string.push_notifications_section,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                Text(
                    text = stringResource(
                        R.string.notification_preferences_subtitle,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(
                    modifier = Modifier.height(12.dp),
                )

                NotificationToggle(
                    title = stringResource(
                        R.string.pref_new_review_title,
                    ),
                    description = stringResource(
                        R.string.pref_new_review_desc,
                    ),
                    checked = prefs.newReviewOnMyPlace,
                    onCheckedChange = {
                        prefs = prefs.copy(
                            newReviewOnMyPlace = it,
                        )
                    },
                )

                NotificationToggle(
                    title = stringResource(
                        R.string.pref_new_badge_title,
                    ),
                    description = stringResource(
                        R.string.pref_new_badge_desc,
                    ),
                    checked = prefs.newBadgeEarned,
                    onCheckedChange = {
                        prefs = prefs.copy(
                            newBadgeEarned = it,
                        )
                    },
                )

                NotificationToggle(
                    title = stringResource(
                        R.string.pref_new_photo_title,
                    ),
                    description = stringResource(
                        R.string.pref_new_photo_desc,
                    ),
                    checked = prefs.newPhotoOnMyPlace,
                    onCheckedChange = {
                        prefs = prefs.copy(
                            newPhotoOnMyPlace = it,
                        )
                    },
                )

                NotificationToggle(
                    title = stringResource(
                        R.string.pref_rankings_title,
                    ),
                    description = stringResource(
                        R.string.pref_rankings_desc,
                    ),
                    checked = prefs.rankings,
                    onCheckedChange = {
                        prefs = prefs.copy(
                            rankings = it,
                        )
                    },
                )

                Spacer(
                    modifier = Modifier.height(16.dp),
                )

                Text(
                    text = stringResource(
                        R.string.email_notifications_section,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                NotificationToggle(
                    title = stringResource(
                        R.string.email_notifications_section,
                    ),
                    description = stringResource(
                        R.string.pref_email_notifs_desc,
                    ),
                    checked = prefs.emailNotificationsEnabled,
                    onCheckedChange = {
                        prefs = prefs.copy(
                            emailNotificationsEnabled = it,
                        )
                    },
                )
            }
        },
        confirmButton = {
            OfflineAwareSubmitButton(
                label = stringResource(
                    R.string.save_changes,
                ),
                onClick = {
                    onSave(prefs)
                },
                isOffline = isOffline,
                enabled = hasChanges,
            )
        },
        dismissButton = {
            ModalTextButton(
                text = stringResource(
                    R.string.cancel,
                ),
                onClick = onDismiss,
            )
        },
    )
}

@Suppress("FunctionNaming")
@Composable
private fun NotificationToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )

            Spacer(
                modifier = Modifier.height(2.dp),
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = ModalActionColor,
                checkedBorderColor = ModalActionColor,
            ),
        )
    }
}
