package org.comon.cscouter.camera

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.comon.cscouter.ml.FaceAnalyzer
import org.comon.cscouter.model.DetectedFaceInfo
import kotlin.math.min

sealed class PowerMeasurementState {
    object Idle: PowerMeasurementState()

    data class Measuring(
        val faceId: Int,
        val startTimeMillis: Long,
        val samples: List<Int>,
        val boundingBox: android.graphics.Rect
    ) : PowerMeasurementState()

    data class Done(
        val faceId: Int,
        val averagedPower: Int,
        val boundingBox: android.graphics.Rect
    ) : PowerMeasurementState()
}

@Composable
fun CameraWithScouterOverlay(
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var faces by remember { mutableStateOf<List<DetectedFaceInfo>>(emptyList()) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var rotationDegrees by remember { mutableIntStateOf(0) }

    var measurementState by remember {
        mutableStateOf<PowerMeasurementState>(PowerMeasurementState.Idle)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 카메라 프리뷰
        AndroidView(
            factory = { context ->
                val previewView = PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
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
                                ContextCompat.getMainExecutor(context),
                                FaceAnalyzer { detected, imgW, imgH, rotation ->
                                    val now = System.currentTimeMillis()

                                    faces = detected
                                    imageWidth = imgW
                                    imageHeight = imgH
                                    rotationDegrees = rotation

                                    // 전투력 측정 상태 업데이트
                                    measurementState = updateMeasurementState(
                                        prev = measurementState,
                                        faces = detected,
                                        now = now
                                    )
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

                }, ContextCompat.getMainExecutor(context))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        FacesOverlay(
            measurementState = measurementState,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            rotationDegrees = rotationDegrees,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun FacesOverlay(
    measurementState: PowerMeasurementState,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    modifier: Modifier = Modifier

) {
    // 아직 프레임 정보가 없으면 그리지 않음
    if (imageWidth == 0 || imageHeight == 0) return

    val isMeasuring = measurementState is PowerMeasurementState.Measuring

    // 측정 중일 때만 회전 애니메이션
    val rotationAnim = if (isMeasuring) {
        val infinite = rememberInfiniteTransition(label = "measuring")
        val angle by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )
        angle
    } else 0f


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

        // 측정 대상 얼굴의 boundingBox 얻기
        val targetBox: android.graphics.Rect? = when (measurementState) {
            is PowerMeasurementState.Measuring -> measurementState.boundingBox
            is PowerMeasurementState.Done -> measurementState.boundingBox
            else -> null
        }

        if (targetBox == null) return@Canvas

        val left = targetBox.left * scale + dx
        val top = targetBox.top * scale + dy
        val right = targetBox.right * scale + dx
        val bottom = targetBox.bottom * scale + dy

        val boxWidth = right - left
        val boxHeight = bottom - top

        when (measurementState) {
            is PowerMeasurementState.Measuring -> {
                // ✅ 얼굴 중심에서 도는 정사각형
                val centerX = (left + right) / 2f
                val centerY = (top + bottom) / 2f
                val side = min(boxWidth, boxHeight) * 0.6f

                // pivot = 얼굴 중심
                rotate(
                    degrees = rotationAnim,
                    pivot = Offset(centerX, centerY)
                ) {
                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(
                            centerX - side / 2f,
                            centerY - side / 2f
                        ),
                        size = Size(side, side),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                // (선택) "측정 중..." 텍스트
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GREEN
                        textSize = 40f
                        isFakeBoldText = true
                    }
                    drawText(
                        "측정 중...",
                        left,
                        bottom + 40f,
                        paint
                    )
                }
            }

            is PowerMeasurementState.Done -> {
                val box = measurementState.boundingBox

                // ML Kit boundingBox → 화면 좌표 변환
                val left = box.left * scale + dx
                val top = box.top * scale + dy
                val right = box.right * scale + dx
                val bottom = box.bottom * scale + dy

                val boxWidth = right - left
                val boxHeight = bottom - top

                // ===========================
                //   1) 완료 사각형 그리기
                // ===========================
                drawRect(
                    color = Color.Green,
                    topLeft = Offset(left, top),
                    size = Size(boxWidth, boxHeight),
                    style = Stroke(width = 4.dp.toPx())
                )

                // ===========================
                //   2) 전투력 텍스트 그리기
                // ===========================
                val text = "전투력 ${measurementState.averagedPower}"

                drawContext.canvas.nativeCanvas.apply {
                    // 반투명 배경
                    val bgPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(180, 0, 0, 0)  // 반투명 검정
                        style = android.graphics.Paint.Style.FILL
                    }
                    // 텍스트 페인트
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GREEN
                        textSize = 42f
                        isFakeBoldText = true
                    }

                    val padding = 10f
                    val textWidth = textPaint.measureText(text)
                    val textHeight = textPaint.fontMetrics.let { it.bottom - it.top }

                    // 텍스트 배경 위치
                    val bgLeft = left
                    val bgTop = (top - textHeight - 20f).coerceAtLeast(0f)
                    val bgRight = bgLeft + textWidth + padding * 2
                    val bgBottom = bgTop + textHeight + padding * 2

                    // 배경 그리기
                    drawRect(bgLeft, bgTop, bgRight, bgBottom, bgPaint)

                    // 텍스트 그리기
                    drawText(
                        text,
                        bgLeft + padding,
                        bgBottom - padding,
                        textPaint
                    )
                }
            }

            else -> Unit
        }

    }
}

