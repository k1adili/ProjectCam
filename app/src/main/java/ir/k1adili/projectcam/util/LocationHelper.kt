package ir.k1adili.projectcam.util

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
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
}
