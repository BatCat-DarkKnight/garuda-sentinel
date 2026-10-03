package com.corbraytechnologies.garudasentinel.collect

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.speech.SpeechRecognizer
import android.view.inputmethod.InputMethodManager
import com.corbraytechnologies.garudasentinel.model.BatteryInfo
import com.corbraytechnologies.garudasentinel.model.DeviceInfo
import com.corbraytechnologies.garudasentinel.model.InputDetails
import com.corbraytechnologies.garudasentinel.model.KeyboardInfo
import com.corbraytechnologies.garudasentinel.model.NetworkDetails
import com.corbraytechnologies.garudasentinel.model.WifiDetails
import java.util.Locale
import java.util.TimeZone

/** Device, battery, network and keyboard facts read straight from the OS. No network access. */
class DeviceCollector(private val context: Context) {

    fun device(now: Long = System.currentTimeMillis()): DeviceInfo {
        val metrics = context.resources.displayMetrics
        val uptime = SystemClock.elapsedRealtime()
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            model = Build.MODEL,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            hardware = Build.HARDWARE,
            board = Build.BOARD,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            fingerprint = Build.FINGERPRINT,
            bootloader = Build.BOOTLOADER,
            deviceName = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME),
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            locale = Locale.getDefault().toLanguageTag(),
            timeZone = TimeZone.getDefault().id,
            screenWidthPx = metrics.widthPixels,
            screenHeightPx = metrics.heightPixels,
            screenDensityDpi = metrics.densityDpi,
            uptimeMs = uptime,
            lastBootAt = now - uptime,
        )
    }

    fun battery(): BatteryInfo {
        // Sticky broadcast: registering with a null receiver just returns the last value.
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val bm = context.getSystemService(BatteryManager::class.java)
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val cycles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            intent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1
        } else {
            -1
        }
        return BatteryInfo(
            levelPercent = if (level >= 0 && scale > 0) level * 100 / scale else null,
            status = statusLabel(intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1),
            pluggedInto = pluggedLabel(intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0),
            health = healthLabel(intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1),
            temperatureCelsius = if (temp != Int.MIN_VALUE) temp / 10.0 else null,
            voltageMillivolts = voltage.takeIf { it > 0 },
            technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() },
            cycleCount = cycleCountOrNull(cycles),
            chargeTimeRemainingMs = bm?.computeChargeTimeRemaining()?.takeIf { it > 0 },
        )
    }

    fun network(): NetworkDetails {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val active = cm.activeNetwork
        val caps = active?.let { cm.getNetworkCapabilities(it) }
        val link = active?.let { cm.getLinkProperties(it) }
        val transports = buildList {
            if (caps == null) return@buildList
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("Wi-Fi")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("Cellular")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("Ethernet")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) add("Bluetooth")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
        }
        val onWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        return NetworkDetails(
            connected = caps != null,
            transports = transports,
            validatedInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            metered = cm.isActiveNetworkMetered,
            vpnActive = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
            interfaceName = link?.interfaceName,
            ipAddresses = link?.linkAddresses?.map { it.address.hostAddress.orEmpty() }.orEmpty(),
            dnsServers = link?.dnsServers?.map { it.hostAddress.orEmpty() }.orEmpty(),
            privateDnsServer = link?.privateDnsServerName,
            wifi = if (onWifi) wifi() else null,
        )
    }

    @Suppress("DEPRECATION") // WifiManager.connectionInfo still works and needs no callback registration.
    private fun wifi(): WifiDetails? {
        val wm = context.applicationContext.getSystemService(WifiManager::class.java) ?: return null
        val info = try {
            wm.connectionInfo
        } catch (_: SecurityException) {
            return null
        } ?: return null
        val ssid = info.ssid?.removeSurrounding("\"")?.takeUnless { it.isBlank() || it == UNKNOWN_SSID }
        val bssid = info.bssid?.takeUnless { it == "02:00:00:00:00:00" }
        return WifiDetails(
            ssid = ssid,
            bssid = bssid,
            rssiDbm = info.rssi.takeIf { it > -127 },
            linkSpeedMbps = info.linkSpeed.takeIf { it > 0 },
            frequencyMhz = info.frequency.takeIf { it > 0 },
        )
    }

    fun input(): InputDetails {
        val imm = context.getSystemService(InputMethodManager::class.java)
        val pm = context.packageManager
        val defaultId = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val keyboards = imm.enabledInputMethodList.map { imi ->
            val languages = imm.getEnabledInputMethodSubtypeList(imi, true)
                .mapNotNull { it.languageTag.takeIf { tag -> tag.isNotBlank() } }
                .distinct()
            KeyboardInfo(
                id = imi.id,
                label = imi.loadLabel(pm).toString(),
                packageName = imi.packageName,
                isDefault = imi.id == defaultId,
                languages = languages,
            )
        }
        return InputDetails(
            defaultKeyboardId = defaultId,
            keyboards = keyboards,
            speechRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context),
        )
    }

    companion object {
        /** Same value as WifiManager.UNKNOWN_SSID, which is only public from API 30. */
        private const val UNKNOWN_SSID = "<unknown ssid>"

        /**
         * Android's charge cycle count, or null when it is missing or 0. Some makers (Samsung)
         * always report 0, and a real 0 cannot be told apart from that, so 0 is "not reported".
         */
        fun cycleCountOrNull(raw: Int): Int? = raw.takeIf { it > 0 }
    }

    private fun statusLabel(status: Int) = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
        else -> "Unknown"
    }

    private fun pluggedLabel(plugged: Int) = when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC charger"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        BatteryManager.BATTERY_PLUGGED_DOCK -> "Dock"
        else -> null
    }

    private fun healthLabel(health: Int) = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        else -> "Not reported"
    }
}
