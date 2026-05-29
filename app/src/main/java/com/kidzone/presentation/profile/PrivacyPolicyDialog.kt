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
 * Dialog z polityką prywatności kidZone.
 *
 * Treść jest celowo trzymana **w aplikacji** (nie linkujemy do zewnętrznej
 * strony), żeby:
 *  - była dostępna offline,
 *  - można ją było aktualizować razem z release'em (jedno źródło prawdy),
 *  - na sklepie Google Play wystarczyło wskazać tę samą treść jako
 *    "in-app privacy policy" (Google akceptuje to równolegle z URL-em).
 *
 * Tekst jest **placeholderem MVP** – do uzupełnienia przez właściciela
 * aplikacji o:
 *  - dane administratora danych (nazwa firmy / osoba),
 *  - kontakt e-mail RODO,
 *  - aktualną datę wejścia w życie.
 *
 * Świadomie pokazujemy ten zalążek od razu zamiast pustego ekranu –
 * wymóg Google Play (każda aplikacja zbierająca dane userów MUSI mieć
 * politykę prywatności widoczną w aplikacji), więc lepiej wystartować
 * z uczciwym szkicem niż z pustym TODO.
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
                SectionTitle("1. Administrator danych")
                Text(
                    "Administratorem Twoich danych osobowych jest zespół kidZone. " +
                        "W sprawach dotyczących przetwarzania danych skontaktuj się " +
                        "z nami pod adresem e-mail wskazanym w sklepie Google Play."
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
                        "przetwarzający, na potrzeby działania aplikacji.\n" +
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
                        "wprowadzić samodzielnie z poziomu zakładki Profil. Aby usunąć " +
                        "konto wraz z opiniami i miejscami, napisz do nas na e-mail z " +
                        "informacją o usunięciu konta."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle("6. Czas przechowywania")
                Text(
                    "Dane konta przechowujemy do momentu usunięcia konta. Treści " +
                        "publiczne (miejsca, opinie) mogą zostać zachowane w formie " +
                        "zanonimizowanej, jeśli ich usunięcie utrudniłoby działanie " +
                        "aplikacji innym użytkownikom."
                )
                Spacer(Modifier.height(12.dp))

                SectionTitle("7. Zmiany w polityce")
                Text(
                    "O istotnych zmianach w polityce prywatności poinformujemy w aplikacji " +
                        "przy najbliższym uruchomieniu po wdrożeniu zmian."
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
