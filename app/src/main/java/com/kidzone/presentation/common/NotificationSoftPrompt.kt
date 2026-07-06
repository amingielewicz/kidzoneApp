package com.kidzone.presentation.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.kidzone.R

private const val NOTIFICATION_PROMPT_PREFS = "notification_soft_prompts"

enum class NotificationPromptReason(
    val prefKey: String,
    @StringRes val messageRes: Int
) {
    FirstPlace(
        prefKey = "first_place",
        messageRes = R.string.notification_prompt_first_place
    ),
    FirstBadge(
        prefKey = "first_badge",
        messageRes = R.string.notification_prompt_first_badge
    ),
    Top10(
        prefKey = "top10",
        messageRes = R.string.notification_prompt_top10
    ),
    Podium(
        prefKey = "podium",
        messageRes = R.string.notification_prompt_podium
    )
}

fun shouldShowNotificationPrompt(context: Context, reason: NotificationPromptReason): Boolean =
    !hasNotificationPermission(context) &&
        !context.getSharedPreferences(NOTIFICATION_PROMPT_PREFS, Context.MODE_PRIVATE)
            .getBoolean(reason.prefKey, false)

fun markNotificationPromptShown(context: Context, reason: NotificationPromptReason) {
    context.getSharedPreferences(NOTIFICATION_PROMPT_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(reason.prefKey, true)
        .apply()
}

fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

@Composable
@Suppress("FunctionNaming")
fun NotificationSoftPromptDialog(
    reason: NotificationPromptReason,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        markNotificationPromptShown(context, reason)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = {
            markNotificationPromptShown(context, reason)
            onDismiss()
        },
        title = {
            Text(stringResource(R.string.notification_permission_title))
        },
        text = {
            Text(stringResource(reason.messageRes))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    markNotificationPromptShown(context, reason)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.later))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        markNotificationPromptShown(context, reason)
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(R.string.enable_notifications))
            }
        }
    )
}
