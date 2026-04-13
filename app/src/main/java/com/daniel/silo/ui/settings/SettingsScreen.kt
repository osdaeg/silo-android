package com.daniel.silo.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.daniel.silo.R
import com.daniel.silo.data.local.SiloSettings

@Composable
fun SettingsScreen(
    settings: SiloSettings,
    onSave: (String, String) -> Unit,
    onSyncNow: () -> Unit,
    syncMessage: String? = null,
    onClearSyncMessage: () -> Unit = {}
) {
    var serverUrl by remember(settings.serverUrl) { mutableStateOf(settings.serverUrl) }
    var apiToken  by remember(settings.apiToken)  { mutableStateOf(settings.apiToken) }
    var tokenVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text(stringResource(R.string.server_url)) },
            placeholder = { Text("http://192.168.1.10:7123") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = apiToken,
            onValueChange = { apiToken = it },
            label = { Text(stringResource(R.string.api_token)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { tokenVisible = !tokenVisible }) {
                    Icon(
                        if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            }
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { onSave(serverUrl, apiToken) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save))
        }

        OutlinedButton(
            onClick = onSyncNow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.sync_now))
        }

        syncMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (msg.startsWith("Error"))
                        MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(12.dp),
                    color = if (msg.startsWith("Error"))
                        MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(3000)
                onClearSyncMessage()
            }
        }
    }
}
