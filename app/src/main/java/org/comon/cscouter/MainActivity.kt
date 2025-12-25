package org.comon.cscouter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import androidx.core.app.ActivityCompat
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
                    val navController = rememberNavController()

                    CScouterNavGraph(
                        navController = navController,
                        faceDetector = faceDetector,
                        stateMachine = stateMachine,
                        isCameraPermissionGranted = cameraPermissionGranted.value,
                        onRequestPermission = {
                            val rationale = ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                Manifest.permission.CAMERA
                            )
                            if (!cameraPermissionGranted.value && !rationale) {
                                // 영구 거부 상태 (또는 처음부터 거부되어 rationale이 false인 경우도 포함되나, 
                                // 보통 권한 안내 화면의 버튼을 눌렀다는 것은 이미 한 번 이상 거부했음을 의미함)
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                }
                                startActivity(intent)
                            } else {
                                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 앱으로 돌아왔을 때 권한 상태 재확인
        cameraPermissionGranted.value = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}