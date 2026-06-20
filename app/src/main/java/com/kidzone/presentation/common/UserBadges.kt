@file:Suppress("MagicNumber")

package com.kidzone.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Reviews
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kidzone.domain.model.User

/**
 * Odznaki użytkownika, wyliczane lokalnie z liczników na [User] oraz
 * (opcjonalnie) z kontekstu rankingowego ([BadgeContext]).
 *
 * Trzymane w warstwie presentation/common (a nie w domain), bo same w sobie
 * nie są częścią modelu domenowego – to tylko prezentacja agregatów,
 * z progami zaszytymi w UI. Progi można później wyciągnąć do configa
 * (np. RemoteConfig), bez zmiany kontraktu domenowego.
 *
 * Filozofia setu: motywować, nagradzać każdą "pierwszą" akcję, drabinka
 * progowa, plus prestiż za bycie w TOP rankingu (zarówno użytkowników,
 * jak i miejsc). Świadomie 5 grup po 2-3 odznaki, żeby user zawsze widział
 * "kolejny próg do zdobycia" niezależnie od tego, co już ma.
 *
 * Aktualne odznaki (15):
 *  - **Pierwsze kroki**:
 *    - [FIRST_PLACE] - dodaj 1. miejsce
 *    - [FIRST_REVIEW] - wystaw 1. opinię
 *  - **Drabinka miejsc**:
 *    - [EXPLORER] - 5 miejsc
 *    - [CARTOGRAPHER] - 15 miejsc
 *    - [PATHFINDER] - 30 miejsc
 *  - **Drabinka opinii**:
 *    - [REVIEWER] - 10 opinii
 *    - [CRITIC] - 25 opinii
 *    - [SENIOR_REVIEWER] - 50 opinii
 *  - **Wszechstronność**:
 *    - [COMMUNITY_PILLAR] - 5+ miejsc i 5+ opinii
 *    - [FAMILY_EXPERT] - 10+ miejsc i 20+ opinii
 *  - **Ranking użytkowników (TOP 100)**:
 *    - [LEADER_BRONZE] - 3. miejsce w rankingu
 *    - [LEADER_SILVER] - 2. miejsce w rankingu
 *    - [LEADER_GOLD] - 1. miejsce w rankingu
 *  - **Ranking miejsc (TOP 100)**:
 *    - [PLACE_TOP3] - twoje miejsce jest w pierwszej trójce
 *    - [PLACE_TOP1] - twoje miejsce jest na pierwszym miejscu
 */
