package org.comon.logic

import org.comon.model.DetectedFaceInfo
import org.comon.model.FaceMeasurementState
import org.comon.model.PowerMeasurementState

class PowerMeasurementStateMachine(
    private val measuringDurationMs: Long = 3_000L,
    private val gracePeriodMs: Long = 500L
) {

    /**
     * 전투력 측정 상태를 업데이트한다.
     *
     * @param prev  이전 프레임까지의 측정 상태 (모든 얼굴)
     * @param faces 현재 프레임에서 감지된 얼굴 목록
     * @param now   현재 시각 (System.currentTimeMillis() 등)
     */
    fun update(
        prev: PowerMeasurementState,
        faces: List<DetectedFaceInfo>,
        now: Long
    ): PowerMeasurementState {
        val nextFaces = mutableMapOf<Int, FaceMeasurementState>()
        val detectedFaceIds = faces.map { it.id }.toSet()

        // 1. 현재 감지된 얼굴 업데이트
        for (face in faces) {
            val prevState = prev.faces[face.id] ?: FaceMeasurementState.Idle
            val nextState = calculateNextState(prevState, face, now)
            nextFaces[face.id] = nextState
        }

        // 2. 감지되지 않은 얼굴 중 유예 기간 내에 있는 얼굴 유지
        prev.faces.forEach { (id, state) ->
            if (id !in detectedFaceIds) {
                when (state) {
                    is FaceMeasurementState.Measuring -> {
                        if (now - state.lastSeenTime < gracePeriodMs) {
                            nextFaces[id] = state
                        }
                    }
                    is FaceMeasurementState.Done -> {
                        if (now - state.lastSeenTime < gracePeriodMs) {
                            nextFaces[id] = state
                        }
                    }
                    else -> {}
                }
            }
        }

        return PowerMeasurementState(faces = nextFaces)
    }

    private fun calculateNextState(
        prev: FaceMeasurementState,
        face: DetectedFaceInfo,
        now: Long
    ): FaceMeasurementState {
        return when (prev) {
            is FaceMeasurementState.Idle -> {
                FaceMeasurementState.Measuring(
                    faceId = face.id,
                    startTimeMillis = now,
                    samples = listOf(face.powerLevel),
                    boundingBox = face.boundingBox,
                    lastSeenTime = now
                )
            }
            is FaceMeasurementState.Measuring -> {
                // 같은 얼굴 ID에 대한 상태이므로 ID 체크 불필요
                val newSamples = prev.samples + face.powerLevel
                val elapsed = now - prev.startTimeMillis

                if (elapsed >= measuringDurationMs) {
                    // 측정 완료
                    val avgPower = newSamples.average().toInt()
                    FaceMeasurementState.Done(
                        faceId = face.id,
                        averagedPower = avgPower,
                        boundingBox = face.boundingBox,
                        lastSeenTime = now
                    )
                } else {
                    // 측정 중 업데이트
                    prev.copy(
                        samples = newSamples,
                        boundingBox = face.boundingBox,
                        lastSeenTime = now
                    )
                }
            }
            is FaceMeasurementState.Done -> {
                // 이미 완료된 얼굴은 위치만 업데이트
                prev.copy(
                    boundingBox = face.boundingBox,
                    lastSeenTime = now
                )
            }
        }
    }
}