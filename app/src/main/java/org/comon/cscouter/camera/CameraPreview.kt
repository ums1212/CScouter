package org.comon.cscouter.camera

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.comon.cscouter.ml.FaceAnalyzer
import org.comon.cscouter.model.DetectedFaceInfo

@Composable
fun CameraWithScouterOverlay(
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var faces by remember { mutableStateOf<List<DetectedFaceInfo>>(emptyList()) }
    var imageWidth by remember { mutableStateOf(0) }
    var imageHeight by remember { mutableStateOf(0) }
    var rotationDegrees by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {

        // 카메라 프리뷰
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(
                                ContextCompat.getMainExecutor(ctx),
                                FaceAnalyzer { detected, imgW, imgH, rotation ->
                                    faces = detected
                                    imageWidth = imgW
                                    imageHeight = imgH
                                    rotationDegrees = rotation
                                }
                            )
                        }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    } catch (e: Exception) {
                        Log.e("Camera", "Bind failed", e)
                    }

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        FacesOverlay(
            faces = faces,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            rotationDegrees = rotationDegrees,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun FacesOverlay(
    faces: List<DetectedFaceInfo>,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    modifier: Modifier = Modifier

) {
    // 아직 프레임 정보가 없으면 그리지 않음
    if (imageWidth == 0 || imageHeight == 0) return

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // CameraX + PreviewView(ScaleType.FILL_CENTER) 기준 스케일 계산
        val scale = maxOf(
            canvasWidth / imageWidth,
            canvasHeight / imageHeight
        )

        val scaledImageWidth = imageWidth * scale
        val scaledImageHeight = imageHeight * scale

        // 가운데 정렬을 위한 offset (FILL_CENTER 중심 기준)
        val dx = (canvasWidth - scaledImageWidth) / 2f
        val dy = (canvasHeight - scaledImageHeight) / 2f

        faces.forEach { face ->
            val box = face.boundingBox

            // ML Kit boundingBox 좌표(원본 이미지 기준)를 화면 좌표로 변환
            val left = box.left * scale + dx
            val top = box.top * scale + dy
            val right = box.right * scale + dx
            val bottom = box.bottom * scale + dy

            // 박스 그리기
            drawRect(
                color = Color.Green,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(
                    width = right - left,
                    height = bottom - top
                ),
                style = Stroke(width = 3.dp.toPx())
            )

            // 전투력 텍스트
            val text = "전투력 ${face.powerLevel}"
            drawContext.canvas.nativeCanvas.apply {
                val bgPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(150, 0, 0, 0)
                    style = android.graphics.Paint.Style.FILL
                }
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GREEN
                    textSize = 36f
                    isFakeBoldText = true
                }

                // 텍스트 배경 박스
                val padding = 8f
                val textWidth = textPaint.measureText(text)
                val textHeight = textPaint.fontMetrics.let { it.bottom - it.top }

                val bgLeft = left
                val bgTop = (top - textHeight - 10f).coerceAtLeast(0f)
                val bgRight = left + textWidth + padding * 2
                val bgBottom = bgTop + textHeight + padding * 2

                drawRect(bgLeft, bgTop, bgRight, bgBottom, bgPaint)

                drawText(
                    text,
                    bgLeft + padding,
                    bgBottom - padding,
                    textPaint
                )
            }
        }
    }
}
