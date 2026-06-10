package com.kidzone.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Pull-to-refresh wrapper z domyślnym Material3 indicatorem.
 * Logo kidZone jest widoczne w TopAppBar, indicator jest standardowy kółeczko.
 *
 * Usage:
 * ```
 * KidZonePullRefresh(
 *     isRefreshing = viewModel.isRefreshing,
 *     onRefresh = { viewModel.refresh() }
 * ) {
 *     LazyColumn { ... }
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidZonePullRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        content()
    }
}
