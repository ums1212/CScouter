package org.comon.cscouter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import org.comon.cscouter.ml.MlKitFaceDetector
import org.comon.cscouter.ui.navigation.CScouterNavGraph
import org.comon.cscouter.ui.theme.CScouterTheme
import org.comon.logic.PowerCalculator
import org.comon.logic.PowerMeasurementStateMachine

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // 권한 허용/거부 결과 처리 (Compose에서 상태로 관리)
            cameraPermissionGranted.value = isGranted
        }

    private val cameraPermissionGranted = mutableStateOf(false)

    private val powerCalculator = PowerCalculator()
    private val stateMachine = PowerMeasurementStateMachine()
    private val faceDetector by lazy { MlKitFaceDetector(powerCalculator) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 권한 체크
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        cameraPermissionGranted.value = granted
        if (!granted) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }


        setContent {
            CScouterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (cameraPermissionGranted.value) {
                        val navController = rememberNavController()

                        CScouterNavGraph(
                            navController = navController,
                            faceDetector = faceDetector,
                            stateMachine = stateMachine
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("카메라 권한이 필요합니다.")
                        }
                    }
                }
            }
        }
    }
}