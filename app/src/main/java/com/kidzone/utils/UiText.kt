package com.kidzone.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Przechowuje tekst do wyświetlenia w UI – albo jako ID zasobu (z argumentami),
 * albo jako surowy String. Pozwala ViewModelom pozostać niezależnymi od Contextu,
 * a jednocześnie wspierać lokalizację.
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText()

    @Composable
    @Suppress("SpreadOperator")
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
        }
    }

    @Suppress("SpreadOperator")
    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
        }
    }
}
