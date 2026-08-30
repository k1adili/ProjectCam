package ir.k1adili.projectcam.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class CapturedLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float
)

object LocationHelper {

    /**
     * Fetches a single current location fix with high accuracy.
     * Returns null if location could not be determined (timeout, no provider, permission denied
     * at the OS level despite the runtime check, etc.) - callers must handle null gracefully
     * rather than crashing, since GPS can legitimately fail indoors or underground.
     *
     * Caller MUST have already been granted ACCESS_FINE_LOCATION before calling this.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context, timeoutMillis: Long = 20_000L): CapturedLocation? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(timeoutMillis)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellationTokenSource.cancel() }

            client.getCurrentLocation(request, cancellationTokenSource.token)
                .addOnSuccessListener { location ->
                    if (continuation.isActive) {
                        val result = location?.let {
                            CapturedLocation(
                                latitude = it.latitude,
                                longitude = it.longitude,
                                accuracyMeters = if (it.hasAccuracy()) it.accuracy else Float.NaN
                            )
                        }
                        continuation.resume(result)
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }
    }

    /**
     * Continuously streams location fixes while collected. Used on the camera screen so that
     * every shot in a multi-photo session gets the freshest available fix instead of relying on
     * a single one-shot request made when the screen first opened (which could still be "Loading"
     * or stale by the time later photos in the same session were taken).
     *
     * Automatically stops requesting updates from the OS when the collecting coroutine is
     * cancelled (screen closed).
     */
    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(context: Context, intervalMillis: Long = 3_000L): Flow<CapturedLocation> =
        callbackFlow {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
                .setMinUpdateIntervalMillis(intervalMillis / 2)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    trySend(
                        CapturedLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = if (location.hasAccuracy()) location.accuracy else Float.NaN
                        )
                    )
                }
            }

            client.requestLocationUpdates(request, callback, Looper.getMainLooper())

            awaitClose { client.removeLocationUpdates(callback) }
        }
}
