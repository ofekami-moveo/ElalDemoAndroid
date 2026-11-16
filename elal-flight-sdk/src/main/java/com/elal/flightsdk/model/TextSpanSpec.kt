package com.elal.flightsdk.model

/** A styled range [start, end) of the notification content text; the mapper turns it into an Android span. */
internal data class TextSpanSpec(
    val start: Int,
    val end: Int,
    val type: SpanType,
    val spokenText: String? = null,
)
