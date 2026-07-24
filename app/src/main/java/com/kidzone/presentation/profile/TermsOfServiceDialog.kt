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
import androidx.compose.ui.graphics.Color
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

private val TermsLinkRegex = Regex("""(https?://\S+|mailto:\S+|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})""")
private val LinkTrailingPunctuation = setOf('.', ',', ';', ':', ')', ']')

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
                TermsText(stringResource(R.string.tos_section_1_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_2_title))
                TermsText(stringResource(R.string.tos_section_2_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_3_title))
                TermsText(stringResource(R.string.tos_section_3_intro))
                Spacer(Modifier.height(4.dp))
                BulletPoint2(stringResource(R.string.tos_section_3_bullet_1))
                BulletPoint2(stringResource(R.string.tos_section_3_bullet_2))
                BulletPoint2(stringResource(R.string.tos_section_3_bullet_3))
                BulletPoint2(stringResource(R.string.tos_section_3_bullet_4))
                BulletPoint2(stringResource(R.string.tos_section_3_bullet_5))
                BulletPoint2(stringResource(R.string.tos_section_3_bullet_6))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_4_title))
                TermsText(
                    text = stringResource(R.string.tos_section_4_body),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                TermsText(stringResource(R.string.tos_section_4_intro))
                Spacer(Modifier.height(4.dp))
                BulletPoint2(stringResource(R.string.tos_section_4_bullet_1))
                BulletPoint2(stringResource(R.string.tos_section_4_bullet_2))
                BulletPoint2(stringResource(R.string.tos_section_4_bullet_3))
                BulletPoint2(stringResource(R.string.tos_section_4_bullet_4))
                BulletPoint2(stringResource(R.string.tos_section_4_bullet_5))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_5_title))
                TermsText(stringResource(R.string.tos_section_5_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_6_title))
                TermsText(stringResource(R.string.tos_section_6_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_7_title))
                TermsText(stringResource(R.string.tos_section_7_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_8_title))
                TermsText(stringResource(R.string.tos_section_8_body))
                Spacer(Modifier.height(12.dp))

                SectionTitle2(stringResource(R.string.tos_section_9_title))
                TermsText(stringResource(R.string.tos_section_9_body))
            }
        }
    )
}

@Composable
@Suppress("FunctionNaming")
private fun TermsText(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val linkColor = MaterialTheme.colorScheme.primary
    Text(
        text = text.linkified(linkColor),
        color = color
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
    val linkColor = MaterialTheme.colorScheme.primary
    Text(
        text = stringResource(R.string.bullet_point, text).linkified(linkColor),
        style = MaterialTheme.typography.bodySmall
    )
}

private fun String.linkified(linkColor: Color): AnnotatedString {
    val matches = TermsLinkRegex.findAll(this).toList()
    if (matches.isEmpty()) return AnnotatedString(this)

    return buildAnnotatedString {
        var cursor = 0
        matches.forEach { match ->
            val rawMatch = match.value
            val linkText = rawMatch.trimEnd { it in LinkTrailingPunctuation }
            val trailingText = rawMatch.substring(linkText.length)
            val start = match.range.first

            append(this@linkified.substring(cursor, start))
            appendLink(linkText, linkColor)
            append(trailingText)

            cursor = match.range.last + 1
        }
        append(this@linkified.substring(cursor))
    }
}

private fun AnnotatedString.Builder.appendLink(
    linkText: String,
    linkColor: Color
) {
    val url = when {
        linkText.startsWith("mailto:") -> linkText
        "@" in linkText && !linkText.startsWith("http") -> "mailto:$linkText"
        else -> linkText
    }
    withLink(
        LinkAnnotation.Url(
            url = url,
            styles = TextLinkStyles(
                style = SpanStyle(
                    color = linkColor,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline
                )
            )
        )
    ) {
        append(linkText)
    }
}
