package org.comon.logic

import org.comon.model.FaceRect

class PowerCalculator {
    fun calculatePower(
        faceRect: FaceRect,
        smileProb: Float?,
        leftEyeOpen: Float?,
        rightEyeOpen: Float?
    ): Int {
        val faceSize = (faceRect.right - faceRect.left) * (faceRect.bottom - faceRect.top)
        val base = (faceSize / 1000f).toInt().coerceAtLeast(10)
        val smileBonus = ((smileProb ?: 0f) * 100).toInt()
        val focusBonus = ((((leftEyeOpen ?: 0f) + (rightEyeOpen ?: 0f)) / 2f) * 150).toInt()
        val raw = base + smileBonus + focusBonus
        return (raw * 7.3).toInt().coerceIn(100, 99999)
    }
}