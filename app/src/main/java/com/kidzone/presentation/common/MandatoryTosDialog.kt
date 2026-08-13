package com.kidzone.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.kidzone.R
import com.kidzone.presentation.profile.PrivacyPolicyDialog
import com.kidzone.presentation.profile.TermsOfServiceDialog

/**
 * Blokujący dialog wymuszający akceptację Regulaminu i Polityki prywatności.
 * Wyświetlany dla wszystkich zalogowanych użytkowników, którzy jeszcze nie wyrazili zgody.
 */
@Suppress("FunctionNaming")
@Composable
fun MandatoryTosDialog(
    onAccept: () -> Unit,
    isAccepting: Boolean
) {
    var showTerms by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { /* Nie pozwalamy zamknąć bez akceptacji */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        icon = {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.mandatory_tos_title),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            MandatoryTosDialogContent(
                onShowTerms = { showTerms = true },
                onShowPrivacy = { showPrivacy = true }
            )
        },
        confirmButton = {
            MandatoryTosConfirmButton(
                onClick = onAccept,
                isLoading = isAccepting
            )
        }
    )

    if (showTerms) {
        TermsOfServiceDialog(onDismiss = { showTerms = false })
    }
    if (showPrivacy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacy = false })
    }
}

@Suppress("FunctionNaming")
@Composable
private fun MandatoryTosDialogContent(
    onShowTerms: () -> Unit,
    onShowPrivacy: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.mandatory_tos_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onShowTerms,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.terms_of_service_short))
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onShowPrivacy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.privacy_policy_short))
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun MandatoryTosConfirmButton(
    onClick: () -> Unit,
    isLoading: Boolean
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = stringResource(R.string.mandatory_tos_accept),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
