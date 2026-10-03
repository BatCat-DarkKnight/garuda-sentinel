package com.corbraytechnologies.garudasentinel.collect

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.corbraytechnologies.garudasentinel.data.LocationMetadataEntity
import com.corbraytechnologies.garudasentinel.permissions.Permissions
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Takes a single location reading when the user asks for one. Uses the platform
 * LocationManager, so no Google Play services or network lookups are involved.
 */
class LocationCollector(private val context: Context) {

    sealed interface Outcome {
        data class Success(val location: LocationMetadataEntity) : Outcome
        data class Unavailable(val reason: String) : Outcome
    }

    @SuppressLint("MissingPermission") // Checked through Permissions.hasAnyLocation below.
    suspend fun readOnce(): Outcome {
        if (!Permissions.hasAnyLocation(context)) {
            return Outcome.Unavailable("Location permission not granted.")
        }
        val lm = context.getSystemService(LocationManager::class.java)
        if (!LocationManagerCompat.isLocationEnabled(lm)) {
            return Outcome.Unavailable("Location is turned off in system settings.")
        }
        val providers = enabledProviders(lm)
        if (providers.isEmpty()) return Outcome.Unavailable("No location provider is available.")

        // Ask every enabled provider at once and keep the first fix. GPS can be slow indoors
        // and network location is missing on some devices, so no single provider is reliable.
        val fresh = withTimeoutOrNull(TIMEOUT_MS) {
            channelFlow {
                providers.forEach { provider ->
                    launch {
                        currentLocation(lm, provider)?.let { send(it) }
                    }
                }
            }.firstOrNull()
        }
        // Fall back to the newest location Android already knows. Its own timestamp is kept,
        // so the screen shows how old it is.
        val location = fresh
            ?: providers.mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
            ?: return Outcome.Unavailable("No location fix within ${TIMEOUT_MS / 1000} seconds.")

        return Outcome.Success(
            LocationMetadataEntity(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                provider = if (fresh == null) "${location.provider} (last known)" else location.provider ?: "unknown",
                timestamp = location.time,
            )
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(lm: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                lm, provider, signal, ContextCompat.getMainExecutor(context),
            ) { cont.resume(it) }
        }

    private fun enabledProviders(lm: LocationManager): List<String> {
        val precise = Permissions.hasPreciseLocation(context)
        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            if (precise) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.filter { lm.isProviderEnabled(it) }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
