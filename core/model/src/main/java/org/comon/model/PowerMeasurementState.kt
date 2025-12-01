package org.comon.model

sealed class PowerMeasurementState {
    object Idle : PowerMeasurementState()

    data class Measuring(
        val faceId: Int,
        val startTimeMillis: Long,
        val samples: List<Int>,
        // 위치는 별도 모델로
        val boundingBox: FaceRect
    ) : PowerMeasurementState()

    data class Done(
        val faceId: Int,
        val averagedPower: Int,
        val boundingBox: FaceRect
    ) : PowerMeasurementState()
}
