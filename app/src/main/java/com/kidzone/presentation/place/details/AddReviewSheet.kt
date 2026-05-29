package com.kidzone.presentation.place.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Maksymalna długość komentarza opinii. Świadomy kompromis między swobodą
 * wypowiedzi a UX listy (długie opinie psują skanowanie karty miejsca)
 * oraz kosztem Firestore (1MB hard limit per dokument).
 *
 * Walidacja jest egzekwowana w dwóch miejscach (defense-in-depth):
 *  - tu w UI – cap w `onValueChange` + licznik + kolor erroru,
 *  - w `FirestoreReviewRepository.addReview` – `require(...)`.
 */
private const val COMMENT_MAX_LENGTH = 1000

/**
 * Bottom sheet z formularzem dodawania opinii o miejscu.
 *
 * - Stan formularza (rating + comment) trzymany lokalnie przez `rememberSaveable`,
 *   żeby przeżył rotację ekranu i tymczasowe schowanie sheetu.
 * - Stan wysyłki (`isSubmitting`, `errorMessage`) przychodzi z parent-VM przez
 *   parametry – sheet jest "głupi", VM steruje cyklem życia operacji.
 * - Po pomyślnym zapisie parent ustawia w VM `showAddReviewSheet = false`,
 *   wtedy sheet znika z drzewa kompozycji – stan formularza się czyści.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewSheet(
    placeName: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var rating by rememberSaveable { mutableIntStateOf(0) }
    var comment by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // imePadding domyślnie nie obsłuży klawiatury wewnątrz sheetu w
        // wszystkich wersjach Material3, dlatego dodajemy ręcznie poniżej.
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Text(
                text = if (placeName.isNotBlank()) "Oceń \"$placeName\"" else "Dodaj opinię",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Twoja ocena",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            StarRatingInput(
                rating = rating,
                onChange = { rating = it },
                enabled = !isSubmitting
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { newValue ->
                    // Hard-cap długości komentarza po stronie UI: tnij do
                    // limitu zamiast odrzucać cały input. Dzięki temu wklejenie
                    // tekstu dłuższego niż 1000 znaków daje pierwsze 1000
                    // (intuicyjne), zamiast po cichu znikać. Repo dodatkowo
                    // waliduje to samo (defense-in-depth).
                    comment = newValue.take(COMMENT_MAX_LENGTH)
                },
                label = { Text("Komentarz (opcjonalnie)") },
                placeholder = { Text("Co sądzisz o tym miejscu?") },
                minLines = 3,
                maxLines = 6,
                enabled = !isSubmitting,
                supportingText = {
                    Text(
                        text = "${comment.length} / $COMMENT_MAX_LENGTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (comment.length >= COMMENT_MAX_LENGTH) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onSubmit(rating, comment) },
                enabled = rating in 1..5 && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Opublikuj opinię")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Interaktywny picker oceny 1..5. Klik w gwiazdkę o numerze N ustawia
 * rating=N (i tym samym podświetla wszystkie gwiazdki ≤ N). Klik w już
 * wybraną gwiazdkę nie zmienia stanu (świadomie, by uniknąć przypadkowego
 * "wyzerowania" oceny).
 */
@Composable
private fun StarRatingInput(
    rating: Int,
    onChange: (Int) -> Unit,
    enabled: Boolean = true,
    starSize: Dp = 40.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (1..5).forEach { star ->
            val isFilled = star <= rating
            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = "Oceń na $star",
                tint = if (isFilled) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .size(starSize)
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClick = { onChange(star) }
                    )
            )
        }
        Spacer(Modifier.width(8.dp))
        if (rating > 0) {
            Text(
                text = "$rating/5",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
