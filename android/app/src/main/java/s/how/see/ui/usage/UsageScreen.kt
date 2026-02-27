package s.how.see.ui.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import s.how.see.R
import s.how.see.ui.components.LoadingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageScreen(
    onBack: () -> Unit,
    viewModel: UsageViewModel = hiltViewModel(),
) {
    val usage by viewModel.usage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isAvailable by viewModel.isAvailable.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadUsage() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.usage)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (isLoading) {
            LoadingOverlay(modifier = Modifier.padding(padding))
        } else if (!isAvailable) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Usage stats are not available for this instance.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            usage?.let { u ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StorageCard(
                        fileCount = u.fileCount,
                        storageUsageMb = u.storageUsageMb,
                        storageUsageLimitMb = u.storageUsageLimitMb,
                    )
                    UsageCard(
                        title = stringResource(R.string.api_calls),
                        dayCount = u.apiCountDay, dayLimit = u.apiCountDayLimit,
                        monthCount = u.apiCountMonth, monthLimit = u.apiCountMonthLimit,
                    )
                    UsageCard(
                        title = stringResource(R.string.links_created),
                        dayCount = u.linkCountDay, dayLimit = u.linkCountDayLimit,
                        monthCount = u.linkCountMonth, monthLimit = u.linkCountMonthLimit,
                    )
                    UsageCard(
                        title = stringResource(R.string.texts_created),
                        dayCount = u.textCountDay, dayLimit = u.textCountDayLimit,
                        monthCount = u.textCountMonth, monthLimit = u.textCountMonthLimit,
                    )
                    UsageCard(
                        title = stringResource(R.string.uploads),
                        dayCount = u.uploadCountDay, dayLimit = u.uploadCountDayLimit,
                        monthCount = u.uploadCountMonth, monthLimit = u.uploadCountMonthLimit,
                    )
                    UsageCard(
                        title = stringResource(R.string.qr_codes),
                        dayCount = u.qrcodeCountDay, dayLimit = u.qrcodeCountDayLimit,
                        monthCount = u.qrcodeCountMonth, monthLimit = u.qrcodeCountMonthLimit,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun formatLimit(limit: Int): String {
    return if (limit == -1) stringResource(R.string.unlimited) else limit.toString()
}

@Composable
private fun UsageCard(
    title: String,
    dayCount: Int, dayLimit: Int,
    monthCount: Int, monthLimit: Int,
) {
    val todayLabel = stringResource(R.string.today_label)
    val monthLabel = stringResource(R.string.month_label)
    val dayLimitStr = formatLimit(dayLimit)
    val monthLimitStr = formatLimit(monthLimit)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = todayLabel, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "$dayCount / $dayLimitStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (dayLimit > 0) {
                LinearProgressIndicator(
                    progress = { (dayCount.toFloat() / dayLimit).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = monthLabel, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "$monthCount / $monthLimitStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (monthLimit > 0) {
                LinearProgressIndicator(
                    progress = { (monthCount.toFloat() / monthLimit).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun StorageCard(
    fileCount: Int,
    storageUsageMb: String,
    storageUsageLimitMb: String,
) {
    val unlimited = stringResource(R.string.unlimited)
    val limitMb = storageUsageLimitMb.toDoubleOrNull() ?: 0.0
    val usageMb = storageUsageMb.toDoubleOrNull() ?: 0.0
    val limitDisplay = if (limitMb < 0) unlimited else formatStorageSize(limitMb)
    val usageDisplay = formatStorageSize(usageMb)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.storage), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(R.string.total_files), style = MaterialTheme.typography.bodySmall)
                Text(
                    text = fileCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(R.string.usage), style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "$usageDisplay / $limitDisplay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (limitMb > 0) {
                LinearProgressIndicator(
                    progress = { (usageMb / limitMb).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
        }
    }
}

private fun formatStorageSize(mb: Double): String {
    return when {
        mb >= 1024 -> String.format("%.1f GB", mb / 1024)
        else -> String.format("%.1f MB", mb)
    }
}
