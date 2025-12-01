package org.comon.model

data class DetectedFaceInfo(
    val id: Int,
    val boundingBox: FaceRect,
    val powerLevel: Int
)