package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.TextSpanSpec

/** A line of content text with its spans, offsets relative to the line. */
internal data class StyledLine(val text: String, val spans: List<TextSpanSpec>)
