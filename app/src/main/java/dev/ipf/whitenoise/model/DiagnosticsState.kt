package dev.ipf.whitenoise.model

data class DiagnosticRecord(
    val id: String,
    val filename: String,
    val byteCount: Int,
    val createdLabel: String,
    val profileName: String,
)

/** Profile-owned, in-memory choices. No collection, transport, or app-owned persistence is performed. */
data class DiagnosticsState(
    val analyticsEnabled: Boolean = false,
    val loggingEnabled: Boolean = false,
    val hasSeenPrompt: Boolean = false,
    val records: List<DiagnosticRecord> = emptyList(),
) {
    val storedBytes: Long get() = records.sumOf { it.byteCount.toLong() }
    val diagnosticLogExportText: String
        get() = buildString {
            appendLine("White Noise Diagnostic Logs")
            appendLine("Sanitized local troubleshooting records.")
            records.filter { it.byteCount > 0 }.forEachIndexed { index, record ->
                appendLine()
                appendLine("Log file: ${index + 1}")
                appendLine("Created: ${record.createdLabel}")
                appendLine("Recorded size: ${record.byteCount} bytes")
                appendLine("info | app.ready")
                appendLine("info | relay.connected")
                appendLine("info | message.pipeline.ready")
            }
        }
    val summary: String get() = when {
        analyticsEnabled && loggingEnabled -> "On"
        analyticsEnabled -> "Analytics"
        loggingEnabled -> "Logs"
        else -> "Off"
    }

    fun withLogging(enabled: Boolean, profileId: String, profileName: String): DiagnosticsState = copy(
        loggingEnabled = enabled,
        records = if (enabled && records.isEmpty()) {
            listOf(
                DiagnosticRecord("audit-$profileId-01", "audit-$profileId-20260806-01.jsonl", 24_000, "Aug 6, 2026, 6:47 PM", profileName),
                DiagnosticRecord("audit-$profileId-02", "audit-$profileId-20260805-01.jsonl", 8_000, "Aug 5, 2026, 5:47 PM", profileName),
            )
        } else records,
    )

    fun clearRecords(): DiagnosticsState = copy(records = records.map { it.copy(byteCount = 0) })
}
