package com.kidzone.presentation.common

import com.kidzone.utils.UiText

sealed interface ScreenState<out T> {
    data object Loading : ScreenState<Nothing>
    data class Content<T>(val data: T) : ScreenState<T>
    data class Error(val message: UiText? = null) : ScreenState<Nothing>
    data object Empty : ScreenState<Nothing>
}
