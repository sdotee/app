package s.how.see.ui.shortlinks

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
fun CreateShortLinkScreen(
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    editDomain: String? = null,
    editSlug: String? = null,
    editTargetUrl: String? = null,
    editTitle: String? = null,
    viewModel: ShortLinkViewModel = hiltViewModel(),
) {
    val domains by viewModel.domains.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val createResult by viewModel.createResult.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isEdit = editSlug != null

    var targetUrl by rememberSaveable { mutableStateOf(editTargetUrl ?: "") }
    var selectedDomain by rememberSaveable { mutableStateOf(editDomain ?: "") }
    var customSlug by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf(editTitle ?: "") }
    var password by rememberSaveable { mutableStateOf("") }
    var expirationRedirectUrl by rememberSaveable { mutableStateOf("") }
    var selectedTagIds by rememberSaveable { mutableStateOf(setOf<Int>()) }

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
                ClipboardUtil.copyToClipboard(context, "Short URL", result.data)
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
                title = { Text(stringResource(if (isEdit) R.string.edit_short_link else R.string.create_short_link)) },
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
                value = targetUrl,
                onValueChange = { targetUrl = it },
                label = { Text(stringResource(R.string.target_url)) },
                placeholder = { Text(stringResource(R.string.target_url_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
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
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            if (!isEdit) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )

                OutlinedTextField(
                    value = expirationRedirectUrl,
                    onValueChange = { expirationRedirectUrl = it },
                    label = { Text(stringResource(R.string.expiration_redirect_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                if (tags.isNotEmpty()) {
                    Text(stringResource(R.string.tags), style = MaterialTheme.typography.titleSmall)
                    TagChipGroup(
                        tags = tags,
                        selectedTagIds = selectedTagIds,
                        onTagToggle = { id ->
                            selectedTagIds = if (id in selectedTagIds) selectedTagIds - id else selectedTagIds + id
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isEdit) {
                        viewModel.updateShortUrl(editDomain!!, editSlug, targetUrl, title)
                        onBack()
                    } else {
                        viewModel.createShortUrl(
                            targetUrl = targetUrl, domain = selectedDomain,
                            customSlug = customSlug, title = title, password = password,
                            expireAt = null, expirationRedirectUrl = expirationRedirectUrl,
                            tagIds = selectedTagIds.toList(),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = targetUrl.isNotBlank() && selectedDomain.isNotBlank() && !isLoading,
            ) {
                Text(stringResource(if (isEdit) R.string.update else R.string.create))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
