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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Reviews
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
        color = Color(0xFFEF6C00), // pomarańcz – akcent "świeżego" startu
        description = "Dodaj swoje pierwsze miejsce na mapie"
    ),
    FIRST_REVIEW(
        label = "Pierwsza opinia",
        icon = Icons.Filled.RateReview,
        color = Color(0xFFEF6C00),
        description = "Wystaw swoją pierwszą opinię"
    ),

    // ----- Drabinka miejsc -----
    EXPLORER(
        label = "Odkrywca",
        icon = Icons.Filled.Explore,
        color = Color(0xFF43A047),
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
        icon = Icons.Filled.AutoAwesome,
        color = Color(0xFF2E7D32),
        description = "Dodaj co najmniej 30 miejsc"
    ),

    // ----- Drabinka opinii -----
    REVIEWER(
        label = "Recenzent",
        icon = Icons.Filled.RateReview,
        color = Color(0xFF1E88E5),
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
        icon = Icons.Filled.Star,
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
        color = Color(0xFFB87333), // brąz
        description = "Zajmij 3. miejsce w rankingu użytkowników"
    ),
    LEADER_SILVER(
        label = "Srebrny lider",
        icon = Icons.Filled.MilitaryTech,
        color = Color(0xFF9E9E9E), // srebrny
        description = "Zajmij 2. miejsce w rankingu użytkowników"
    ),
    LEADER_GOLD(
        label = "Złoty lider",
        icon = Icons.Filled.EmojiEvents,
        color = Color(0xFFFFB300), // złoty
        description = "Zajmij 1. miejsce w rankingu użytkowników"
    ),

    // ----- Ranking miejsc (twoje miejsca w TOP 100) -----
    PLACE_TOP3(
        label = "Lokalny faworyt",
        icon = Icons.Filled.WorkspacePremium,
        color = Color(0xFFB87333),
        description = "Twoje miejsce trafiło do TOP 3 najlepiej ocenianych"
    ),
    PLACE_TOP1(
        label = "Architekt zabawy",
        icon = Icons.Filled.WorkspacePremium,
        color = Color(0xFFFFB300),
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
