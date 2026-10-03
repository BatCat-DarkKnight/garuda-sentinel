package com.corbraytechnologies.garudasentinel.findings

/** How much a finding matters. Declared most serious first, so ordinal order is ranking order. */
enum class FindingSeverity { HIGH, MEDIUM, LOW }

/**
 * Android settings screens a finding can point to. The UI turns each one into a standard
 * Settings intent; Garuda Sentinel itself never changes a setting.
 */
enum class SettingsTarget {
    ACCESSIBILITY,
    NOTIFICATION_ACCESS,
    SECURITY,
    LOCATION,
    DEVELOPER_OPTIONS,
    ALL_APPS,
    DEFAULT_APPS,
}

/** What the gold text under a finding does when tapped. */
sealed interface FindingAction {
    val label: String

    /** Opens the app's row in the Apps screen. */
    data class ReviewApp(val packageName: String) : FindingAction {
        override val label = "Review this app"
    }

    /** Opens a system settings screen. */
    data class OpenSetting(val target: SettingsTarget, override val label: String = OPEN_SETTING) : FindingAction

    /** Opens the system App info screen for one app. */
    data class OpenAppInfo(val packageName: String, override val label: String = OPEN_SETTING) : FindingAction

    /** Opens Photos and media filtered to photos that carry a location. */
    data object SeePlaces : FindingAction {
        override val label = "See the places"
    }

    /** Opens Apps sorted by the most sensitive access. */
    data object SeeAppsByAccess : FindingAction {
        override val label = "See which apps"
    }

    /** Opens the Permissions screen, for a check that was skipped. */
    data object SetUp : FindingAction {
        override val label = "Set up"
    }

    /** Opens Who can watch, where the unread settings are listed. */
    data object SeeWhoCanWatch : FindingAction {
        override val label = "See which"
    }

    companion object {
        const val OPEN_SETTING = "Open Android setting"
    }
}

/**
 * One thing worth a look. [recency] is the newest time involved (an app's last update or a
 * photo's date), used to order findings of the same severity; 0 when no time applies.
 */
data class Finding(
    val severity: FindingSeverity,
    val title: String,
    val sentence: String,
    val action: FindingAction,
    val recency: Long = 0,
)

/** A check that could not run, shown as a neutral row at the end instead of being hidden. */
data class NotChecked(
    val title: String,
    val sentence: String,
    val action: FindingAction,
)

/** Everything the Report screen shows below its headline. */
data class Report(
    val findings: List<Finding>,
    val passed: Int,
    val notChecked: List<NotChecked>,
) {
    fun count(severity: FindingSeverity): Int = findings.count { it.severity == severity }
}
