package s.how.see.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import s.how.see.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToTags: () -> Unit,
    onNavigateToUsage: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_more)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.tags_title)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToTags),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.usage)) },
                leadingContent = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToUsage),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings)) },
                leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToSettings),
            )
        }
    }
}