private const val MEASURING_DURATION_MS = 3_000L

private fun updateMeasurementState(
    prev: PowerMeasurementState,
    faces: List<DetectedFaceInfo>,
    now: Long
): PowerMeasurementState {

    // 1) 현재 프레임에서 가장 큰 얼굴 하나만 대상으로 사용
    val mainFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }

    // 얼굴이 하나도 안 보이는 경우
    if (mainFace == null) {
        return prev
    }

    return when (prev) {
        PowerMeasurementState.Idle -> {
            // 새 얼굴 등장 → 측정 시작
            PowerMeasurementState.Measuring(
                faceId = mainFace.id,
                startTimeMillis = now,
                samples = listOf(mainFace.powerLevel),
                boundingBox = mainFace.boundingBox
            )
        }

        is PowerMeasurementState.Measuring -> {
            if (mainFace.id != prev.faceId) {
                // 다른 얼굴로 바뀌면 새로 측정 시작
                PowerMeasurementState.Measuring(
                    faceId = mainFace.id,
                    startTimeMillis = now,
                    samples = listOf(mainFace.powerLevel),
                    boundingBox = mainFace.boundingBox
                )
            } else {
                // 같은 얼굴 계속 측정
                val newSamples = prev.samples + mainFace.powerLevel
                val elapsed = now - prev.startTimeMillis

                if (elapsed >= MEASURING_DURATION_MS) {
                    // 3초 경과 → 평균 전투력 계산
                    val avg = newSamples.average().toInt()
                    PowerMeasurementState.Done(
                        faceId = mainFace.id,
                        averagedPower = avg,
                        boundingBox = mainFace.boundingBox
                    )
                } else {
                    prev.copy(
                        samples = newSamples,
                        boundingBox = mainFace.boundingBox // 최신 위치 갱신
                    )
                }
            }
        }

        is PowerMeasurementState.Done -> {
            return if (mainFace.id == prev.faceId) {
                // ✅ 같은 얼굴이면 → 결과 유지 + 위치만 갱신
                prev.copy(boundingBox = mainFace.boundingBox)
            } else {
                // ✅ 다른 얼굴만 보이면 → 새로 측정 시작
                PowerMeasurementState.Measuring(
                    faceId = mainFace.id,
                    startTimeMillis = now,
                    samples = listOf(mainFace.powerLevel),
                    boundingBox = mainFace.boundingBox
                )
            }
        }
    }
}