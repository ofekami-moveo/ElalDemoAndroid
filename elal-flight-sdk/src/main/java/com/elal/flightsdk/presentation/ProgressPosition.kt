package com.elal.flightsdk.presentation

/** Where the progress bar is filled to for a stage; [ProgressCalculator] turns it into a value. */
internal enum class ProgressPosition {
    /** Empty bar (CHECK_IN, errors). */
    START,

    /** End of the first short segment (gate stages). */
    GATE,

    /** Start of the flight segment (LAST_CALL). */
    FLIGHT_START,

    /** Moving along the flight segment (DEPARTED). */
    IN_FLIGHT,

    /** Full bar (landing onward). */
    END,
}
