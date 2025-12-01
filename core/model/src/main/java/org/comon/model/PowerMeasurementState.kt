package org.comon.model

data class PowerMeasurementState(
    val faces: Map<Int, FaceMeasurementState> = emptyMap()
)
