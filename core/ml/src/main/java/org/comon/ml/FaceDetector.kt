package org.comon.ml

import org.comon.model.DetectedFaceInfo
import org.comon.model.FrameData

interface FaceDetector {
    suspend fun detectFaces(frame: FrameData): List<DetectedFaceInfo>
}