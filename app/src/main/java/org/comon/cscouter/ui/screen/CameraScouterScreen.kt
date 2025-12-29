package org.comon.cscouter.ui.screen

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import org.comon.cscouter.ui.component.FacesOverlay
import org.comon.cscouter.ui.component.HelpDialog
import org.comon.cscouter.util.DataStoreManager
import org.comon.cscouter.logic.FaceAnalysisAnalyzer
import org.comon.logic.PowerMeasurementStateMachine
import org.comon.ml.FaceDetector
import org.comon.model.PowerMeasurementState

@Composable
fun CameraScouterScreen(
    faceDetector: FaceDetector,
    stateMachine: PowerMeasurementStateMachine,
    onNavigateToResult: (Int, String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    var measurementState by remember {
        mutableStateOf(PowerMeasurementState())
    }
    var imageWidth by remember { mutableStateOf(0) }
    var imageHeight by remember { mutableStateOf(0) }
    
    // DataStore & Help Dialog State
    val dataStoreManager = remember { DataStoreManager(context) }
    val isFirstLaunch by dataStoreManager.isFirstLaunch.collectAsState(initial = false)
    var showHelpDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 앱 종료 다이얼로그 상태
    var showExitDialog by remember { mutableStateOf(false) }
    
    // 뒤로 가기 버튼 처리
    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(org.comon.cscouter.R.string.exit_app_title)) },
            text = { Text(stringResource(org.comon.cscouter.R.string.exit_app_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        (context as? ComponentActivity)?.finish()
                    }
                ) {
                    Text(stringResource(org.comon.cscouter.R.string.exit))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text(stringResource(org.comon.cscouter.R.string.cancel))
                }
            }
        )
    }

    // 첫 실행 시 자동으로 도움말 표시
    LaunchedEffect(isFirstLaunch) {
        if (isFirstLaunch) {
            showHelpDialog = true
        }
    }

    // PreviewView 참조를 저장하기 위한 변수
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    
    // 중복 클릭 방지를 위한 시간 기록
    var lastClickTime by remember { mutableStateOf(0L) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val view = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                previewView = view

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also { it.surfaceProvider = view.surfaceProvider }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(
                                ContextCompat.getMainExecutor(ctx),
                                FaceAnalysisAnalyzer(
                                    faceDetector = faceDetector,
                                    stateMachine = stateMachine,
                                    getPrevState = { measurementState },
                                    onNewState = { newState, imgW, imgH ->
                                        measurementState = newState
                                        imageWidth = imgW
                                        imageHeight = imgH
                                    }
                                )
                            )
                        }

                    fun updateRotation() {
                        val display = view.display ?: return
                        val rotation = display.rotation
                        preview.targetRotation = rotation
                        analysis.targetRotation = rotation
                    }

                    // 초기 바인딩
                    cameraProvider.unbindAll()
                    try {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                        updateRotation()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // 화면 회전 감지
                    val orientationEventListener = object : android.view.OrientationEventListener(ctx) {
                        override fun onOrientationChanged(orientation: Int) {
                             // 회전 변경 시 targetRotation 업데이트 (너무 빈번한 호출 방지 로직은 CameraX 내부적으로 최적화됨)
                             // 다만 View의 display.rotation이 변경되었을 때만 호출하는 것이 좋음
                             val display = view.display
                             if (display != null) {
                                  val rotation = display.rotation
                                  if (preview.targetRotation != rotation) {
                                      preview.targetRotation = rotation
                                      analysis.targetRotation = rotation
                                  }
                             }
                        }
                    }
                    orientationEventListener.enable()

                }, ContextCompat.getMainExecutor(ctx))

                view
            }
        )

        FacesOverlay(
            measurementState = measurementState,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            onFaceTap = { faceState ->
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 1000L) {
                    android.util.Log.d("CameraScouterScreen", "Click ignored due to debounce")
                    return@FacesOverlay
                }
                lastClickTime = currentTime

                android.util.Log.d("CameraScouterScreen", "onFaceTap called with state: $faceState")
                val currentPreview = previewView
                if (currentPreview == null) {
                    android.util.Log.e("CameraScouterScreen", "previewView is null")
                    return@FacesOverlay
                }
                val bitmap = currentPreview.bitmap
                if (bitmap == null) {
                    android.util.Log.e("CameraScouterScreen", "previewView.bitmap is null")
                    return@FacesOverlay
                }
                
                // 비트맵 크롭 로직
                // PreviewView의 비트맵은 화면에 보이는 그대로임 (View 크기)
                // 따라서 FacesOverlay에서 계산한 화면 좌표를 그대로 사용 가능
                
                val screenWidth = currentPreview.width
                val screenHeight = currentPreview.height
                
                if (imageWidth == 0 || imageHeight == 0) {
                    android.util.Log.e("CameraScouterScreen", "imageWidth or imageHeight is 0")
                    return@FacesOverlay
                }

                val scale = kotlin.math.max(screenWidth.toFloat() / imageWidth, screenHeight.toFloat() / imageHeight)
                val offsetX = (screenWidth - imageWidth * scale) / 2f
                val offsetY = (screenHeight - imageHeight * scale) / 2f
                
                val box = faceState.boundingBox
                val left = (box.left * scale + offsetX).toInt().coerceAtLeast(0)
                val top = (box.top * scale + offsetY).toInt().coerceAtLeast(0)
                val right = (box.right * scale + offsetX).toInt().coerceAtMost(screenWidth)
                val bottom = (box.bottom * scale + offsetY).toInt().coerceAtMost(screenHeight)
                
                val width = right - left
                val height = bottom - top
                
                android.util.Log.d("CameraScouterScreen", "Crop rect: $left, $top, $width, $height")

                if (width > 0 && height > 0) {
                    val croppedBitmap = android.graphics.Bitmap.createBitmap(bitmap, left, top, width, height)
                    
                    // 파일로 저장
                    val file = java.io.File(context.cacheDir, "cropped_face_${System.currentTimeMillis()}.jpg")
                    try {
                        java.io.FileOutputStream(file).use { out ->
                            croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
                        }
                        android.util.Log.d("CameraScouterScreen", "Navigating to result: ${file.absolutePath}")
                        onNavigateToResult(faceState.averagedPower, Uri.fromFile(file).toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.util.Log.e("CameraScouterScreen", "Error saving bitmap", e)
                    }
                } else {
                    android.util.Log.e("CameraScouterScreen", "Invalid crop width/height")
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 도움말 아이콘
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = androidx.compose.ui.Alignment.TopEnd
        ) {
            IconButton(
                onClick = { showHelpDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = stringResource(org.comon.cscouter.R.string.help_icon_desc),
                    tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.8f) // 잘 보이게 색상 조정
                )
            }
        }

        if (showHelpDialog) {
            HelpDialog(
                onDismissRequest = {
                    showHelpDialog = false
                    if (isFirstLaunch) {
                        scope.launch {
                            dataStoreManager.setFirstLaunch(false)
                        }
                    }
                }
            )
        }
    }
}