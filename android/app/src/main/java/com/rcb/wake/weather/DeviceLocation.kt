package com.rcb.wake.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import com.rcb.wake.storage.AppStorage
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.round

data class Coordinates(val latitude: Double, val longitude: Double)

/**
 * Optional "use my location" for widget weather. Uses the platform LocationManager with coarse
 * permission only (no Play Services, no background location). The position is refreshed while
 * the app is open and stored rounded to two decimals (about 1 km); background weather refreshes
 * reuse the stored position.
 */
object DeviceLocation {
    private const val KEY_LAT = "location_lat"
    private const val KEY_LON = "location_lon"
    private const val KEY_AT = "location_at"
    private const val KEY_PLACE = "location_place"
    const val MIN_UPDATE_INTERVAL_MILLIS = 30 * 60_000L

    /** Rounds to ~1 km so the stored and transmitted position is never precise. */
    fun coarsen(value: Double): Double = round(value * 100) / 100

    fun hasPermission(context: Context) =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun stored(context: Context): Coordinates? {
        val p = AppStorage.state(context)
        if (!p.contains(KEY_LAT)) return null
        return Coordinates(p.getFloat(KEY_LAT, 0f).toDouble(), p.getFloat(KEY_LON, 0f).toDouble())
    }

    fun clear(context: Context) =
        AppStorage.state(context).edit().remove(KEY_LAT).remove(KEY_LON).remove(KEY_AT).remove(KEY_PLACE).apply()

    /** Village / suburb name for the stored position (e.g. "Kosgama"), or null if unknown. */
    fun placeName(context: Context): String? = AppStorage.state(context).getString(KEY_PLACE, null)?.takeIf { it.isNotBlank() }

    /**
     * Gets a fresh coarse fix if the stored one is older than [MIN_UPDATE_INTERVAL_MILLIS].
     * Blocking for up to 15 s; call off the main thread. Returns true if a position is available.
     */
    fun update(context: Context, force: Boolean = false): Boolean {
        if (!hasPermission(context)) return false
        val p = AppStorage.state(context)
        // KEY_PLACE is stored even when the lookup fails, so this only re-runs for older installs.
        if (!force && stored(context) != null && p.contains(KEY_PLACE) && System.currentTimeMillis() - p.getLong(KEY_AT, 0) < MIN_UPDATE_INTERVAL_MILLIS) {
            return true
        }
        val location = currentLocation(context) ?: lastKnown(context) ?: return stored(context) != null
        val before = stored(context)
        val lat = coarsen(location.latitude)
        val lon = coarsen(location.longitude)
        val edit = p.edit()
            .putFloat(KEY_LAT, lat.toFloat())
            .putFloat(KEY_LON, lon.toFloat())
            .putLong(KEY_AT, System.currentTimeMillis())
        // Only look the name up again when the position moved (or it is still unknown).
        if (before != Coordinates(lat, lon) || !p.contains(KEY_PLACE)) {
            edit.putString(KEY_PLACE, reverseGeocode(context, location.latitude, location.longitude).orEmpty())
        }
        edit.apply()
        return true
    }

    /**
     * Smallest named area for the position: sub-locality (village / suburb) first, then locality.
     * Uses the platform Geocoder; on devices with Google services it asks Google's geocoder.
     */
    private fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val address: Address? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val latch = CountDownLatch(1)
                var found: Address? = null
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            found = addresses.firstOrNull()
                            latch.countDown()
                        }

                        override fun onError(errorMessage: String?) {
                            latch.countDown()
                        }
                    },
                )
                latch.await(10, TimeUnit.SECONDS)
                found
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
        return address?.let { it.subLocality ?: it.locality ?: it.subAdminArea }?.take(60)
    }

    @Suppress("MissingPermission") // Checked in update().
    private fun lastKnown(context: Context): Location? {
        val lm = context.getSystemService(LocationManager::class.java)
        return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) || it == LocationManager.PASSIVE_PROVIDER }
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    @Suppress("MissingPermission") // Checked in update().
    private fun currentLocation(context: Context): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val lm = context.getSystemService(LocationManager::class.java)
        if (!runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) return null
        val latch = CountDownLatch(1)
        var result: Location? = null
        val executor = Executors.newSingleThreadExecutor()
        val cancel = CancellationSignal()
        try {
            lm.getCurrentLocation(LocationManager.NETWORK_PROVIDER, cancel, executor) {
                result = it
                latch.countDown()
            }
            if (!latch.await(15, TimeUnit.SECONDS)) cancel.cancel()
        } catch (e: Exception) {
            cancel.cancel()
        } finally {
            executor.shutdown()
        }
        return result
    }
}
