package com.daniel.silo.ui.links

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.daniel.silo.R
import com.daniel.silo.domain.model.Link
import com.daniel.silo.domain.model.SiloCollection
import com.daniel.silo.ui.LinksUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinksScreen(
    uiState: LinksUiState,
    onQueryChange: (String) -> Unit,
    onSelectCollection: (Long?) -> Unit,
    onDeleteLink: (Long) -> Unit,
    onMoveLink: (Long, Long?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(Modifier.fillMaxSize().then(modifier)) {

            SearchBar(
                query = uiState.query,
                onQueryChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCollectionId == null,
                        onClick = { onSelectCollection(null) },
                        label = { Text(stringResource(R.string.all_collections)) }
                    )
                }
                items(uiState.collections) { col ->
                    FilterChip(
                        selected = uiState.selectedCollectionId == col.id,
                        onClick = { onSelectCollection(col.id) },
                        label = { Text(col.name) }
                    )
                }
            }

            if (uiState.pendingCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.pending_sync, uiState.pendingCount),
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (uiState.isLoading && uiState.links.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.links.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_links), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(uiState.links, key = { it.id }) { link ->
                        LinkCard(
                            link = link,
                            collections = uiState.collections,
                            onDelete = { onDeleteLink(link.id) },
                            onMove = { colId, colName -> onMoveLink(link.id, colId, colName) }
                        )
                    }
                }
            }
        }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text(stringResource(R.string.links)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkCard(
    link: Link,
    collections: List<SiloCollection>,
    onDelete: () -> Unit,
    onMove: (Long?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = link.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = link.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!link.collectionName.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text(link.collectionName, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
                if (link.pendingSync || link.pendingDelete) {
                    Icon(Icons.Default.CloudOff, contentDescription = null,
                        modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { uriHandler.openUri(link.url) }) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Abrir")
                }
                IconButton(onClick = { showMoveSheet = true }) {
                    Icon(Icons.Default.DriveFileMove, contentDescription = stringResource(R.string.move_to))
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.confirm_delete_link)) },
            text = { Text(link.title) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showMoveSheet) {
        MoveToCollectionSheet(
            collections = collections,
            currentCollectionId = link.collectionId,
            onSelect = { colId, colName -> onMove(colId, colName); showMoveSheet = false },
            onDismiss = { showMoveSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToCollectionSheet(
    collections: List<SiloCollection>,
    currentCollectionId: Long?,
    onSelect: (Long?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.move_to),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        val items = listOf<Pair<Long?, String>>(null to "Sin colección") +
                collections.map { it.id to it.name }
        items.forEach { (id, name) ->
            ListItem(
                headlineContent = { Text(name) },
                leadingContent = {
                    if (id == currentCollectionId)
                        Icon(Icons.Default.Check, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(id, if (id == null) null else name) }
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
