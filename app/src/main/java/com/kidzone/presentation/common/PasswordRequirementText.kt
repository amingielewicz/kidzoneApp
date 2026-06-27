package com.kidzone.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kidzone.R
import com.kidzone.utils.PasswordPolicy

@Composable
fun passwordRequirementText(labelKey: PasswordPolicy.LabelKey): String = when (labelKey) {
    PasswordPolicy.LabelKey.MinLength -> stringResource(
        R.string.password_requirement_min_length,
        PasswordPolicy.MIN_LENGTH
    )
    PasswordPolicy.LabelKey.Lowercase -> stringResource(R.string.password_requirement_lowercase)
    PasswordPolicy.LabelKey.Uppercase -> stringResource(R.string.password_requirement_uppercase)
    PasswordPolicy.LabelKey.SpecialCharacter -> stringResource(
        R.string.password_requirement_special_character
    )
}
