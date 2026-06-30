package com.kidzone.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kidzone.R
import com.kidzone.utils.UiText
import kotlinx.coroutines.launch

/**
 * Modal bottom sheet z formularzem edycji profilu.
 */
@Suppress("LongParameterList", "LongMethod", "FunctionNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    initialDisplayName: String,
    initialFirstName: String,
    initialLastName: String,
    currentAvatarUrl: String?,
    isSaving: Boolean,
    errorMessage: UiText?,
    onDismiss: () -> Unit,
    onSave: (displayName: String, firstName: String, lastName: String, newAvatarUri: Uri?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var displayName by rememberSaveable(initialDisplayName) {
        mutableStateOf(initialDisplayName)
    }
    var firstName by rememberSaveable(initialFirstName) {
        mutableStateOf(initialFirstName)
    }
    var lastName by rememberSaveable(initialLastName) {
        mutableStateOf(initialLastName)
    }
    var pendingAvatarUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        uri?.let { pendingAvatarUri = it }
    }

    val isFormValid = displayName.trim().isNotBlank()

    ModalBottomSheet(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.edit_profile),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AvatarPicker(
                    pendingUri = pendingAvatarUri,
                    fallbackUrl = currentAvatarUrl,
                    enabled = !isSaving,
                ) {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.click_to_change_avatar),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { RequiredFieldLabel(stringResource(R.string.login_nick_label)) },
                supportingText = {
                    if (displayName.isBlank()) {
                        Text(stringResource(R.string.field_required))
                    } else {
                        Text(stringResource(R.string.username_helper))
                    }
                },
                singleLine = true,
                isError = displayName.trim().isEmpty(),
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text(stringResource(R.string.first_name)) },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text(stringResource(R.string.last_name)) },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            errorMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = msg.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion { onDismiss() }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(
                            displayName.trim(),
                            firstName.trim(),
                            lastName.trim(),
                            pendingAvatarUri,
                        )
                    },
                    enabled = !isSaving && isFormValid,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.save_changes))
                    }
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun AvatarPicker(
    pendingUri: Uri?,
    fallbackUrl: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val avatarSize = 96.dp
    Box(modifier = Modifier.size(avatarSize)) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(
                    enabled = enabled,
                    onClickLabel = stringResource(R.string.change_avatar_label),
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AvatarImageContent(pendingUri, fallbackUrl, avatarSize)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun AvatarImageContent(pendingUri: Uri?, fallbackUrl: String?, avatarSize: Dp) {
    when {
        pendingUri != null -> {
            AsyncImage(
                model = pendingUri,
                contentDescription = stringResource(R.string.selected_avatar_desc),
                modifier = Modifier.size(avatarSize).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        !fallbackUrl.isNullOrBlank() -> {
            AsyncImage(
                model = fallbackUrl,
                contentDescription = stringResource(R.string.current_avatar_desc),
                modifier = Modifier.size(avatarSize).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun RequiredFieldLabel(text: String) {
    val errorColor = MaterialTheme.colorScheme.error
    Text(
        buildAnnotatedString {
            append(text)
            withStyle(SpanStyle(color = errorColor)) {
                append(" *")
            }
        },
    )
}
