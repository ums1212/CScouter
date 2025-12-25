package org.comon.cscouter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.comon.cscouter.ui.screen.CameraScouterScreen
import org.comon.cscouter.ui.screen.PermissionScreen
import org.comon.cscouter.ui.screen.ResultScreen
import org.comon.cscouter.ui.screen.TitleScreen
import org.comon.logic.PowerMeasurementStateMachine
import org.comon.ml.FaceDetector

@Composable
fun CScouterNavGraph(
    navController: NavHostController,
    faceDetector: FaceDetector,
    stateMachine: PowerMeasurementStateMachine,
    isCameraPermissionGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Title.route
    ) {
        composable(Screen.Title.route) {
            TitleScreen(
                isPermissionGranted = isCameraPermissionGranted,
                onNext = { granted ->
                    if (granted) {
                        navController.navigate(Screen.Camera.route) {
                            popUpTo(Screen.Title.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Permission.route)
                    }
                }
            )
        }

        composable(Screen.Permission.route) {
            PermissionScreen(
                isPermissionGranted = isCameraPermissionGranted,
                onRequestPermission = onRequestPermission,
                onNext = {
                    navController.navigate(Screen.Camera.route) {
                        // 권한 허용 후 이동 시 스택에서 권한 화면 제거
                        popUpTo(Screen.Permission.route) { inclusive = true }
                    }
                }
            )
        }

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
