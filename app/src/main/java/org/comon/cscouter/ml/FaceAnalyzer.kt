package org.comon.cscouter.ml

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.comon.cscouter.model.DetectedFaceInfo

class FaceAnalyzer(
    private val onFacesDetected: (
        faces: List<DetectedFaceInfo>,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int
    ) -> Unit
): ImageAnalysis.Analyzer {

    private val detector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        FaceDetection.getClient(options)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        // ML Kit 좌표계는 rotation을 보정한 "정방향" 기준이라
        // 90/270도일 때 width/height가 뒤집힌 상태로 본다고 가정
        val (imgWidth, imgHeight) = when (rotationDegrees) {
            90, 270 -> mediaImage.height to mediaImage.width
            else -> mediaImage.width to mediaImage.height
        }
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces ->
                val list = faces.mapIndexed { index, face ->
                    DetectedFaceInfo(
                        id = face.trackingId ?: index,
                        boundingBox = face.boundingBox,
                        powerLevel = calculatePower(face)
                    )
                }
                onFacesDetected(list, imgWidth, imgHeight, rotationDegrees)
            }
            .addOnFailureListener {
                onFacesDetected(emptyList(), imgWidth, imgHeight, rotationDegrees)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * 전투력 계산 예시:
     * - 얼굴 크기
     * - 미소 확률
     * - 왼쪽/오른쪽 눈 뜸 여부
     * 등을 조합해서 재미용으로 점수를 만들어줌.
     */
    private fun calculatePower(face: Face): Int {
        val box: Rect = face.boundingBox
        val faceSize = box.width() * box.height()

        val smileProb = face.smilingProbability ?: 0f
        val leftEyeOpen = face.leftEyeOpenProbability ?: 0f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 0f

        // 아주 대충 만든 예시 포뮬러
        val base = (faceSize / 1000).coerceAtLeast(10)
        val smileBonus = (smileProb * 100).toInt()
        val focusBonus = (((leftEyeOpen + rightEyeOpen) / 2f) * 150).toInt()

        val raw = base + smileBonus + focusBonus

        // 드래곤볼 느낌 나게 스케일업
        return (raw * 7.3).toInt().coerceIn(100, 99999)
    }

}