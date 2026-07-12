package com.kidzone.presentation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.kidzone.R
import com.kidzone.presentation.common.ModalActionColor
import com.kidzone.presentation.common.ModalDialogShape
import com.kidzone.presentation.common.ModalTextButton
import com.kidzone.utils.AppConfig

/**
 * Dialog z polityką prywatności kidZone.
 */
@Suppress("FunctionNaming", "LongMethod")
@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit,
) {
    val administratorSection = stringResource(
        R.string.pp_section_1_body,
        AppConfig.ADMINISTRATOR_NAME,
        AppConfig.PRIVACY_CONTACT_EMAIL,
    )

    val contactSection = stringResource(
        R.string.pp_section_5_body,
        AppConfig.PRIVACY_CONTACT_EMAIL,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ModalDialogShape,
        title = {
            Text(
                text = stringResource(
                    R.string.privacy_policy,
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.privacy_policy_effective_date,
                        AppConfig.PRIVACY_POLICY_EFFECTIVE_DATE,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                PolicySectionSpacer()

                PolicySectionTitle(
                    text = stringResource(
                        R.string.pp_section_1_title,
                    ),
                )

                Text(
                    text = textWithEmailLink(
                        fullText = administratorSection,
                        email = AppConfig.PRIVACY_CONTACT_EMAIL,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                PolicySectionSpacer()

                PolicySectionTitle(
                    text = stringResource(
                        R.string.pp_section_2_title,
                    ),
                )

                Text(
                    text = stringResource(
                        R.string.pp_section_2_body,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                PolicySectionSpacer()

                PolicySectionTitle(
                    text = stringResource(
                        R.string.pp_section_3_title,
                    ),
                )

                Text(
                    text = stringResource(
                        R.string.pp_section_3_body,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                PolicySectionSpacer()

                PolicySectionTitle(
                    text = stringResource(
                        R.string.pp_section_4_title,
                    ),
                )

                Text(
                    text = stringResource(
                        R.string.pp_section_4_body,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                PolicySectionSpacer()

                PolicySectionTitle(
                    text = stringResource(
                        R.string.pp_section_5_title,
                    ),
                )

                Text(
                    text = textWithEmailLink(
                        fullText = contactSection,
                        email = AppConfig.PRIVACY_CONTACT_EMAIL,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                PolicySectionSpacer()

                PolicySectionTitle(
                    text = stringResource(
                        R.string.pp_section_6_title,
                    ),
                )

                Text(
                    text = stringResource(
                        R.string.pp_section_6_body,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                PolicySectionSpacer()

                PolicySectionTitle(
                    text = stringResource(
                        R.string.pp_section_7_title,
                    ),
                )

                Text(
                    text = stringResource(
                        R.string.pp_section_7_body,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                PolicySectionSpacer()

                PolicySectionTitle(
                    text = stringResource(
                        R.string.pp_section_8_title,
                    ),
                )

                Text(
                    text = stringResource(
                        R.string.pp_section_8_body,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            ModalTextButton(
                text = stringResource(R.string.close),
                onClick = onDismiss,
            )
        },
    )
}

/**
 * Buduje tekst, w którym wskazany adres e-mail jest klikalny.
 */
private fun textWithEmailLink(
    fullText: String,
    email: String,
): AnnotatedString {
    if (email.isBlank() || !fullText.contains(email)) {
        return AnnotatedString(fullText)
    }

    val startIndex = fullText.indexOf(email)
    val before = fullText.substring(0, startIndex)
    val after = fullText.substring(startIndex + email.length)

    return buildAnnotatedString {
        append(before)

        withLink(
            LinkAnnotation.Url(
                url = "mailto:$email",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = ModalActionColor,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
            ),
        ) {
            append(email)
        }

        append(after)
    }
}

@Suppress("FunctionNaming")
@Composable
private fun PolicySectionTitle(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )

    Spacer(
        modifier = Modifier.height(4.dp),
    )
}

@Suppress("FunctionNaming")
@Composable
private fun PolicySectionSpacer() {
    Spacer(
        modifier = Modifier.height(12.dp),
    )
}