enum class UserBadge(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    /** Krótki opis zasady przyznawania – pokazujemy go w info-dialogu. */
    val description: String
) {
    // ----- Pierwsze kroki -----
    FIRST_PLACE(
        label = "Pierwszy ślad",
        icon = Icons.Filled.AddLocationAlt,
        color = Color(0xFFBF360C), // dostępny pomarańcz – akcent "świeżego" startu
        description = "Dodaj swoje pierwsze miejsce na mapie"
    ),
    FIRST_REVIEW(
        label = "Pierwsza opinia",
        // ChatBubble (a nie RateReview jak REVIEWER) - w 24dp ikon-only
        // na karcie rankingu chcemy wizualnie odróżnić "pierwszą opinię"
        // od "10 opinii". RateReview to kartka z gwiazdką (jak REVIEWER),
        // ChatBubble to dymek - inne sylwetki, łatwo rozróżnialne.
        icon = Icons.Filled.ChatBubble,
        color = Color(0xFFBF360C),
        description = "Wystaw swoją pierwszą opinię"
    ),

    // ----- Drabinka miejsc -----
    EXPLORER(
        label = "Odkrywca",
        icon = Icons.Filled.Explore,
        color = Color(0xFF2E7D32),
        description = "Dodaj co najmniej 5 miejsc"
    ),
    CARTOGRAPHER(
        label = "Kartograf",
        icon = Icons.Filled.Map,
        color = Color(0xFF388E3C),
        description = "Dodaj co najmniej 15 miejsc"
    ),
    PATHFINDER(
        label = "Tropiciel",
        // Terrain (góry) lepiej oddaje "tropiciela" w dziczy niż AutoAwesome
        // (sparkles - generyczny "magia/wow"). Też wizualnie lepiej skaluje
        // się w 24dp ikon-only.
        icon = Icons.Filled.Terrain,
        color = Color(0xFF2E7D32),
        description = "Dodaj co najmniej 30 miejsc"
    ),

    // ----- Drabinka opinii -----
    REVIEWER(
        label = "Recenzent",
        icon = Icons.Filled.RateReview,
        color = Color(0xFF1565C0),
        description = "Wystaw co najmniej 10 opinii"
    ),
    CRITIC(
        label = "Krytyk",
        icon = Icons.Filled.Reviews,
        color = Color(0xFF1565C0),
        description = "Wystaw co najmniej 25 opinii"
    ),
    SENIOR_REVIEWER(
        label = "Wytrawny recenzent",
        // Stars (mnoga) sygnalizuje "mistrzostwo" (multiple stars),
        // jest wizualnie odróżnialny od pojedynczej Star, którą rezerwujemy
        // dla generycznego rating-icon w innych miejscach UI.
        icon = Icons.Filled.Stars,
        color = Color(0xFF0D47A1),
        description = "Wystaw co najmniej 50 opinii"
    ),

    // ----- Wszechstronność -----
    COMMUNITY_PILLAR(
        label = "Filar społeczności",
        icon = Icons.Filled.Groups,
        color = Color(0xFF6D4C41),
        description = "Dodaj 5+ miejsc oraz 5+ opinii"
    ),
    FAMILY_EXPERT(
        label = "Ekspert rodzinny",
        icon = Icons.Filled.Verified,
        color = Color(0xFF8E24AA),
        description = "Dodaj 10+ miejsc oraz 20+ opinii"
    ),

    // ----- Ranking użytkowników -----
    LEADER_BRONZE(
        label = "Brązowy lider",
        icon = Icons.Filled.MilitaryTech,
        color = Color(0xFF8D5524), // brąz
        description = "Zajmij 3. miejsce w rankingu użytkowników"
    ),
    LEADER_SILVER(
        label = "Srebrny lider",
        icon = Icons.Filled.MilitaryTech,
        color = Color(0xFF616161), // srebrny
        description = "Zajmij 2. miejsce w rankingu użytkowników"
    ),
    LEADER_GOLD(
        label = "Złoty lider",
        icon = Icons.Filled.EmojiEvents,
        color = Color(0xFF8A5A00), // złoty
        description = "Zajmij 1. miejsce w rankingu użytkowników"
    ),

    // ----- Ranking miejsc (twoje miejsca w TOP 100) -----
    // PLACE_TOP3: Whatshot (flame) - "twoje miejsce jest gorące, ludzie
    // je polecają". Wizualnie odróżnia się od PLACE_TOP1 (premium star) -
    // w 24dp ikon-only nie chcemy dwóch identycznych gwiazdek różniących
    // się tylko kolorem.
    PLACE_TOP3(
        label = "Lokalny faworyt",
        icon = Icons.Filled.Whatshot,
        color = Color(0xFF8D5524),
        description = "Twoje miejsce trafiło do TOP 3 najlepiej ocenianych"
    ),
    PLACE_TOP1(
        label = "Architekt zabawy",
        icon = Icons.Filled.WorkspacePremium,
        color = Color(0xFF8A5A00),
        description = "Twoje miejsce jest #1 w rankingu najlepiej ocenianych"
    )
}

/**
 * Kontekst rankingowy potrzebny do wyliczenia odznak zależnych od pozycji
 * w rankingach. Pola opcjonalne (null = "nie znamy / nie w TOP"), żeby
 * call-sites bez dostępu do rankingów (np. karta usera w [BadgesRow] na
 * RankingScreen) mogły dalej liczyć tylko intrinsic-badges przez wartość
 * domyślną [BadgeContext].
 *
 * @property userRank 1-based pozycja w rankingu TOP 100 użytkowników,
 *   lub null gdy poza top.
 * @property bestPlaceRank 1-based najlepsza pozycja jakiegokolwiek miejsca
 *   user-a w rankingu TOP 100 miejsc, lub null gdy żadne nie trafiło.
 */
data class BadgeContext(
    val userRank: Int? = null,
    val bestPlaceRank: Int? = null
)

