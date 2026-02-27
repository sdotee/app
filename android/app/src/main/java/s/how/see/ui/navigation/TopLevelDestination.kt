package s.how.see.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import s.how.see.R

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelResId: Int,
    val route: String,
) {
    LINKS(
        selectedIcon = Icons.Filled.Link,
        unselectedIcon = Icons.Outlined.Link,
        labelResId = R.string.nav_links,
        route = "links",
    ),
    TEXT(
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description,
        labelResId = R.string.nav_text,
        route = "text",
    ),
    FILES(
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.FolderOpen,
        labelResId = R.string.nav_files,
        route = "files",
    ),
    MORE(
        selectedIcon = Icons.Filled.MoreHoriz,
        unselectedIcon = Icons.Outlined.MoreHoriz,
        labelResId = R.string.nav_more,
        route = "more",
    ),
}
