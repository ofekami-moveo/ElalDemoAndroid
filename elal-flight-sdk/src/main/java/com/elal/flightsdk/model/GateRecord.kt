package com.elal.flightsdk.model

/** The gate a journey had before its last gate change, so the notification can show "Gate ~~D2~~ D3". */
internal data class GateRecord(val activityId: String, val previousGate: String)
