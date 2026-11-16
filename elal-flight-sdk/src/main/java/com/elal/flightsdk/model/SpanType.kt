package com.elal.flightsdk.model

/** The kind of styling a [TextSpanSpec] applies. */
internal enum class SpanType {
    /** Strike the old value through (`StrikethroughSpan`). */
    STRIKETHROUGH,

    /** Have TalkBack read [TextSpanSpec.spokenText] instead of the characters (`TtsSpan`). */
    SPOKEN,
}
