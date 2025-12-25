package org.comon.cscouter.ui.navigation

sealed class Screen(val route: String) {
    data object Title : Screen("title")
    data object Permission : Screen("permission")
    data object Camera : Screen("camera")
    data object Result : Screen("result/{power}/{imageUri}") {
        fun createRoute(power: Int, imageUri: String) = "result/$power/$imageUri"
    }
}
