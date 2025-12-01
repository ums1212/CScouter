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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.comon.cscouter.ui.component.FacesOverlay
import org.comon.cscouter.logic.FaceAnalysisAnalyzer
import org.comon.logic.PowerMeasurementStateMachine
import org.comon.ml.FaceDetector
import org.comon.model.PowerMeasurementState

@Composable
fun CameraScouterScreen(
    faceDetector: FaceDetector,
    stateMachine: PowerMeasurementStateMachine
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var measurementState by remember {
        mutableStateOf<PowerMeasurementState>(PowerMeasurementState.Idle)
    }
    var imageWidth by remember { mutableStateOf(0) }
    var imageHeight by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }

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

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        FacesOverlay(
            measurementState = measurementState,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            modifier = Modifier.fillMaxSize()
        )
    }
}