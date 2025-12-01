package org.comon.cscouter.ml

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import org.comon.logic.PowerCalculator
import org.comon.ml.FaceDetector
import org.comon.model.DetectedFaceInfo
import org.comon.model.FaceRect
import org.comon.model.FrameData
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitFaceDetector(
    private val powerCalculator: PowerCalculator
): FaceDetector {

    private val detector: com.google.mlkit.vision.face.FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()

        FaceDetection.getClient(options)
    }

    override suspend fun detectFaces(frame: FrameData): List<DetectedFaceInfo> {
        // ML Kit에서 YUV(NV21) 처리 → 회전 = 0 으로 넣어야 정확함
        val inputImage = InputImage.fromByteArray(
            frame.yuv,
            frame.width,          // ✅ 센서 원본 크기
            frame.height,
            frame.rotationDegrees, // ✅ 여기서 회전 반영
            InputImage.IMAGE_FORMAT_NV21
        )

        val faces = suspendCancellableCoroutine<List<Face>> { cont ->
            detector.process(inputImage)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        return faces.mapIndexed { index, face ->
            val box = face.boundingBox

            val faceRect = FaceRect(
                left = box.left.toFloat(),
                top = box.top.toFloat(),
                right = box.right.toFloat(),
                bottom = box.bottom.toFloat()
            )

            val power = powerCalculator.calculatePower(
                faceRect = faceRect,
                smileProb = face.smilingProbability,
                leftEyeOpen = face.leftEyeOpenProbability,
                rightEyeOpen = face.rightEyeOpenProbability
            )

            DetectedFaceInfo(
                id = face.trackingId ?: index,
                boundingBox = faceRect,
                powerLevel = power
            )
        }

    }
}