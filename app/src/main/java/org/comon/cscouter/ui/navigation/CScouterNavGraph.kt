package org.comon.cscouter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.comon.cscouter.ui.screen.CameraScouterScreen
import org.comon.cscouter.ui.screen.ResultScreen
import org.comon.logic.PowerMeasurementStateMachine
import org.comon.ml.FaceDetector

@Composable
fun CScouterNavGraph(
    navController: NavHostController,
    faceDetector: FaceDetector,
    stateMachine: PowerMeasurementStateMachine
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Camera.route
    ) {
        composable(Screen.Camera.route) {
            CameraScouterScreen(
                faceDetector = faceDetector,
                stateMachine = stateMachine,
                onNavigateToResult = { power, imageUri ->
                    val encodedUri = android.net.Uri.encode(imageUri)
                    navController.navigate(
                        Screen.Result.createRoute(power, encodedUri)
                    )
                }
            )
        }

        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("power") { type = NavType.IntType },
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val power = backStackEntry.arguments?.getInt("power") ?: 0
            val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""

            ResultScreen(
                power = power,
                imageUri = imageUri,
                onRetry = {
                    navController.popBackStack()
                }
            )
        }
    }
}
