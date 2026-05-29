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
import com.kidzone.utils.AppConfig

/**
 * Dialog z polityką prywatności kidZone.
 *
 * Treść jest celowo trzymana **w aplikacji** (nie linkujemy do zewnętrznej
 * strony), żeby:
 *  - była dostępna offline,
 *  - można ją było aktualizować razem z release'em (jedno źródło prawdy),
 *  - na sklepie Google Play wystarczyło wskazać tę samą treść jako
 *    "in-app privacy policy" (Google akceptuje to równolegle z URL-em).
 *
 * Dane kontaktowe administratora i e-mail RODO pochodzą z [AppConfig] –
 * jedno źródło prawdy, edytowalne bez modyfikowania samego dialogu.
 *
 * **Uwaga prawna**: ten tekst jest szablonem stworzonym z perspektywy
 * developerskiej. Dla aplikacji wprowadzanej oficjalnie do obrotu w UE
 * (Google Play) zalecana jest weryfikacja u prawnika RODO, szczególnie:
 *  - podstawa prawna przetwarzania (art. 6 RODO),
 *  - klauzula o profilowaniu,
 *  - lista podmiotów przetwarzających poza EOG (Google – serwery US).
 */
@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        },
        title = { Text("Polityka prywatności") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Obowiązuje od: ${AppConfig.PRIVACY_POLICY_EFFECTIVE_DATE}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle("1. Administrator danych")
                Text(
                    "Administratorem Twoich danych osobowych jest " +
                        "${AppConfig.ADMINISTRATOR_NAME}. W sprawach dotyczących " +
                        "przetwarzania Twoich danych skontaktuj się z nami pod adresem " +
                        "e-mail: ${AppConfig.PRIVACY_CONTACT_EMAIL}."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle("2. Jakie dane zbieramy")
                Text(
                    "• Adres e-mail oraz nazwa konta (login) podane przy rejestracji.\n" +
                        "• Imię, nazwisko i avatar – tylko jeśli sam(a) je dodasz w profilu.\n" +
                        "• Treści, które tworzysz w aplikacji: dodane miejsca, opinie, " +
                        "oceny i przybliżona lokalizacja tych miejsc.\n" +
                        "• Dane logowania Google (jeśli wybierzesz logowanie przez Google) " +
                        "– otrzymujemy od Google jedynie identyfikator, e-mail i avatar.\n" +
                        "• Bieżąca lokalizacja urządzenia – tylko gdy świadomie użyjesz " +
                        "funkcji „Pobierz moją lokalizację” przy dodawaniu miejsca lub " +
                        "wyświetlaniu miejsc w pobliżu. Nie zapisujemy historii lokalizacji."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle("3. W jakim celu")
                Text(
                    "Dane wykorzystujemy wyłącznie do działania aplikacji: zalogowania, " +
                        "wyświetlania mapy miejsc przyjaznych dzieciom, prezentacji opinii " +
                        "innych rodziców oraz rankingu najaktywniejszych użytkowników. " +
                        "Nie używamy Twoich danych do reklam ani profilowania."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle("4. Komu udostępniamy dane")
                Text(
                    "• Google Firebase (Authentication, Firestore, Storage) – jako podmiot " +
                        "przetwarzający, na potrzeby działania aplikacji. Serwery Google " +
                        "mogą znajdować się poza Europejskim Obszarem Gospodarczym; " +
                        "Google zapewnia odpowiedni poziom ochrony w ramach " +
                        "Standardowych Klauzul Umownych UE.\n" +
                        "• Inni użytkownicy aplikacji – widzą Twój login, avatar oraz treści, " +
                        "które publicznie publikujesz (miejsca, opinie). Imię i nazwisko " +
                        "pozostają prywatne."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle("5. Twoje prawa")
                Text(
                    "Masz prawo do dostępu do swoich danych, ich sprostowania, usunięcia " +
                        "(„prawo do bycia zapomnianym”), ograniczenia przetwarzania, " +
                        "przenoszenia oraz wniesienia sprzeciwu. Większość zmian możesz " +
                        "wprowadzić samodzielnie z poziomu zakładki Profil – w tym " +
                        "edycję danych i trwałe usunięcie konta wraz z opiniami " +
                        "i miejscami (przycisk „Usuń konto”).\n\n" +
                        "Jeśli z jakiegokolwiek powodu nie możesz wykonać tych operacji " +
                        "samodzielnie – napisz na ${AppConfig.PRIVACY_CONTACT_EMAIL}, " +
                        "a my pomożemy."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle("6. Czas przechowywania")
                Text(
                    "Dane konta przechowujemy do momentu usunięcia konta. Treści " +
                        "publiczne (miejsca, opinie) mogą zostać zachowane w formie " +
                        "zanonimizowanej, jeśli ich usunięcie utrudniłoby działanie " +
                        "aplikacji innym użytkownikom (np. opinie innych userów na " +
                        "Twoich miejscach – po usunięciu konta Twoje miejsca znikają, " +
                        "ale opinie innych mogą zostać do czasu manualnego sprzątnięcia)."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle("7. Bezpieczeństwo")
                Text(
                    "Komunikacja aplikacji z serwerami odbywa się wyłącznie przez " +
                        "szyfrowane połączenia HTTPS. Twoje hasło nigdy nie jest " +
                        "przesyłane ani przechowywane w postaci jawnej – Firebase Auth " +
                        "trzyma tylko jego skrót (hash). Avatary i zdjęcia trafiają " +
                        "do prywatnego bucketa Firebase Storage, dostępnego tylko " +
                        "dla zalogowanych użytkowników."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle("8. Zmiany w polityce")
                Text(
                    "O istotnych zmianach w polityce prywatności poinformujemy w aplikacji " +
                        "przy najbliższym uruchomieniu po wdrożeniu zmian, aktualizując " +
                        "datę „Obowiązuje od” na górze tego dokumentu."
                )
            }
        }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
}
