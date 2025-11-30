package org.comon.cscouter.model

data class DetectedFaceInfo(
    val id: Int,
    val boundingBox: android.graphics.Rect,
    val powerLevel: Int
)