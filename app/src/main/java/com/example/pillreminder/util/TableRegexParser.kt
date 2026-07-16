package com.example.pillreminder.util

/**
 * Legacy entry point kept for compatibility. The old implementation contained medicine-name lookup tables, which made
 * OCR behavior brittle. All regex-only text extraction now delegates to the drug-agnostic prescription pipeline.
 */
object TableRegexParser {
    fun parse(text: String): List<ParsedPrescriptionItem> = PrescriptionParser.parse(text)
}
