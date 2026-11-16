package com.elal.flightsdk.presentation

/** Isolates a value's text direction (gates, codes, times) inside a sentence; faked in JVM tests. */
internal interface BidiWrapper {

    /** Returns [text] wrapped so it keeps its own direction inside surrounding RTL or LTR text. */
    fun wrap(text: String): String
}
