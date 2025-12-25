package org.comon.cscouter.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.comon.cscouter.R

@Composable
fun PermissionScreen(
    isPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onNext: () -> Unit
) {
    // 권한이 허용되면 자동으로 다음 화면으로 이동
    LaunchedEffect(isPermissionGranted) {
        if (isPermissionGranted) {
            onNext()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "카메라 권한이 필요합니다",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Image(
            painter = painterResource(id = R.drawable.need_camera_permission),
            contentDescription = "Permission Guide Image",
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(200.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "전투력을 측정하기 위해서는 카메라를 통해 얼굴을 인식해야 합니다. 권한을 허용해 주세요.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { onRequestPermission() }
        ) {
            Text("권한 허용")
        }
    }
}
