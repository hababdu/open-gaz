package uz.oppengaz.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Hozirgi joylashuv; ruxsat yoki GPS bo'lmasa null. */
    @SuppressLint("MissingPermission")
    suspend fun current(): LatLon? {
        if (!hasPermission()) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        val fresh = suspendCancellableCoroutine<LatLon?> { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (cont.isActive) cont.resume(loc?.let { LatLon(it.latitude, it.longitude) })
                }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }
        if (fresh != null) return fresh
        // Yangi joylashuv topilmasa, oxirgi ma'lum joylashuvga qaytamiz.
        return suspendCancellableCoroutine { cont ->
            client.lastLocation
                .addOnSuccessListener { loc ->
                    if (cont.isActive) cont.resume(loc?.let { LatLon(it.latitude, it.longitude) })
                }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }
    }
}
