package com.elal.flightsdk.model

/**
 * The `ProgressStyle` bar: [segments], the filled [value] (0..240), the [tracker] icon at that point, and the
 * [startIcon] (origin) / [endIcon] (destination) at the ends.
 */
internal data class LiveUpdateProgress(
    val segments: List<SegmentSpec>,
    val value: Int,
    val tracker: IconRole,
    val startIcon: IconRole,
    val endIcon: IconRole,
)
