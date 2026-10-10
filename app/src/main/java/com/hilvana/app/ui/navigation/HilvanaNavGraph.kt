package com.hilvana.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hilvana.app.ui.screen.InboxScreen
import com.hilvana.app.ui.screen.TodayScreen

sealed class Screen(val route: String) {
    data object Today : Screen("today")
    data object Inbox : Screen("inbox")
}

@Composable
fun HilvanaNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Today.route) {
        composable(Screen.Today.route) { TodayScreen() }
        composable(Screen.Inbox.route) { InboxScreen() }
    }
}
