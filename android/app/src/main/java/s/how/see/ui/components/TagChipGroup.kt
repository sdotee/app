package s.how.see.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import s.how.see.data.remote.model.Tag

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChipGroup(
    tags: List<Tag>,
    selectedTagIds: Set<Int>,
    onTagToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.forEach { tag ->
            FilterChip(
                selected = tag.id in selectedTagIds,
                onClick = { onTagToggle(tag.id) },
                label = { Text(tag.name) },
            )
        }
    }
}
