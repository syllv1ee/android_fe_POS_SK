
package com.mhyddnn.mubarokappmobile.feature.visit

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mhyddnn.mubarokappmobile.feature.visit.ui.VisitDetailScreen
import com.mhyddnn.mubarokappmobile.feature.visit.ui.VisitFormScreen
import com.mhyddnn.mubarokappmobile.feature.visit.ui.VisitListScreen
import kotlinx.serialization.Serializable

object VisitRoutes {

    @Serializable
    data object Index

    @Serializable
    data object Create

    @Serializable
    data class Detail(val id: Int)
}

fun NavGraphBuilder.visitGraph(navController: NavHostController) {

    val onBack: () -> Unit = {
        navController.popBackStack()
    }

    composable<VisitRoutes.Index> {
        VisitListScreen(
            onVisitClick = { id ->
                navController.navigate(VisitRoutes.Detail(id))
            },
            onAddClick = {
                navController.navigate(VisitRoutes.Create)
            },
            onBack = onBack,
        )
    }

    composable<VisitRoutes.Create> {
        VisitFormScreen(
            onCreated = { id ->
                navController.navigate(VisitRoutes.Detail(id)) {
                    popUpTo<VisitRoutes.Create> {
                        inclusive = true
                    }
                }
            },
            onBack = onBack,
        )
    }

    composable<VisitRoutes.Detail> { backStackEntry ->
        val route = backStackEntry.toRoute<VisitRoutes.Detail>()

        VisitDetailScreen(
            visitId = route.id,
            onBack = onBack,
        )
    }
}
