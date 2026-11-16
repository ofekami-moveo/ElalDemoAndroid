package com.elal.flightsdk.presentation

/** Localized strings for the content builder; an interface so the builder runs in JVM tests with a fake. */
internal interface StringProvider {

    /** Returns the string for [key] formatted with [args], like `Context.getString(res, *args)`. */
    fun get(key: StringKey, vararg args: Any): String
}
