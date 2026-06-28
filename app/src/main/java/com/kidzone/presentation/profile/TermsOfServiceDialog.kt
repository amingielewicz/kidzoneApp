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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Dialog z regulaminem kidZone.
 */
@Composable
fun TermsOfServiceDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        },
        title = { Text("Regulamin użytkowania") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SectionTitle2("1. Zasady ogólne")
                Text(
                    "Aplikacja kidZone służy do dzielenia się informacjami o miejscach " +
                        "przyjaznych dzieciom. Korzystając z niej, zobowiązujesz się do " +
                        "przestrzegania niniejszego Regulaminu."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle2("2. Konto")
                Text(
                    "Każdy użytkownik może posiadać jedno konto. Jesteś odpowiedzialny " +
                        "za bezpieczeństwo swoich danych logowania. Możesz usunąć konto " +
                        "w dowolnym momencie w Profilu, w sekcji Konto i bezpieczeństwo. " +
                        "Publiczna instrukcja usuwania konta jest dostępna pod adresem " +
                        "https://playground-705e7162.web.app/account-deletion."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle2("3. Zasady zachowania")
                Text("Zobowiązujesz się do:")
                Spacer(Modifier.height(4.dp))
                BulletPoint2("Publikowania prawdziwych informacji o miejscach")
                BulletPoint2("Wyrażania opinii kulturalnie i merytorycznie")
                BulletPoint2("Szanowania innych użytkowników")
                BulletPoint2("Niepublikowania treści obraźliwych, spamu ani reklam")
                BulletPoint2("Niedodawania nieodpowiednich zdjęć")
                BulletPoint2("Niemanipulowania rankingami i odznakami")
                Spacer(Modifier.height(12.dp))

                SectionTitle2("4. Blokada konta")
                Text(
                    "Naruszenie Regulaminu może skutkować blokadą konta. Blokada " +
                        "może być czasowa (na określoną liczbę dni) lub bezpowrotna " +
                        "(permanentna). Zablokowany użytkownik nie może się zalogować " +
                        "do aplikacji. O blokadzie zostaniesz poinformowany emailem " +
                        "z podaniem powodu.",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                Text("Blokada może nastąpić w szczególności za:")
                Spacer(Modifier.height(4.dp))
                BulletPoint2("Dodawanie fałszywych informacji")
                BulletPoint2("Publikowanie treści obraźliwych lub spamu")
                BulletPoint2("Nękanie innych użytkowników")
                BulletPoint2("Dodawanie nieodpowiednich zdjęć")
                BulletPoint2("Tworzenie wielu kont w celu obejścia blokady")
                Spacer(Modifier.height(12.dp))

                SectionTitle2("5. Usunięcie treści")
                Text(
                    "Administrator może usunąć miejsce, opinię lub zdjęcie naruszające " +
                        "Regulamin. O usunięciu zostaniesz poinformowany emailem z powodem."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle2("6. Treści użytkowników")
                Text(
                    "Dodając treści, oświadczasz, że masz prawo do ich publikacji. " +
                        "Po usunięciu konta Twoje opinie zostaną zanonimizowane, " +
                        "a dodane miejsca mogą pozostać widoczne, jeżeli nie zawierają " +
                        "danych osobowych i są częścią publicznej bazy miejsc."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle2("7. Zdjęcia dzieci i osób trzecich")
                Text(
                    "Nie publikuj zdjęć dzieci ani osób trzecich bez wszystkich wymaganych " +
                        "zgód. Zdjęcia naruszające prywatność, wizerunek lub bezpieczeństwo " +
                        "dziecka mogą zostać usunięte po zgłoszeniu."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle2("8. Odpowiedzialność")
                Text(
                    "kidZone nie ponosi odpowiedzialności za treści publikowane " +
                        "przez użytkowników ani nie gwarantuje ich aktualności."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle2("9. Zmiany Regulaminu")
                Text(
                    "O istotnych zmianach Regulaminu zostaniesz poinformowany " +
                        "w aplikacji lub emailem. Dalsze korzystanie z aplikacji " +
                        "oznacza akceptację nowych warunków."
                )
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
