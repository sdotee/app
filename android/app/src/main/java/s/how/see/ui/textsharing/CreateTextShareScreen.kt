package s.how.see.ui.textsharing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import s.how.see.R
import s.how.see.data.remote.model.Result
import s.how.see.ui.components.DomainSelector
import s.how.see.ui.components.TagChipGroup
import s.how.see.util.ClipboardUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTextShareScreen(
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    editDomain: String? = null,
    editSlug: String? = null,
    viewModel: TextShareViewModel = hiltViewModel(),
) {
    val domains by viewModel.domains.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val createResult by viewModel.createResult.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isEdit = editSlug != null

    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var selectedDomain by rememberSaveable { mutableStateOf(editDomain ?: "") }
    var textType by rememberSaveable { mutableStateOf("plain_text") }
    var customSlug by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var selectedTagIds by rememberSaveable { mutableStateOf(setOf<Int>()) }

    val textTypes = listOf(
        "plain_text" to R.string.text_type_plain,
        "source_code" to R.string.text_type_code,
        "markdown" to R.string.text_type_markdown,
    )

    LaunchedEffect(Unit) {
        viewModel.loadDomains()
        viewModel.loadTags()
    }

    LaunchedEffect(domains) {
        if (selectedDomain.isBlank() && domains.isNotEmpty()) {
            selectedDomain = domains.first()
        }
    }

    LaunchedEffect(createResult) {
        when (val result = createResult) {
            is Result.Success -> {
                ClipboardUtil.copyToClipboard(context, "Text Share URL", result.data)
                onShowSnackbar(context.getString(R.string.link_copied))
                viewModel.clearCreateResult()
                onBack()
            }
            is Result.Error -> {
                onShowSnackbar(result.message)
                viewModel.clearCreateResult()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isEdit) R.string.edit_text_share else R.string.create_text_share)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.title)) },
                placeholder = { Text(stringResource(R.string.title_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                textTypes.forEachIndexed { index, (type, labelRes) ->
                    SegmentedButton(
                        selected = textType == type,
                        onClick = { textType = type },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = textTypes.size),
                    ) { Text(stringResource(labelRes)) }
                }
            }

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.content_hint)) },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                textStyle = if (textType == "source_code") {
                    MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                } else {
                    MaterialTheme.typography.bodyMedium
                },
            )

            if (domains.isNotEmpty()) {
                DomainSelector(
                    domains = domains,
                    selectedDomain = selectedDomain,
                    onDomainSelected = { selectedDomain = it },
                    label = stringResource(R.string.domain),
                )
            }

            if (!isEdit) {
                OutlinedTextField(
                    value = customSlug,
                    onValueChange = { customSlug = it },
                    label = { Text(stringResource(R.string.custom_slug)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )

                if (tags.isNotEmpty()) {
                    Text(stringResource(R.string.tags), style = MaterialTheme.typography.titleSmall)
                    TagChipGroup(
                        tags = tags,
                        selectedTagIds = selectedTagIds,
                        onTagToggle = { id ->
                            selectedTagIds = if (id in selectedTagIds) selectedTagIds - id
                            else if (selectedTagIds.size < 5) selectedTagIds + id
                            else selectedTagIds
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isEdit) {
                        viewModel.updateTextShare(editDomain!!, editSlug, content, title.ifBlank { "Untitled" })
                        onBack()
                    } else {
                        viewModel.createTextShare(
                            content = content, title = title, domain = selectedDomain,
                            customSlug = customSlug, textType = textType, password = password,
                            expireAt = null, tagIds = selectedTagIds.toList(),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = content.isNotBlank() && !isLoading,
            ) {
                Text(stringResource(if (isEdit) R.string.update else R.string.create))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
