package com.elal.flightsdk.presentation

/** How a stage colours its progress segments. */
internal enum class SegmentStyle {
    /** Brand blue, amber when the flight is delayed (before landing). */
    DELAY_AWARE,

    /** Always brand blue (landing onward: a delay no longer matters). */
    ON_TIME_ONLY,

    /** Grey (error stages). */
    ERROR,
}
