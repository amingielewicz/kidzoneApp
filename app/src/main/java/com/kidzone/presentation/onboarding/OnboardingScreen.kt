package com.kidzone.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidzone.ui.theme.BrandBlue
import com.kidzone.ui.theme.BrandGreen
import com.kidzone.ui.theme.BrandYellow
import com.kidzone.ui.theme.Poppins
import kotlinx.coroutines.launch

/**
 * Model danych pojedynczego slajdu onboardingu.
 */
private data class OnboardingPage(
    val icon: ImageVector,
    val iconTint: Color,
    val title: String,
    val description: String,
    val backgroundColor: Color
)

/**
 * 4 slajdy onboardingu kidZone:
 *  1. Powitanie – czym jest apka (BrandBlue)
 *  2. Odkrywaj miejsca na mapie (BrandGreen)
 *  3. Przegladaj liste i ranking
 *  4. Dziel sie opiniami (BrandYellow)
 */
private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Default.ChildCare,
        iconTint = BrandBlue,
        title = "Witaj w kidZone!",
        description = "Znajdź najlepsze miejsca dla dzieci w Twojej okolicy. Place zabaw, parki, atrakcje \u2013 wszystko w jednym miejscu.",
        backgroundColor = Color(0xFFE3F2FD) // light blue tint
    ),
    OnboardingPage(
        icon = Icons.Default.Map,
        iconTint = BrandGreen,
        title = "Odkrywaj na mapie",
        description = "Przeglądaj miejsca na interaktywnej mapie. Filtruj po kategorii, odległości i ocenach innych rodziców.",
        backgroundColor = Color(0xFFE8F5E9) // light green tint
    ),
    OnboardingPage(
        icon = Icons.AutoMirrored.Filled.List,
        iconTint = BrandBlue,
        title = "Porównuj miejsca",
        description = "Użyj listy, gdy chcesz szybko filtrować wyniki. " +
            "Ranking pokaże najlepiej oceniane miejsca i najbardziej aktywnych użytkowników.",
        backgroundColor = Color(0xFFE3F2FD) // light blue tint
    ),
    OnboardingPage(
        icon = Icons.Filled.RateReview,
        iconTint = BrandYellow,
        title = "Dziel się opiniami",
        description = "Dodawaj miejsca, wystawiaj opinie i pomagaj innym rodzicom w wyborze. Zdobywaj odznaki za aktywność!",
        backgroundColor = Color(0xFFFFF8E1) // light yellow tint
    )
)

/**
 * Ekran onboardingu – wyswietlany po pierwszym zalogowaniu.
 *
 * 3 slajdy w HorizontalPager z dot indicators na dole.
 * User moze przesuwac palcem lub kliknac "Dalej" / "Zaczynamy!".
 * "Pomin" na kazdym slajdzie pozwala pominac calosc.
 *
 * @param onComplete wywolywane po zakonczeniu/pominieniu – nawiguje do Main.
 * @param onStarted wywolywane raz przy pierwszym renderze (Analytics: onboarding_started).
 * @param onSkipped wywolywane jesli user kliknal "Pomin" (Analytics: onboarding_skipped).
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onStarted: () -> Unit = {},
    onSkipped: (lastPage: Int) -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        onStarted()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPageContent(onboardingPages[page])
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dot indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(onboardingPages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        label = "dot_width"
                    )
                    val color by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                        label = "dot_color"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action button
            val isLastPage = pagerState.currentPage == onboardingPages.size - 1
            Button(
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isLastPage) "Zaczynamy!" else "Dalej",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Skip button (hidden on last page)
            if (!isLastPage) {
                TextButton(onClick = {
                    onSkipped(pagerState.currentPage)
                    onComplete()
                }) {
                    Text(
                        text = "Pomiń",
                        fontFamily = Poppins,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Spacer to keep layout stable
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(page.backgroundColor)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Big icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(page.iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = page.iconTint,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = page.title,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            fontFamily = Poppins,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            lineHeight = 24.sp
        )

        // Extra space at the bottom to avoid overlap with buttons
        Spacer(modifier = Modifier.height(120.dp))
    }
}
