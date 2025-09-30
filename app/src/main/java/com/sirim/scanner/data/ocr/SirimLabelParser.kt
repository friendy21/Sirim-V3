package com.sirim.scanner.data.ocr

import java.util.Locale

object SirimLabelParser {
    private val patterns = mapOf(
        "sirimSerialNo" to Regex("(SIRIM|Serial)\\s*[:\uFF1A]?\\s*([A-Za-z0-9]{4,12})", RegexOption.IGNORE_CASE),
        "batchNo" to Regex("Batch\\s*No\\.?\\s*[:\uFF1A]?\\s*([A-Za-z0-9-]{1,200})", RegexOption.IGNORE_CASE),
        "brandTrademark" to Regex("Brand\\s*/?\\s*Trademark\\s*[:\uFF1A]?\\s*(.+)", RegexOption.IGNORE_CASE),
        "model" to Regex("Model\\s*[:\uFF1A]?\\s*([A-Za-z0-9\- ]{1,1500})", RegexOption.IGNORE_CASE),
        "type" to Regex("Type\\s*[:\uFF1A]?\\s*([A-Za-z0-9\- ]{1,1500})", RegexOption.IGNORE_CASE),
        "rating" to Regex("Rating\\s*[:\uFF1A]?\\s*([A-Za-z0-9\- ]{1,600})", RegexOption.IGNORE_CASE),
        "size" to Regex("Size\\s*[:\uFF1A]?\\s*([A-Za-z0-9\- ]{1,1500})", RegexOption.IGNORE_CASE)
    )

    fun parse(text: String): Map<String, String> {
        val normalized = text.replace("\n", " ").replace("  ", " ")
        val results = mutableMapOf<String, String>()
        patterns.forEach { (key, pattern) ->
            val match = pattern.find(normalized)
            if (match != null) {
                results[key] = match.groupValues.last().trim()
            }
        }
        return results
    }

    fun prettifyKey(key: String): String = key.replace(Regex("([A-Z])")) {
        " " + it.value.lowercase(Locale.getDefault())
    }.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
