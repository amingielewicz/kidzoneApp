@file:Suppress("FunctionNaming")

package com.kidzone.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.kidzone.presentation.profile.PrivacyPolicyDialog
import com.kidzone.presentation.profile.TermsOfServiceDialog

private val TOS_DIALOG_SHAPE = RoundedCornerShape(12.dp)
private val ICON_SIZE_LARGE = 48.dp
private val SPACING_STANDARD = 24.dp
private val SPACING_SMALL = 8.dp
private val PROGRESS_SIZE = 20.dp

/**
 * Blokujący dialog wymuszający akceptację Regulaminu i Polityki prywatności.
 * Wyświetlany dla wszystkich zalogowanych użytkowników, którzy jeszcze nie wyrazili zgody.
 */
@Composable
fun MandatoryTosDialog(
    onAccept: () -> Unit,
    isAccepting: Boolean
) {
    var showTerms by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        icon = { TosIcon() },
        title = { TosTitle() },
        text = {
            TosContent(
                onShowTerms = { showTerms = true },
                onShowPrivacy = { showPrivacy = true }
            )
        },
        confirmButton = {
            TosConfirmButton(onAccept = onAccept, isAccepting = isAccepting)
        }
    )

    if (showTerms) TermsOfServiceDialog(onDismiss = { showTerms = false })
    if (showPrivacy) PrivacyPolicyDialog(onDismiss = { showPrivacy = false })
}

@Composable
private fun TosIcon() {
    Icon(
        imageVector = Icons.Default.Description,
        contentDescription = null,
        modifier = Modifier.size(ICON_SIZE_LARGE),
        tint = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun TosTitle() {
    Text(
        text = "Zasady korzystania z kidZone",
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TosContent(onShowTerms: () -> Unit, onShowPrivacy: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Przed przejściem do aplikacji prosimy o zapoznanie się i zaakceptowanie " +
                    "Regulaminu oraz Polityki prywatności.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(SPACING_STANDARD))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onShowTerms,
                modifier = Modifier.weight(1f),
                shape = TOS_DIALOG_SHAPE
            ) {
                Text("Regulamin")
            }
            Spacer(Modifier.width(SPACING_SMALL))
            OutlinedButton(
                onClick = onShowPrivacy,
                modifier = Modifier.weight(1f),
                shape = TOS_DIALOG_SHAPE
            ) {
                Text("Prywatność")
            }
        }
    }
}

@Composable
private fun TosConfirmButton(onAccept: () -> Unit, isAccepting: Boolean) {
    Button(
        onClick = onAccept,
        enabled = !isAccepting,
        modifier = Modifier.fillMaxWidth(),
        shape = TOS_DIALOG_SHAPE
    ) {
        if (isAccepting) {
            CircularProgressIndicator(
                modifier = Modifier.size(PROGRESS_SIZE),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("Akceptuję i przechodzę dalej", fontWeight = FontWeight.Bold)
        }
    }
}
