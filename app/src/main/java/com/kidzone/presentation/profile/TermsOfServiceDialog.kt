package com.kidzone.presentation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
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

/**
 * Dialog z regulaminem kidZone.
 */
@Composable
fun TermsOfServiceDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        title = { Text(stringResource(R.string.terms_of_service)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SectionTitle2(stringResource(R.string.tos_section_1_title))
                Text(stringResource(R.string.tos_section_1_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_2_title))
                Text(stringResource(R.string.tos_section_2_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_3_title))
                Text(stringResource(R.string.tos_section_3_intro))
                Spacer(Modifier.height(4.dp))
                BulletPoint2(stringResource(R.string.tos_section_3_bullet_1))
                BulletPoint2(stringResource(R.string.tos_section_3_bullet_2))
                BulletPoint2(stringResource(R.string.tos_section_3_bullet_3))
                BulletPoint2(stringResource(R.string.tos_section_3_bullet_4))
                BulletPoint2(stringResource(R.string.tos_section_3_bullet_5))
                BulletPoint2(stringResource(R.string.tos_section_3_bullet_6))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_4_title))
                Text(
                    text = stringResource(R.string.tos_section_4_body),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.tos_section_4_intro))
                Spacer(Modifier.height(4.dp))
                BulletPoint2(stringResource(R.string.tos_section_4_bullet_1))
                BulletPoint2(stringResource(R.string.tos_section_4_bullet_2))
                BulletPoint2(stringResource(R.string.tos_section_4_bullet_3))
                BulletPoint2(stringResource(R.string.tos_section_4_bullet_4))
                BulletPoint2(stringResource(R.string.tos_section_4_bullet_5))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_5_title))
                Text(stringResource(R.string.tos_section_5_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_6_title))
                Text(stringResource(R.string.tos_section_6_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_7_title))
                Text(stringResource(R.string.tos_section_7_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_8_title))
                Text(stringResource(R.string.tos_section_8_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_9_title))
                Text(stringResource(R.string.tos_section_9_body))
            }
        }
    )
}

@Composable
private fun SectionTitle2(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun BulletPoint2(text: String) {
    Text(text = "  \u2022  $text", style = MaterialTheme.typography.bodySmall)
}
