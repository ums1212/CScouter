package org.comon.cscouter.logic

import android.util.Log

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.comon.cscouter.util.toYuvByteArray
import org.comon.logic.PowerMeasurementStateMachine
import org.comon.ml.FaceDetector
import org.comon.model.FrameData
import org.comon.model.PowerMeasurementState

class FaceAnalysisAnalyzer(
    private val faceDetector: FaceDetector,
    private val stateMachine: PowerMeasurementStateMachine,
    private val getPrevState: () -> PowerMeasurementState,
    private val onNewState: (PowerMeasurementState, Int, Int) -> Unit
) : ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        Log.d("FaceAnalysisAnalyzer", "analyze: rotation=$rotation, width=$mediaImage.width, height=$mediaImage.height")

        val sensorWidth = mediaImage.width
        val sensorHeight = mediaImage.height

        val yuvBytes = imageProxy.toYuvByteArray()

        val frame = FrameData(
            timestampMs = System.currentTimeMillis(),
            width = sensorWidth,
            height = sensorHeight,
            rotationDegrees = rotation,
            yuv = yuvBytes
        )

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val faces = faceDetector.detectFaces(frame)
                val now = System.currentTimeMillis()
                val prev = getPrevState()
                val newState = stateMachine.update(prev, faces, now)

                // 🔹 오버레이용으로 쓸 imageWidth/Height는
                //    rotation을 고려해서 여기서 따로 계산
                val (imgW, imgH) = when (rotation) {
                    90, 270 -> sensorHeight to sensorWidth
                    else -> sensorWidth to sensorHeight
                }

                withContext(Dispatchers.Main) {
                    onNewState(newState, imgW, imgH)
                }
            } finally {
                imageProxy.close()
            }
        }
    }
}