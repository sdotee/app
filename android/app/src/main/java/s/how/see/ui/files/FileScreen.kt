package s.how.see.ui.files

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import s.how.see.R
import s.how.see.data.local.db.entity.UploadedFileEntity
import s.how.see.data.remote.model.Result
import s.how.see.ui.components.EmptyStateView
import s.how.see.ui.components.PaginationBar
import s.how.see.util.ClipboardUtil
import s.how.see.util.DateTimeUtil
import s.how.see.util.LinkDisplayType
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileScreen(
    onShowSnackbar: (String) -> Unit,
    viewModel: FileViewModel = hiltViewModel(),
) {
    val files by viewModel.files.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val uploadResult by viewModel.uploadResult.collectAsStateWithLifecycle()
    val fileLinkDisplayTypeStr by viewModel.fileLinkDisplayType.collectAsStateWithLifecycle()
    val linkDisplayType = LinkDisplayType.fromString(fileLinkDisplayTypeStr)
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val batchDeleteProgress by viewModel.batchDeleteProgress.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var deleteTarget by remember { mutableStateOf<UploadedFileEntity?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    deleteTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFile(file.hash)
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
                    val toDelete = files.filter { it.id in selectedIds }
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

    // Pre-resolve strings to avoid lint warnings
    val strUploadSuccess = stringResource(R.string.upload_success)
    val strLinkCopied = stringResource(R.string.link_copied)
    val strCameraPermDenied = stringResource(R.string.camera_permission_denied)
    val strLinksCopied = stringResource(R.string.links_copied, selectedIds.size)

    // File picker
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadFile(it) }
    }

    // Photo picker (gallery)
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.uploadFile(it) }
    }

    // Camera photo capture
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraAction by remember { mutableStateOf<String?>(null) }

    val cameraPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { viewModel.uploadFile(it) }
        }
    }

    val cameraVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) {
            photoUri?.let { viewModel.uploadFile(it) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
            when (pendingCameraAction) {
                "photo" -> {
                    val file = File(cameraDir, "photo_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    photoUri = uri
                    cameraPhotoLauncher.launch(uri)
                }
                "video" -> {
                    val file = File(cameraDir, "video_${System.currentTimeMillis()}.mp4")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    photoUri = uri
                    cameraVideoLauncher.launch(uri)
                }
            }
            pendingCameraAction = null
        } else {
            onShowSnackbar(strCameraPermDenied)
            pendingCameraAction = null
        }
    }

    fun launchCamera(action: String) {
        val hasPermission = context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
            when (action) {
                "photo" -> {
                    val file = File(cameraDir, "photo_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    photoUri = uri
                    cameraPhotoLauncher.launch(uri)
                }
                "video" -> {
                    val file = File(cameraDir, "video_${System.currentTimeMillis()}.mp4")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    photoUri = uri
                    cameraVideoLauncher.launch(uri)
                }
            }
        } else {
            pendingCameraAction = action
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) { viewModel.loadDomains() }

    LaunchedEffect(uploadResult) {
        when (val result = uploadResult) {
            is Result.Success -> {
                ClipboardUtil.copyToClipboard(context, "File URL", result.data)
                onShowSnackbar(strUploadSuccess)
                viewModel.clearUploadResult()
            }
            is Result.Error -> {
                onShowSnackbar(result.message)
                viewModel.clearUploadResult()
            }
            else -> {}
        }
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
                            if (selectedIds.size == files.size) viewModel.deselectAll()
                            else viewModel.selectAll()
                        }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.select_all))
                        }
                        // Batch copy links
                        IconButton(
                            onClick = {
                                val selected = files.filter { it.id in selectedIds }
                                val allLinks = selected.joinToString("\n") { linkDisplayType.formatted(it) }
                                ClipboardUtil.copyToClipboard(context, "File Links", allLinks)
                                onShowSnackbar(context.getString(R.string.links_copied, selected.size))
                            },
                            enabled = selectedIds.isNotEmpty(),
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.batch_copy_links), tint = MaterialTheme.colorScheme.primary)
                        }
                        // Batch delete
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
                    title = { Text(stringResource(R.string.files)) },
                    actions = {
                        if (files.isNotEmpty()) {
                            IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                                Icon(Icons.Filled.Checklist, contentDescription = stringResource(R.string.select))
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // Batch delete progress
            batchDeleteProgress?.let { (current, total) ->
                LinearProgressIndicator(
                    progress = { current.toFloat() / total },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.batch_deleting, current, total),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            // Upload buttons (hidden in selection mode)
            if (!selectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { filePicker.launch("*/*") },
                        modifier = Modifier.weight(1f),
                        enabled = !isUploading,
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.select_file))
                    }
                    OutlinedButton(
                        onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                        modifier = Modifier.weight(1f),
                        enabled = !isUploading,
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.select_image))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = { launchCamera("photo") },
                        modifier = Modifier.weight(1f),
                        enabled = !isUploading,
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.take_photo))
                    }
                    FilledTonalButton(
                        onClick = { launchCamera("video") },
                        modifier = Modifier.weight(1f),
                        enabled = !isUploading,
                    ) {
                        Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.record_video))
                    }
                }

                if (isUploading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.uploading),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (files.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.Folder,
                    title = stringResource(R.string.no_items),
                    description = stringResource(R.string.no_items_desc),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(files, key = { it.id }) { file ->
                        FileCard(
                            file = file,
                            selectionMode = selectionMode,
                            isSelected = file.id in selectedIds,
                            onToggleSelect = { viewModel.toggleSelection(file.id) },
                            onLongClick = {
                                if (!selectionMode) {
                                    viewModel.toggleSelectionMode()
                                    viewModel.toggleSelection(file.id)
                                }
                            },
                            onCopyLink = {
                                val formattedLink = linkDisplayType.formatted(file)
                                ClipboardUtil.copyToClipboard(context, "File Link", formattedLink)
                                onShowSnackbar(strLinkCopied)
                            },
                            onOpenBrowser = {
                                val pageUrl = file.page ?: file.url
                                context.startActivity(Intent(Intent.ACTION_VIEW, pageUrl.toUri()))
                            },
                            onDelete = { deleteTarget = file },
                            linkDisplayType = linkDisplayType,
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
private fun FileCard(
    file: UploadedFileEntity,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onCopyLink: () -> Unit,
    onOpenBrowser: () -> Unit,
    onDelete: () -> Unit,
    linkDisplayType: LinkDisplayType,
) {
    val fileType = s.how.see.util.LinkFormatter.fileType(file.filename)
    val isImage = fileType == s.how.see.util.LinkFormatter.FileType.IMAGE
    val isVideo = fileType == s.how.see.util.LinkFormatter.FileType.VIDEO
    val isAudio = fileType == s.how.see.util.LinkFormatter.FileType.AUDIO
    val isMedia = isImage || isVideo

    val context = LocalContext.current

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
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AnimatedVisibility(visible = selectionMode) {
                Row {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            // Thumbnail area
            if (isMedia) {
                val imageModel = if (isVideo) {
                    coil3.request.ImageRequest.Builder(context)
                        .data(file.url)
                        .decoderFactory(coil3.video.VideoFrameDecoder.Factory())
                        .build()
                } else {
                    file.url
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = file.filename,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_file_fallback),
                    fallback = painterResource(R.drawable.ic_file_fallback),
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                val fallbackIcon = when {
                    isAudio -> Icons.Filled.AudioFile
                    else -> Icons.Filled.InsertDriveFile
                }
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = file.filename, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = DateTimeUtil.formatFileSize(file.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = DateTimeUtil.formatTimestamp(file.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                if (linkDisplayType != LinkDisplayType.DIRECT_LINK && linkDisplayType != LinkDisplayType.SHARE_PAGE) {
                    Text(
                        text = linkDisplayType.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }

                if (!selectionMode) {
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = onCopyLink) { Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.copy_link), tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = onOpenBrowser) { Icon(Icons.Filled.OpenInBrowser, contentDescription = stringResource(R.string.open_in_browser)) }
                        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}
