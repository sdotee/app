package s.how.see.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import s.how.see.ui.files.FileScreen
import s.how.see.ui.more.MoreScreen
import s.how.see.ui.shortlinks.CreateShortLinkScreen
import s.how.see.ui.shortlinks.LinkStatsScreen
import s.how.see.ui.shortlinks.ShortLinkListScreen
import s.how.see.ui.textsharing.CreateTextShareScreen
import s.how.see.ui.textsharing.TextShareListScreen
import s.how.see.ui.settings.SettingsScreen
import s.how.see.ui.tags.TagsScreen
import s.how.see.ui.usage.UsageScreen

@Composable
fun SEENavHost(
    navController: NavHostController,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.LINKS.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.LINKS.route) {
            ShortLinkListScreen(
                onCreateClick = { navController.navigate("create_link") },
                onStatsClick = { domain, slug -> navController.navigate("link_stats/$domain/$slug") },
                onShowSnackbar = onShowSnackbar,
            )
        }

        composable("create_link") {
            CreateShortLinkScreen(
                onBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar,
            )
        }

        composable("edit_link/{domain}/{slug}/{targetUrl}/{title}") { backStackEntry ->
            val domain = backStackEntry.arguments?.getString("domain") ?: ""
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            val targetUrl = backStackEntry.arguments?.getString("targetUrl") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            CreateShortLinkScreen(
                editDomain = domain,
                editSlug = slug,
                editTargetUrl = targetUrl,
                editTitle = title,
                onBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar,
            )
        }

        composable("link_stats/{domain}/{slug}") { backStackEntry ->
            val domain = backStackEntry.arguments?.getString("domain") ?: ""
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            LinkStatsScreen(
                domain = domain,
                slug = slug,
                onBack = { navController.popBackStack() },
            )
        }

        composable(TopLevelDestination.TEXT.route) {
            TextShareListScreen(
                onCreateClick = { navController.navigate("create_text") },
                onShowSnackbar = onShowSnackbar,
            )
        }

        composable("create_text") {
            CreateTextShareScreen(
                onBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar,
            )
        }

        composable("edit_text/{domain}/{slug}") { backStackEntry ->
            val domain = backStackEntry.arguments?.getString("domain") ?: ""
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            CreateTextShareScreen(
                editDomain = domain,
                editSlug = slug,
                onBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar,
            )
        }

        composable(TopLevelDestination.FILES.route) {
            FileScreen(
                onShowSnackbar = onShowSnackbar,
            )
        }

        composable(TopLevelDestination.MORE.route) {
            MoreScreen(
                onNavigateToTags = { navController.navigate("tags") },
                onNavigateToUsage = { navController.navigate("usage") },
                onNavigateToSettings = { navController.navigate("settings") },
            )
        }

        composable("tags") {
            TagsScreen(onBack = { navController.popBackStack() })
        }

        composable("usage") {
            UsageScreen(onBack = { navController.popBackStack() })
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar,
            )
        }
    }
}
