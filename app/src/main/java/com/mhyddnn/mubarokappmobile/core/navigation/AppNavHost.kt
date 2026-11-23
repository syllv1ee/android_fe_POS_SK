
package com.mhyddnn.mubarokappmobile.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mhyddnn.mubarokappmobile.feature.auth.ui.LoginScreen
import com.mhyddnn.mubarokappmobile.feature.dashboard.ui.DashboardScreen
import com.mhyddnn.mubarokappmobile.feature.profile.ui.ProfileScreen
import com.mhyddnn.mubarokappmobile.feature.salescanvas.salesCanvasGraph
import com.mhyddnn.mubarokappmobile.feature.visit.visitGraph

@Composable
fun AppNavHost(
    startDestination: Any,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<Routes.Login> {
            LoginScreen(
                onLoggedIn = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
            )
        }

        composable<Routes.Home> {
            DashboardScreen(
                onMenuClick = { route ->
                    navController.navigate(route)
                },
                onOpenProfile = {
                    navController.navigate(Routes.Profile)
                },
                onRelogin = {
                    navController.navigate(Routes.Login)
                },
            )
        }

        salesCanvasGraph(navController)

        visitGraph(navController)

        composable<Routes.Profile> {
            ProfileScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