/**
 * Lista odznak przyznanych [User] na podstawie aktualnych liczników i
 * (opcjonalnego) [context] z rankingami.
 *
 * Wynik jest deterministyczny dla tego samego stanu liczników i kontekstu –
 * kolejność w wyniku odpowiada kolejności w enumie [UserBadge].
 *
 * Bez [context] (default = pusty) zwraca tylko odznaki "intrinsic" - count-based
 * + first-time. Odznaki rankingowe (LEADER_*, PLACE_*) są przyznawane
 * tylko gdy odpowiednie pole [BadgeContext] zostanie wypełnione przez
 * call-site (najczęściej [com.kidzone.presentation.profile.ProfileViewModel],
 * który ma dostęp do rankingów).
 */
fun User.computeBadges(context: BadgeContext = BadgeContext()): List<UserBadge> {
    val list = mutableListOf<UserBadge>()

    // Pierwsze kroki - świadomie sprawdzamy >= 1, nie > 0, bo licznik
    // może być nigdy nie zinkrementowany dla bardzo świeżego konta.
    if (placesAddedCount >= 1) list += UserBadge.FIRST_PLACE
    if (reviewsCount >= 1) list += UserBadge.FIRST_REVIEW

    // Drabinka miejsc.
    if (placesAddedCount >= 5) list += UserBadge.EXPLORER
    if (placesAddedCount >= 15) list += UserBadge.CARTOGRAPHER
    if (placesAddedCount >= 30) list += UserBadge.PATHFINDER

    // Drabinka opinii.
    if (reviewsCount >= 10) list += UserBadge.REVIEWER
    if (reviewsCount >= 25) list += UserBadge.CRITIC
    if (reviewsCount >= 50) list += UserBadge.SENIOR_REVIEWER

    // Wszechstronność.
    if (placesAddedCount >= 5 && reviewsCount >= 5) list += UserBadge.COMMUNITY_PILLAR
    if (placesAddedCount >= 10 && reviewsCount >= 20) list += UserBadge.FAMILY_EXPERT

    // Ranking użytkowników. Dajemy odznakę wyłącznie za dokładnie #1/#2/#3 -
    // nie wszystkie naraz "do dołu" (1. nie dostaje też odznaki za 2. i 3.),
    // bo nagrody mają być rozróżnialne (gold > silver > bronze).
    when (context.userRank) {
        1 -> list += UserBadge.LEADER_GOLD
        2 -> list += UserBadge.LEADER_SILVER
        3 -> list += UserBadge.LEADER_BRONZE
        else -> { /* poza TOP 3 - brak odznaki rankingowej */ }
    }

    // Ranking miejsc - dajemy obie odznaki dla #1 (TOP 1 + TOP 3),
    // bo to "warstwowe" osiągnięcie i user widzi pełną drabinkę swojego
    // sukcesu na karcie odznak.
    val placeRank = context.bestPlaceRank
    if (placeRank != null) {
        if (placeRank in 1..3) list += UserBadge.PLACE_TOP3
        if (placeRank == 1) list += UserBadge.PLACE_TOP1
    }

    return list
}

