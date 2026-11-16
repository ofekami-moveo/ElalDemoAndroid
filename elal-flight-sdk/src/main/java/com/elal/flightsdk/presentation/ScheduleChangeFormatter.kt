package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.ScheduleChangeStyle
import com.elal.flightsdk.model.SpanType
import com.elal.flightsdk.model.TextSpanSpec

/**
 * Writes one changed value as a line: "Departs ~~17:00~~ 17:55" (old struck through, TalkBack reads
 * "17:55, was 17:00") or, in the plain style, "Departs 17:55 (was 17:00)". Both values are bidi-isolated so the
 * order is right in Hebrew. Pure; the mapper turns the spans into Android spans.
 */
internal class ScheduleChangeFormatter(
    private val strings: StringProvider,
    private val bidi: BidiWrapper,
    private val style: ScheduleChangeStyle,
) {

    /** The line for [label] (a "%s" string) with [oldValue] → [newValue], plus its spans (offsets within the line). */
    fun line(label: StringKey, oldValue: String, newValue: String): StyledLine {
        val oldWrapped = bidi.wrap(oldValue)
        val newWrapped = bidi.wrap(newValue)
        return when (style) {
            ScheduleChangeStyle.PLAIN_TEXT -> StyledLine(
                strings.get(label, strings.get(StringKey.CHANGE_WAS, newWrapped, oldWrapped)),
                emptyList(),
            )
            ScheduleChangeStyle.SPAN -> {
                val value = strings.get(StringKey.CHANGE_VALUES, oldWrapped, newWrapped)
                val text = strings.get(label, value)
                val valueStart = text.indexOf(value)
                val oldStart = valueStart + value.indexOf(oldWrapped)
                StyledLine(
                    text,
                    listOf(
                        TextSpanSpec(oldStart, oldStart + oldWrapped.length, SpanType.STRIKETHROUGH),
                        TextSpanSpec(
                            valueStart, valueStart + value.length, SpanType.SPOKEN,
                            spokenText = strings.get(StringKey.CHANGE_SPOKEN, newValue, oldValue),
                        ),
                    ),
                )
            }
        }
    }
}
