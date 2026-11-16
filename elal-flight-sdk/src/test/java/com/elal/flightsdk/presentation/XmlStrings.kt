package com.elal.flightsdk.presentation

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Reads the SDK's real `strings.xml` for one locale, so tests check the shipped EN and HE texts instead of
 * copies. Keys missing in the locale fall back to `values/` (as Android does).
 */
internal class XmlStrings(localeFolder: String) {

    private val strings: Map<String, String> = load("values") + load(localeFolder)

    /** Returns the raw pattern for [name], with Android's `\'` and `\"` escapes removed. */
    fun pattern(name: String): String = requireNotNull(strings[name]) { "missing string $name" }

    /** Formats [name] with [args], like `Context.getString(res, *args)`. */
    fun format(name: String, vararg args: Any): String = pattern(name).format(*args)

    /** Parses `src/main/res/<folder>/strings.xml` into name → text; an absent file gives an empty map. */
    private fun load(folder: String): Map<String, String> {
        val file = File(RES_DIR, "$folder/strings.xml")
        if (!file.exists()) {
            return emptyMap()
        }
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            val name = node.attributes.getNamedItem("name").nodeValue
            name to node.textContent.replace("\\'", "'").replace("\\\"", "\"")
        }
    }

    private companion object {
        /** Unit tests run with the module directory as the working directory. */
        const val RES_DIR = "src/main/res"
    }
}
