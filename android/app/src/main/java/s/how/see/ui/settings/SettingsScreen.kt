package s.how.see.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import s.how.see.BuildConfig
import s.how.see.R
import s.how.see.ui.components.DomainSelector
import s.how.see.util.LinkDisplayType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onSignedOut: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val validationState by viewModel.validationState.collectAsStateWithLifecycle()
    val linkDomains by viewModel.linkDomains.collectAsStateWithLifecycle()
    val textDomains by viewModel.textDomains.collectAsStateWithLifecycle()
    val fileDomains by viewModel.fileDomains.collectAsStateWithLifecycle()
    val defaultLinkDomain by viewModel.defaultLinkDomain.collectAsStateWithLifecycle()
    val defaultTextDomain by viewModel.defaultTextDomain.collectAsStateWithLifecycle()
    val defaultFileDomain by viewModel.defaultFileDomain.collectAsStateWithLifecycle()
    val fileLinkDisplayType by viewModel.fileLinkDisplayType.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var baseUrlInput by rememberSaveable { mutableStateOf(baseUrl) }
    var apiKeyInput by rememberSaveable { mutableStateOf(viewModel.getApiKey()) }
    var showPassword by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    val clipboardManager = context.getSystemService(android.content.ClipboardManager::class.java)
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadDomains() }

    LaunchedEffect(validationState) {
        when (validationState) {
            is SettingsViewModel.ValidationState.Valid ->
                onShowSnackbar("API key is valid")
            is SettingsViewModel.ValidationState.Invalid ->
                onShowSnackbar((validationState as SettingsViewModel.ValidationState.Invalid).message)
            else -> {}
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.sign_out)) },
            text = { Text(stringResource(R.string.sign_out_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.signOut()
                    showSignOutDialog = false
                    apiKeyInput = ""
                    onSignedOut()
                }) { Text(stringResource(R.string.sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_history)) },
            text = { Text(stringResource(R.string.clear_history_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearDialog = false
                    onShowSnackbar("History cleared")
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Base URL
            Text(stringResource(R.string.base_url), style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = baseUrlInput,
                onValueChange = { baseUrlInput = it },
                label = { Text(stringResource(R.string.base_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = { viewModel.setBaseUrl(baseUrlInput) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }

            HorizontalDivider()

            // API Key
            Text(stringResource(R.string.api_key), style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text(stringResource(R.string.api_key_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                            )
                        }
                        IconButton(onClick = {
                            clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString()?.let { apiKeyInput = it }
                        }) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = stringResource(R.string.paste))
                        }
                    }
                },
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.validateApiKey(apiKeyInput) },
                    enabled = apiKeyInput.isNotBlank() && validationState !is SettingsViewModel.ValidationState.Validating,
                    modifier = Modifier.weight(1f),
                ) {
                    if (validationState is SettingsViewModel.ValidationState.Validating) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                    } else {
                        Text(stringResource(R.string.validate))
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.saveApiKey(apiKeyInput) },
                    enabled = apiKeyInput.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.save)) }
            }

            HorizontalDivider()

            // Default Domains
            if (linkDomains.isNotEmpty()) {
                Text(stringResource(R.string.default_link_domain), style = MaterialTheme.typography.titleSmall)
                DomainSelector(
                    domains = linkDomains,
                    selectedDomain = defaultLinkDomain ?: linkDomains.firstOrNull() ?: "",
                    onDomainSelected = { viewModel.setDefaultLinkDomain(it) },
                    label = stringResource(R.string.default_link_domain),
                )
            }
            if (textDomains.isNotEmpty()) {
                DomainSelector(
                    domains = textDomains,
                    selectedDomain = defaultTextDomain ?: textDomains.firstOrNull() ?: "",
                    onDomainSelected = { viewModel.setDefaultTextDomain(it) },
                    label = stringResource(R.string.default_text_domain),
                )
            }
            if (fileDomains.isNotEmpty()) {
                DomainSelector(
                    domains = fileDomains,
                    selectedDomain = defaultFileDomain ?: fileDomains.firstOrNull() ?: "",
                    onDomainSelected = { viewModel.setDefaultFileDomain(it) },
                    label = stringResource(R.string.default_file_domain),
                )
            }

            HorizontalDivider()

            // File Link Format
            Text(stringResource(R.string.file_link_format), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.file_link_format_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            var linkFormatExpanded by remember { mutableStateOf(false) }
            val currentDisplayType = LinkDisplayType.fromString(fileLinkDisplayType)
            ExposedDropdownMenuBox(
                expanded = linkFormatExpanded,
                onExpandedChange = { linkFormatExpanded = it },
            ) {
                OutlinedTextField(
                    value = currentDisplayType.label,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = linkFormatExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = linkFormatExpanded,
                    onDismissRequest = { linkFormatExpanded = false },
                ) {
                    LinkDisplayType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                viewModel.setFileLinkDisplayType(type.name)
                                linkFormatExpanded = false
                            },
                        )
                    }
                }
            }

            HorizontalDivider()

            // Theme
            Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleSmall)
            val themeModes = listOf("system" to R.string.theme_system, "light" to R.string.theme_light, "dark" to R.string.theme_dark)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeModes.forEachIndexed { index, (mode, labelRes) ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themeModes.size),
                    ) { Text(stringResource(labelRes)) }
                }
            }

            // Dynamic Color
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.dynamic_color), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.dynamic_color_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = dynamicColor, onCheckedChange = { viewModel.setDynamicColor(it) })
            }

            HorizontalDivider()

            // Clear History
            OutlinedButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.clear_history)) }

            // Sign Out
            if (hasApiKey) {
                Button(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.sign_out))
                }
            }

            // About
            HorizontalDivider()
            Text(stringResource(R.string.about), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Legal Links
            Text(
                text = stringResource(R.string.privacy_policy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://s.ee/privacy/".toUri()))
                    }
                    .padding(vertical = 4.dp),
            )
            Text(
                text = stringResource(R.string.terms_of_service),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://s.ee/terms/".toUri()))
                    }
                    .padding(vertical = 4.dp),
            )
            Text(
                text = stringResource(R.string.acceptable_use_policy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://s.ee/aup/".toUri()))
                    }
                    .padding(vertical = 4.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
