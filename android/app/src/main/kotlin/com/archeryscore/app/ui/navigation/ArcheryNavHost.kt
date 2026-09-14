package com.archeryscore.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.archeryscore.app.R
import com.archeryscore.app.ui.history.HistoryScreen
import com.archeryscore.app.ui.history.SessionDetailScreen
import com.archeryscore.app.ui.record.ActiveSessionScreen
import com.archeryscore.app.ui.start.StartScreen
import com.archeryscore.app.ui.stats.StatsScreen

object Routes {
    const val START = "start"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val ACTIVE = "active/{sessionId}"
    const val DETAIL = "detail/{sessionId}"

    fun active(sessionId: String) = "active/$sessionId"
    fun detail(sessionId: String) = "detail/$sessionId"
}

@Composable
fun ArcheryNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showBottomBar = route == Routes.START || route == Routes.HISTORY || route == Routes.STATS

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(navController, route)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.START,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.START) {
                StartScreen(
                    onResumeActive = { id -> navController.navigate(Routes.active(id)) },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) },
                    padding = padding,
                )
            }
            composable(
                route = Routes.ACTIVE,
                arguments = listOf(navArgument("sessionId") { defaultValue = "" }),
            ) {
                ActiveSessionScreen(
                    onFinished = {
                        navController.navigate(Routes.START) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    padding = padding,
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onOpenDetail = { id -> navController.navigate(Routes.detail(id)) },
                    padding = padding,
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("sessionId") { defaultValue = "" }),
            ) {
                SessionDetailScreen(onBack = { navController.popBackStack() }, padding = padding)
            }
            composable(Routes.STATS) {
                StatsScreen(padding = padding)
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        val items = listOf(
            Triple(Routes.START, Icons.Filled.Home, R.string.nav_start),
            Triple(Routes.HISTORY, Icons.Filled.History, R.string.nav_history),
            Triple(Routes.STATS, Icons.Filled.DateRange, R.string.nav_stats),
        )
        items.forEach { (route, icon, labelRes) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(stringResource(labelRes)) },
            )
        }
    }
}