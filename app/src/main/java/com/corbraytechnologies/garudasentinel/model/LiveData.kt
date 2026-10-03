package com.corbraytechnologies.garudasentinel.model

import kotlinx.serialization.Serializable

/*
 * Snapshots read live from the OS each time they are needed. They are not stored in
 * the database. Fields the OS does not expose are null rather than guessed.
 */

@Serializable
data class DeviceInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val hardware: String,
    val board: String,
    val androidVersion: String,
    val sdkInt: Int,
    val securityPatch: String,
    val fingerprint: String,
    val bootloader: String,
    /** User-visible device name from Settings. Often contains the owner's first name. */
    val deviceName: String?,
    val supportedAbis: List<String>,
    val locale: String,
    val timeZone: String,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val screenDensityDpi: Int,
    val uptimeMs: Long,
    val lastBootAt: Long,
)

@Serializable
data class BatteryInfo(
    val levelPercent: Int?,
    val status: String,
    val pluggedInto: String?,
    val health: String,
    val temperatureCelsius: Double?,
    val voltageMillivolts: Int?,
    val technology: String?,
    /** Charge cycles; only on Android 14+ and only by some devices. Null when Android reports 0. */
    val cycleCount: Int?,
    val chargeTimeRemainingMs: Long?,
)

@Serializable
data class WifiDetails(
    /** Null when Android hides it (needs location permission and location turned on). */
    val ssid: String?,
    val bssid: String?,
    val rssiDbm: Int?,
    val linkSpeedMbps: Int?,
    val frequencyMhz: Int?,
)

@Serializable
data class NetworkDetails(
    val connected: Boolean,
    val transports: List<String>,
    val validatedInternet: Boolean,
    val metered: Boolean,
    val vpnActive: Boolean,
    val interfaceName: String?,
    val ipAddresses: List<String>,
    val dnsServers: List<String>,
    val privateDnsServer: String?,
    val wifi: WifiDetails?,
)

@Serializable
data class KeyboardInfo(
    val id: String,
    val label: String,
    val packageName: String,
    val isDefault: Boolean,
    val languages: List<String>,
)

@Serializable
data class InputDetails(
    val defaultKeyboardId: String?,
    val keyboards: List<KeyboardInfo>,
    val speechRecognitionAvailable: Boolean,
)

@Serializable
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val foregroundTodayMs: Long,
    val foregroundLast7DaysMs: Long,
    val opensToday: Int,
    val opensLast7Days: Int,
    val lastUsedAt: Long?,
    val isBrowser: Boolean,
)

@Serializable
data class UsageSummary(
    val collectedAt: Long,
    val todayStart: Long,
    val windowStart: Long,
    /** Screen-on time since local midnight, from screen on/off events. */
    val screenOnTodayMs: Long,
    val apps: List<AppUsageInfo>,
)
