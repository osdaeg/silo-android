package com.daniel.silo.ui.links

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daniel.silo.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLinkScreen(
    state: AddLinkUiState,
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCollectionChange: (Long?) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(state.saved, state.savedOffline) {
        if (state.saved || state.savedOffline) onBack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_link)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = state.url, onValueChange = onUrlChange,
                label = { Text("URL") }, placeholder = { Text(stringResource(R.string.url_hint)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = state.title, onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.title_hint)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                trailingIcon = { if (state.isFetchingTitle) CircularProgressIndicator(Modifier.size(20.dp)) })
            OutlinedTextField(value = state.description, onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.description_hint)) },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
            var expanded by remember { mutableStateOf(false) }
            val selectedName = state.collections.find { it.id == state.selectedCollectionId }?.name
                ?: stringResource(R.string.collection_hint)
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(value = selectedName, onValueChange = {}, readOnly = true,
                    label = { Text(stringResource(R.string.collection_hint)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Sin coleccion") },
                        onClick = { onCollectionChange(null); expanded = false })
                    state.collections.forEach { col ->
                        DropdownMenuItem(text = { Text(col.name) },
                            onClick = { onCollectionChange(col.id); expanded = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onSave, enabled = state.url.isNotBlank() && !state.isSaving,
                modifier = Modifier.fillMaxWidth()) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.save))
            }
        }
    }
}
