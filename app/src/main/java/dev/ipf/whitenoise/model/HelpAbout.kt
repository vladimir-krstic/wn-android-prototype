package dev.ipf.whitenoise.model

import java.net.URI

enum class HelpExternalDestination {
    BugReport,
    PrivacyPolicy,
}

enum class BugReportExcludedData {
    Messages,
    Media,
    Contacts,
    ProfileDetails,
    Keys,
    DiagnosticLogs,
    AuditLogs,
}

data class HelpExternalTarget(
    val url: String,
    val host: String,
    val pathPrefix: String,
)

object HelpAboutPolicy {
    private val targets = mapOf(
        HelpExternalDestination.BugReport to HelpExternalTarget(
            url = "https://github.com/marmot-protocol/whitenoise-android/issues/new/choose",
            host = "github.com",
            pathPrefix = "/marmot-protocol/whitenoise-android/issues/new",
        ),
        HelpExternalDestination.PrivacyPolicy to HelpExternalTarget(
            url = "https://www.whitenoise.chat/privacy",
            host = "www.whitenoise.chat",
            pathPrefix = "/privacy",
        ),
    )

    val bugReportExcludedData: Set<BugReportExcludedData> = BugReportExcludedData.entries.toSet()

    fun target(destination: HelpExternalDestination): HelpExternalTarget = targets.getValue(destination)

    fun isAllowed(destination: HelpExternalDestination, rawUrl: String): Boolean {
        val expected = target(destination)
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(expected.host, ignoreCase = true) &&
            uri.port == -1 &&
            uri.rawUserInfo == null &&
            uri.rawFragment == null &&
            uri.path.orEmpty().let { path ->
                path == expected.pathPrefix || path.startsWith("${expected.pathPrefix}/")
            }
    }
}
