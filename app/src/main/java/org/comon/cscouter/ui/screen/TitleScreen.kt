package org.comon.cscouter.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import org.comon.cscouter.R

@Composable
fun TitleScreen(
    isPermissionGranted: Boolean,
    onNext: (Boolean) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val imageRes = if (isLandscape) {
        R.drawable.title_landscape
    } else {
        R.drawable.title_portrait
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onNext(isPermissionGranted) },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = androidx.compose.ui.res.stringResource(org.comon.cscouter.R.string.title_img_desc),
            modifier = Modifier.fillMaxSize()
        )
    }
}
