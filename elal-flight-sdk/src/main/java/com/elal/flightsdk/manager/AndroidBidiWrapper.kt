package com.elal.flightsdk.manager

import androidx.core.text.BidiFormatter
import com.elal.flightsdk.presentation.BidiWrapper

/** [BidiWrapper] using `BidiFormatter` for the current locale, as the notification code did before. */
internal class AndroidBidiWrapper : BidiWrapper {

    /** Wraps [text] with `BidiFormatter.unicodeWrap`. */
    override fun wrap(text: String): String = BidiFormatter.getInstance().unicodeWrap(text)
}
