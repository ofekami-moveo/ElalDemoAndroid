package com.elal.flightsdk.presentation

/** Wraps in square brackets, so tests can see exactly which values were bidi-isolated. */
internal class FakeBidiWrapper : BidiWrapper {

    /** Returns `[text]`. */
    override fun wrap(text: String): String = "[$text]"
}
