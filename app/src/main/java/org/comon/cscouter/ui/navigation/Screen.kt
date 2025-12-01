package org.comon.cscouter.ui.navigation

sealed class Screen(val route: String) {
    data object Camera : Screen("camera")
    data object Result : Screen("result/{power}/{imageUri}") {
        fun createRoute(power: Int, imageUri: String) = "result/$power/$imageUri"
    }
}
