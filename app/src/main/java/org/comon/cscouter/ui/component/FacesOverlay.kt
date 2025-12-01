package org.comon.cscouter.ui.component

import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.pointer.pointerInput
import org.comon.model.FaceMeasurementState
import org.comon.model.PowerMeasurementState
import kotlin.math.min

@Composable
fun FacesOverlay(
    measurementState: PowerMeasurementState,
    imageWidth: Int,
    imageHeight: Int,
    onFaceTap: (FaceMeasurementState.Done) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMeasurementState by androidx.compose.runtime.rememberUpdatedState(measurementState)
    val currentImageWidth by androidx.compose.runtime.rememberUpdatedState(imageWidth)
    val currentImageHeight by androidx.compose.runtime.rememberUpdatedState(imageHeight)
    val currentOnFaceTap by androidx.compose.runtime.rememberUpdatedState(onFaceTap)

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { tapOffset ->
                // 터치된 좌표가 어떤 얼굴 박스 안에 있는지 확인
                val screenWidth = size.width
                val screenHeight = size.height
                
                val imgW = currentImageWidth
                val imgH = currentImageHeight
                
                if (imgW == 0 || imgH == 0) return@detectTapGestures

                val scale = kotlin.math.max(screenWidth / imgW, screenHeight / imgH)
                val offsetX = (screenWidth - imgW * scale) / 2f
                val offsetY = (screenHeight - imgH * scale) / 2f

                currentMeasurementState.faces.values.forEach { state ->
                    if (state is FaceMeasurementState.Done) {
                        val box = state.boundingBox
                        val left = box.left * scale + offsetX
                        val top = box.top * scale + offsetY
                        val right = box.right * scale + offsetX
                        val bottom = box.bottom * scale + offsetY

                        // 터치 영역 1.5배 확장
                        val width = right - left
                        val height = bottom - top
                        val cx = left + width / 2f
                        val cy = top + height / 2f
                        
                        val expandedWidth = width * 1.5f
                        val expandedHeight = height * 1.5f
                        
                        val touchLeft = cx - expandedWidth / 2f
                        val touchTop = cy - expandedHeight / 2f
                        val touchRight = cx + expandedWidth / 2f
                        val touchBottom = cy + expandedHeight / 2f

                        if (tapOffset.x in touchLeft..touchRight && tapOffset.y in touchTop..touchBottom) {
                            currentOnFaceTap(state)
                            return@detectTapGestures
                        }
                    }
                }
            }
        }
    ) {
        // 화면 사이즈
        val screenWidth = size.width
        val screenHeight = size.height

        if (imageWidth == 0 || imageHeight == 0) return@Canvas

        // 스케일링 비율 (FILL_CENTER)
        val scale = kotlin.math.max(screenWidth / imageWidth, screenHeight / imageHeight)
        
        // 중앙 정렬을 위한 오프셋
        val offsetX = (screenWidth - imageWidth * scale) / 2f
        val offsetY = (screenHeight - imageHeight * scale) / 2f

        // 모든 얼굴 상태에 대해 루프
        measurementState.faces.values.forEach { state ->
            when (state) {
                is FaceMeasurementState.Idle -> {
                    // 아무것도 안 그림
                }

                is FaceMeasurementState.Measuring -> {
                    val box = state.boundingBox

                    // MLKit rect → 화면 좌표 변환
                    val left = box.left * scale + offsetX
                    val top = box.top * scale + offsetY
                    val right = box.right * scale + offsetX
                    val bottom = box.bottom * scale + offsetY

                    val width = right - left
                    val height = bottom - top
                    val size = min(width, height)

                    // 중심 좌표
                    val cx = left + width / 2f
                    val cy = top + height / 2f

                    // 회전 값
                    val rotation = (System.currentTimeMillis() % 2000L).toFloat() / 2000f * 360f

                    // Measuring 상태 → 중심에서 회전하는 정사각형
                    rotate(rotation, pivot = Offset(cx, cy)) {
                        drawRect(
                            color = Color.Cyan,
                            topLeft = Offset(cx - size / 2, cy - size / 2),
                            size = Size(size, size),
                            style = Stroke(
                                width = 5.dp.toPx()
                            )
                        )
                    }
                }

                is FaceMeasurementState.Done -> {
                    val box = state.boundingBox

                    // MLKit rect → 화면 좌표 변환
                    val left = box.left * scale + offsetX
                    val top = box.top * scale + offsetY
                    val right = box.right * scale + offsetX
                    val bottom = box.bottom * scale + offsetY

                    // 박스 그리기 (전투력 결과)
                    // 1. 터치 영역 시각화 (반투명 채우기)
                    drawRect(
                        color = Color.Green.copy(alpha = 0.3f),
                        topLeft = Offset(left, top),
                        size = Size(
                            width = right - left,
                            height = bottom - top
                        )
                    )

                    // 2. 테두리
                    drawRect(
                        color = Color(0xFF00FF00),
                        topLeft = Offset(left, top),
                        size = Size(
                            width = right - left,
                            height = bottom - top
                        ),
                        style = Stroke(width = 5.dp.toPx())
                    )

                    // 전투력 텍스트 표시
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            "POWER: ${state.averagedPower}",
                            left,
                            top - 20.dp.toPx(),
                            Paint().apply {
                                color = android.graphics.Color.GREEN
                                textSize = 40f
                                isFakeBoldText = true
                            }
                        )
                    }
                }
            }
        }
    }
}