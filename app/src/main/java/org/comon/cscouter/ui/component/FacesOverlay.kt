package org.comon.cscouter.ui.component

import android.graphics.Paint
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
import org.comon.model.PowerMeasurementState
import kotlin.math.min

@Composable
fun FacesOverlay(
    measurementState: PowerMeasurementState,
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {

        // 화면 사이즈
        val screenWidth = size.width
        val screenHeight = size.height

        if (imageWidth == 0 || imageHeight == 0) return@Canvas

        // 스케일링 비율
        val scaleX = screenWidth / imageWidth
        val scaleY = screenHeight / imageHeight

        // 상태에 따라 그리기
        when (measurementState) {
            is PowerMeasurementState.Idle -> {
                // 아무것도 안 그림
            }

            is PowerMeasurementState.Measuring -> {
                val box = measurementState.boundingBox

                // MLKit rect → 화면 좌표 변환
                val left = box.left * scaleX
                val top = box.top * scaleY
                val right = box.right * scaleX
                val bottom = box.bottom * scaleY

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

            is PowerMeasurementState.Done -> {
                val box = measurementState.boundingBox

                // MLKit rect → 화면 좌표 변환
                val left = box.left * scaleX
                val top = box.top * scaleY
                val right = box.right * scaleX
                val bottom = box.bottom * scaleY

                // 박스 그리기 (전투력 결과)
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
                        "POWER: ${measurementState.averagedPower}",
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