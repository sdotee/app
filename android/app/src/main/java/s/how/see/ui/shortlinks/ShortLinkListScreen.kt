package s.how.see.ui.shortlinks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import s.how.see.R
import s.how.see.data.local.db.entity.ShortLinkEntity
import s.how.see.ui.components.EmptyStateView
import s.how.see.ui.components.PaginationBar
import s.how.see.util.ClipboardUtil
import s.how.see.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShortLinkListScreen(
    onCreateClick: () -> Unit,
    onStatsClick: (String, String) -> Unit,
    onShowSnackbar: (String) -> Unit,
    viewModel: ShortLinkViewModel = hiltViewModel(),
) {
    val links by viewModel.links.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val batchDeleteProgress by viewModel.batchDeleteProgress.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var deleteTarget by remember { mutableStateOf<ShortLinkEntity?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    deleteTarget?.let { link ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteShortUrl(link.domain, link.slug)
                    deleteTarget = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.batch_delete_confirm, selectedIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    showBatchDeleteConfirm = false
                    val toDelete = links.filter { it.id in selectedIds }
                    viewModel.batchDelete(toDelete)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text(stringResource(R.string.selected_count, selectedIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (selectedIds.size == links.size) viewModel.deselectAll()
                            else viewModel.selectAll()
                        }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.select_all))
                        }
                        IconButton(
                            onClick = {
                                val selected = links.filter { it.id in selectedIds }
                                val allLinks = selected.joinToString("\n") { it.shortUrl }
                                ClipboardUtil.copyToClipboard(context, "Short URLs", allLinks)
                                onShowSnackbar(context.getString(R.string.links_copied, selected.size))
                            },
                            enabled = selectedIds.isNotEmpty(),
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.batch_copy_links), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { showBatchDeleteConfirm = true },
                            enabled = selectedIds.isNotEmpty() && batchDeleteProgress == null,
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.batch_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.short_links)) },
                    actions = {
                        if (links.isNotEmpty()) {
                            IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                                Icon(Icons.Filled.Checklist, contentDescription = stringResource(R.string.select))
                            }
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = onCreateClick) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_short_link))
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Batch delete progress
            batchDeleteProgress?.let { (current, total) ->
                LinearProgressIndicator(
                    progress = { current.toFloat() / total },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.batch_deleting, current, total),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (!selectionMode) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onSearch = {},
                            expanded = false,
                            onExpandedChange = {},
                            placeholder = { Text(stringResource(R.string.search)) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {}
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (links.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.Link,
                    title = stringResource(R.string.no_items),
                    description = stringResource(R.string.no_items_desc),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(links, key = { it.id }) { link ->
                        ShortLinkCard(
                            link = link,
                            selectionMode = selectionMode,
                            isSelected = link.id in selectedIds,
                            onToggleSelect = { viewModel.toggleSelection(link.id) },
                            onLongClick = {
                                if (!selectionMode) {
                                    viewModel.toggleSelectionMode()
                                    viewModel.toggleSelection(link.id)
                                }
                            },
                            onCopy = {
                                ClipboardUtil.copyToClipboard(context, "Short URL", link.shortUrl)
                                onShowSnackbar(context.getString(R.string.link_copied))
                            },
                            onStats = { onStatsClick(link.domain, link.slug) },
                            onDelete = { deleteTarget = link },
                        )
                    }
                }
                PaginationBar(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPreviousPage = { viewModel.setPage(currentPage - 1) },
                    onNextPage = { viewModel.setPage(currentPage + 1) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortLinkCard(
    link: ShortLinkEntity,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onCopy: () -> Unit,
    onStats: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() },
                onLongClick = onLongClick,
            ),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AnimatedVisibility(visible = selectionMode) {
                Row {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.shortUrl,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = link.targetUrl,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!link.title.isNullOrBlank()) {
                    Text(
                        text = link.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DateTimeUtil.formatTimestamp(link.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                if (!selectionMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(onClick = onCopy) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.copy_link), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onStats) {
                            Icon(Icons.Filled.BarChart, contentDescription = stringResource(R.string.view_stats))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
