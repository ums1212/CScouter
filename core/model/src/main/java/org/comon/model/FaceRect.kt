package org.comon.model

data class FaceRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {

    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float): Boolean =
        x in left..right && y >= top && y <= bottom

    fun intersects(other: FaceRect): Boolean =
        left < other.right && right > other.left &&
                top < other.bottom && bottom > other.top
}
