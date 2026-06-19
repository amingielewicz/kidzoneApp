package com.kidzone.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/**
 * Modal bottom sheet z formularzem edycji profilu.
 *
 * Pola:
 *  - Avatar – klikalny, wybór z galerii przez nowy
 *    [ActivityResultContracts.PickVisualMedia]. Działa od Androida 4.4
 *    (compat-mode w starszych systemach), nie wymaga żadnych uprawnień
 *    runtime'owych. Dopóki user nie wybierze nowego zdjęcia, pokazuje
 *    obecny [currentAvatarUrl].
 *  - Login (nick) – wymagany, używany jako autor opinii i miejsc.
 *  - Imię, Nazwisko – opcjonalne dane osobowe.
 *
 * Sheet sam **nie wykonuje** zapisu ani uploadu – woła [onSave] z lokalnymi
 * wartościami i ewentualnym [Uri] nowego avatara. ProfileViewModel
 * wykonuje upload + Firestore write i zarządza spinnerem przez
 * [isSaving]. Błąd zapisu prezentujemy jako [errorMessage] u dołu –
 * sheet pozostaje otwarty, użytkownik może spróbować ponownie.
 *
 * Rotacja: dane formularza są trzymane w `rememberSaveable`, więc obrót
 * ekranu w trakcie edycji nie zgubi wpisanych wartości. `pendingAvatarUri`
 * NIE jest saveable (zwykłe `remember`), bo lokalny URI z Photo Pickera
 * traci uprawnienie po zniszczeniu Activity – zachowanie go między
 * rotacjami i tak nic by nie dało.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    initialDisplayName: String,
    initialFirstName: String,
    initialLastName: String,
    currentAvatarUrl: String?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (displayName: String, firstName: String, lastName: String, newAvatarUri: Uri?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Stan formularza – saveable, żeby przeżył rotację.
    var displayName by rememberSaveable(initialDisplayName) {
        mutableStateOf(initialDisplayName)
    }
    var firstName by rememberSaveable(initialFirstName) {
        mutableStateOf(initialFirstName)
    }
    var lastName by rememberSaveable(initialLastName) {
        mutableStateOf(initialLastName)
    }
    // Lokalnie wybrany avatar – jeszcze nie wgrany. Tracimy go po
    // dismissie sheet'a (intencjonalnie – Cancel = wyrzuć wybór).
    var pendingAvatarUri by remember { mutableStateOf<Uri?>(null) }

    // Modern photo picker – brak konieczności pytania o READ_MEDIA_IMAGES.
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingAvatarUri = uri
        }
    }

    // Walidacja: login (display name) jest wymagany. Imię/nazwisko mogą
    // być puste – traktujemy je jako opcjonalne pola personalne.
    val isFormValid = displayName.trim().isNotBlank()

    ModalBottomSheet(
        onDismissRequest = {
            // W trakcie zapisu nie pozwalamy zamknąć (uniknij gubienia
            // spinnera / nieoczekiwanego dismissa), użytkownik widzi
            // "Zapisywanie..." i czeka.
            if (!isSaving) onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Edytuj profil",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))

            // --- Avatar (klikalny, otwiera photo picker) ---
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AvatarPicker(
                    pendingUri = pendingAvatarUri,
                    fallbackUrl = currentAvatarUrl,
                    enabled = !isSaving,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Kliknij avatar, by zmienić zdjęcie",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // --- Login (publiczny nick) ---
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { RequiredFieldLabel("Login (publiczny nick)") },
                supportingText = {
                    Text("Widoczny w opiniach, miejscach i rankingu")
                },
                singleLine = true,
                isError = displayName.isNotEmpty() && displayName.isBlank(),
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // --- Imię ---
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Imię") },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // --- Nazwisko ---
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Nazwisko") },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(20.dp))

            // --- Akcje (Anuluj / Zapisz) ---
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        // Wykorzystujemy `scope` z rememberCoroutineScope,
                        // żeby gładko zamknąć animację sheet'a przed
                        // wywołaniem callbacku rodzica.
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion { onDismiss() }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Anuluj")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(
                            displayName.trim(),
                            firstName.trim(),
                            lastName.trim(),
                            pendingAvatarUri
                        )
                    },
                    enabled = !isSaving && isFormValid,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Zapisz")
                    }
                }
            }
        }
    }
}

/**
 * Klikalny okrągły avatar z lekkim badge'em "kamerki" w prawym dolnym
 * rogu – sygnalizuje, że da się go wymienić. Priorytet źródła obrazu:
 * 1. [pendingUri] – właśnie wybrane lokalnie (jeszcze nie wgrane do Storage),
 * 2. [fallbackUrl] – aktualny avatar usera z Firestore,
 * 3. placeholder – ikona aparatu na tle primary container.
 */
@Composable
private fun AvatarPicker(
    pendingUri: Uri?,
    fallbackUrl: String?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val avatarSize = 96.dp
    Box(modifier = Modifier.size(avatarSize)) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(enabled = enabled, onClickLabel = "Zmień avatar", role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            when {
                pendingUri != null -> {
                    AsyncImage(
                        model = pendingUri,
                        contentDescription = "Wybrany avatar",
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                !fallbackUrl.isNullOrBlank() -> {
                    AsyncImage(
                        model = fallbackUrl,
                        contentDescription = "Aktualny avatar",
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        // Mały "FAB-like" badge z ikoną kamerki – afordancja "klikalne".
        Box(
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Etykieta wymaganego pola – tekst + czerwona gwiazdka (spójnie z RegisterScreen). */
@Composable
private fun RequiredFieldLabel(text: String) {
    val errorColor = MaterialTheme.colorScheme.error
    Text(
        buildAnnotatedString {
            append(text)
            withStyle(SpanStyle(color = errorColor)) {
                append(" *")
            }
        }
    )
}
