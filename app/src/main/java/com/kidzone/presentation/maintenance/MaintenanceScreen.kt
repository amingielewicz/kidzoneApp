package com.kidzone.presentation.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidzone.R

/**
 * 🎯 Odpowiedzialności:
 * - Wyświetlanie komunikatu o trwającej przerwie technicznej lub braku wsparcia wersji.
 * - Blokowanie dostępu do aplikacji w sytuacjach awaryjnych serwera.
 *
 * 📥 Wejście:
 * - [message] komunikat przekazany z serwera (Remote Config).
 *
 * 📤 Wyjście:
 * - Wyjście z aplikacji lub przekierowanie do sklepu.
 */
@Composable
fun MaintenanceScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Construction,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.maintenance_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            // Wiadomość z Remote Config może być już zlokalizowana po stronie serwera,
            // ale jeśli fetch zawiedzie, używamy domyślnego klucza (obsłużone w VM/Service).
            text = message.ifBlank { stringResource(R.string.maintenance_message) },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
