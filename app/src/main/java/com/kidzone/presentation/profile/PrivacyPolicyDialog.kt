package com.kidzone.presentation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kidzone.R
import com.kidzone.utils.AppConfig

/**
 * Dialog z polityką prywatności kidZone.
 */
@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        title = { Text(stringResource(R.string.privacy_policy)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(
                        R.string.privacy_policy_effective_date,
                        AppConfig.PRIVACY_POLICY_EFFECTIVE_DATE,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle(stringResource(R.string.pp_section_1_title))
                Text(
                    stringResource(
                        R.string.pp_section_1_body,
                        AppConfig.ADMINISTRATOR_NAME,
                        AppConfig.PRIVACY_CONTACT_EMAIL,
                    ),
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle(stringResource(R.string.pp_section_2_title))
                Text(stringResource(R.string.pp_section_2_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle(stringResource(R.string.pp_section_3_title))
                Text(stringResource(R.string.pp_section_3_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle(stringResource(R.string.pp_section_4_title))
                Text(stringResource(R.string.pp_section_4_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle(stringResource(R.string.pp_section_5_title))
                Text(
                    stringResource(R.string.pp_section_5_body, AppConfig.PRIVACY_CONTACT_EMAIL),
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle(stringResource(R.string.pp_section_6_title))
                Text(stringResource(R.string.pp_section_6_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle(stringResource(R.string.pp_section_7_title))
                Text(stringResource(R.string.pp_section_7_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle(stringResource(R.string.pp_section_8_title))
                Text(stringResource(R.string.pp_section_8_body))
            }
        },
    )
}

@Suppress("FunctionNaming")
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(4.dp))
}