/**
 * Pozioma rozjeżdżająca się lista chipów z odznakami. Używana w karcie
 * użytkownika w rankingu (gdzie pokazujemy tylko "co user osiągnął" bez
 * tooltipa - zachęta do wbicia tych samych odznak).
 *
 * Chipy są disabled (nieklikalne), bo to czysto informacyjny element.
 * Kolory ikon i tła pochodzą z [UserBadge.color] (półprzezroczyste tło,
 * żeby nie krzyczały na karcie).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgesRow(
    badges: List<UserBadge>,
    modifier: Modifier = Modifier
) {
    if (badges.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        badges.forEach { badge ->
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        text = badge.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = badge.icon,
                        contentDescription = null,
                        tint = badge.color,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = badge.color.copy(alpha = 0.10f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                    disabledLeadingIconContentColor = badge.color
                )
            )
        }
    }
}

/**
 * Kompaktowy rząd ikon-only odznak - kolorowe okrągłe kafelki, każda
 * wielkości [iconSize]. Bez tekstu. Używana na karcie użytkownika
 * w [com.kidzone.presentation.ranking.RankingScreen], gdzie chcemy
 * pokazać wszystkie zdobyte odznaki bez zajmowania miejsca pod opisy.
 *
 * Layout: [FlowRow] - przy 15 max odznakach o szerokości 24dp + 4dp gap
 * mieści się w 2 rzędach na typowym telefonie (~360dp content width).
 * Wartość [iconSize] jest wybrana świadomie: 24dp daje dobry balans
 * "widoczne, ale nie dominujące nad nazwą usera + statystykami".
 *
 * **Tooltip**: każda ikona owinięta jest w [TooltipBox] z [PlainTooltip]
 * pokazującym nazwę odznaki. Na touch screenie pojawia się po long-press,
 * na urządzeniach z myszką (Chromebook / Samsung DeX) po hover. Bez tego
 * user nie wie co znaczy ikona, bo nie pokazujemy labeli.
 *
 * Brak akcji on-click - karta usera w rankingu jako całość jest klikalna
 * (otwiera profil), więc dodatkowy click handler na ikonkach by tylko
 * blokował to globalne zachowanie. Long-press i tap są obsługiwane
 * niezależnie - tooltip nie konsumuje tap-u, tylko long-press.
 *
 * @param badges lista odznak DO WYŚWIETLENIA. Wywołujący decyduje
 *   o sortowaniu (zwykle [chronologicalOrder]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgesIconRow(
    badges: List<UserBadge>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    if (badges.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        badges.forEach { badge ->
            BadgeIconTile(badge = badge, iconSize = iconSize)
        }
    }
}

/**
 * Pojedyncza okrągła ikona odznaki + Material 3 [TooltipBox] z nazwą.
 *
 * Wyciągnięte z [BadgesIconRow] do osobnego composable, bo:
 *  - `rememberTooltipState()` musi być wywołane raz per ikona (nie raz
 *    dla całego rzędu),
 *  - keeps `forEach { ... }` w BadgesIconRow czytelnym (jedna linia).
 *
 * `TooltipDefaults.rememberPlainTooltipPositionProvider()` ustawia
 * tooltip nad/pod ikoną automatycznie wybierając lepszą pozycję względem
 * krawędzi ekranu - nie musimy się martwić o ikonki przy prawej krawędzi
 * karty.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgeIconTile(badge: UserBadge, iconSize: Dp) {
    val tooltipState = rememberTooltipState()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(text = badge.label)
            }
        },
        state = tooltipState
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .background(badge.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = badge.icon,
                // contentDescription daje a11y "label" dla TalkBacka,
                // jednoczesnie nie zaburzajac wizualnie kompaktowego rzedu.
                contentDescription = badge.label,
                tint = badge.color,
                modifier = Modifier.size(iconSize * 0.6f)
            )
        }
    }
}

/**
 * Sortuje listę odznak chronologicznie - od najwcześniej zdobytej do
 * najnowszej - na podstawie [com.kidzone.domain.model.User.badgeEarnedAt].
 *
 * Odznaki bez zarejestrowanego timestampu (legacy users sprzed wprowadzenia
 * tego pola, lub odznaki nigdy "nie wykryte" przez ProfileViewModel)
 * lądują na końcu, w porządku [UserBadge.ordinal] (czyli "naturalnym"
 * porządku zdefiniowanym w enumie - drabinka: pierwsze kroki, drabinka
 * miejsc, drabinka opinii, ...).
 *
 * Tiebreak: dwie odznaki zdobyte w tym samym millis (typowy przypadek -
 * user wbił wiele progów naraz, np. po imporcie / liczniki się
 * zaktualizowały) sortowane po enum.ordinal.
 */
fun chronologicalOrder(
    badges: List<UserBadge>,
    badgeEarnedAt: Map<String, Long>
): List<UserBadge> = badges.sortedWith(
    compareBy<UserBadge> { badgeEarnedAt[it.name] ?: Long.MAX_VALUE }
        .thenBy { it.ordinal }
)

/**
 * Pełen wiersz odznaki - duża okrągła ikona w kolorowym tle + nazwa + opis.
 * Używana w info-dialogu na ekranie profilu, żeby user widział wszystkie
 * dostępne odznaki i sposób ich zdobycia.
 *
 * @param highlighted true = już zdobyte (pełny kolor); false = jeszcze
 *   nie (lekko wyszarzona ikona, bez "kolorowego" tła).
 */
@Composable
fun BadgeRowItem(
    badge: UserBadge,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (highlighted) badge.color.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = badge.icon,
                contentDescription = null,
                tint = if (highlighted) {
                    badge.color
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = badge.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
