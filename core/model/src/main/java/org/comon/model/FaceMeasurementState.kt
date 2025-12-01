package org.comon.model

sealed class FaceMeasurementState {
    object Idle : FaceMeasurementState()

    data class Measuring(
        val faceId: Int,
        val startTimeMillis: Long,
        val samples: List<Int>,
        val boundingBox: FaceRect,
        val lastSeenTime: Long
    ) : FaceMeasurementState()

    data class Done(
        val faceId: Int,
        val averagedPower: Int,
        val boundingBox: FaceRect,
        val lastSeenTime: Long
    ) : FaceMeasurementState()
}
