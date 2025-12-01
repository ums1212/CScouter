package org.comon.logic

import org.comon.model.DetectedFaceInfo
import org.comon.model.PowerMeasurementState

class PowerMeasurementStateMachine(
    private val measuringDurationMs: Long = 3_000L
) {

    /**
     * 전투력 측정 상태를 업데이트한다.
     *
     * @param prev  이전 프레임까지의 측정 상태
     * @param faces 현재 프레임에서 감지된 얼굴 목록
     * @param now   현재 시각 (System.currentTimeMillis() 등)
     */
    fun update(
        prev: PowerMeasurementState,
        faces: List<DetectedFaceInfo>,
        now: Long
    ): PowerMeasurementState {
        // 1) 현재 프레임에서 "메인 타겟" 얼굴 하나 선택
        //    - 가장 큰 얼굴(화면상 박스 면적 기준)을 사용
        val mainFace: DetectedFaceInfo? = faces.maxByOrNull {
            it.boundingBox.width * it.boundingBox.height
        }

        // 2) 얼굴이 하나도 안 보이는 경우
        //    - Idle/Measuring/Done 모두 그대로 유지 (자동 리셋 X)
        if (mainFace == null) {
            return when (prev) {
                PowerMeasurementState.Idle -> PowerMeasurementState.Idle
                is PowerMeasurementState.Measuring -> PowerMeasurementState.Idle   // ✨ 측정 중이었으면 취소
                is PowerMeasurementState.Done -> prev                              // ✨ 완료 상태는 유지
            }
        }

        // 3) 이전 상태에 따라 분기
        return when (prev) {
            is PowerMeasurementState.Idle -> {
                // 새 얼굴 등장 → Measuring 시작
                PowerMeasurementState.Measuring(
                    faceId = mainFace.id,
                    startTimeMillis = now,
                    samples = listOf(mainFace.powerLevel),
                    boundingBox = mainFace.boundingBox
                )
            }

            is PowerMeasurementState.Measuring -> {
                if (mainFace.id != prev.faceId) {
                    // 다른 얼굴로 바뀐 경우 → 해당 얼굴로 새 측정 시작
                    PowerMeasurementState.Measuring(
                        faceId = mainFace.id,
                        startTimeMillis = now,
                        samples = listOf(mainFace.powerLevel),
                        boundingBox = mainFace.boundingBox
                    )
                } else {
                    // 같은 얼굴 계속 측정 중
                    val newSamples = prev.samples + mainFace.powerLevel
                    val elapsed = now - prev.startTimeMillis

                    if (elapsed >= measuringDurationMs) {
                        // 3초 이상 측정 완료 → 평균 전투력 계산 후 Done 상태
                        val avgPower = newSamples.average().toInt()
                        PowerMeasurementState.Done(
                            faceId = mainFace.id,
                            averagedPower = avgPower,
                            boundingBox = mainFace.boundingBox
                        )
                    } else {
                        // 아직 3초 안 지남 → 샘플/위치만 갱신
                        prev.copy(
                            samples = newSamples,
                            boundingBox = mainFace.boundingBox
                        )
                    }
                }
            }

            is PowerMeasurementState.Done -> {
                if (mainFace.id == prev.faceId) {
                    // 같은 얼굴이면 → 결과 유지 + 박스 위치만 업데이트
                    prev.copy(
                        boundingBox = mainFace.boundingBox
                    )
                } else {
                    // 다른 얼굴로 바뀌었으면 → 새 측정 시작
                    PowerMeasurementState.Measuring(
                        faceId = mainFace.id,
                        startTimeMillis = now,
                        samples = listOf(mainFace.powerLevel),
                        boundingBox = mainFace.boundingBox
                    )
                }
            }
        }
    }
}