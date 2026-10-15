package com.example.core.prayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.data.MosqueLocationEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class UserGpsPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 15f
)

data class MosqueVerificationResult(
    val isVerifiedInsideMosque: Boolean,
    val matchedMosque: MosqueLocationEntity?,
    val distanceMeters: Int?,
    val userLocation: UserGpsPoint?,
    val message: String
)

/**
 * محرك التحقق المكاني بالمسجد (GPS Geofence)
 * يتحقق فعلياً من وجود المستخدم داخل النطاق الجغرافي لأحد المساجد المعتمدة لتسجيل الصلاة بدون تلاعب يدوي
 */
object MosqueLocationVerifier {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentUserLocation(context: Context): UserGpsPoint? {
        if (!hasLocationPermission(context)) return null

        val fusedPoint = suspendCancellableCoroutine<UserGpsPoint?> { cont ->
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                val tokenSource = CancellationTokenSource()
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                    .addOnSuccessListener { loc: Location? ->
                        if (loc != null) {
                            cont.resume(
                                UserGpsPoint(
                                    latitude = loc.latitude,
                                    longitude = loc.longitude,
                                    accuracyMeters = loc.accuracy
                                )
                            )
                        } else {
                            client.lastLocation
                                .addOnSuccessListener { lastLoc: Location? ->
                                    if (lastLoc != null) {
                                        cont.resume(
                                            UserGpsPoint(
                                                latitude = lastLoc.latitude,
                                                longitude = lastLoc.longitude,
                                                accuracyMeters = lastLoc.accuracy
                                            )
                                        )
                                    } else {
                                        cont.resume(null)
                                    }
                                }
                                .addOnFailureListener { cont.resume(null) }
                        }
                    }
                    .addOnFailureListener {
                        cont.resume(null)
                    }
            } catch (_: Exception) {
                cont.resume(null)
            }
        }

        if (fusedPoint != null) return fusedPoint

        // Fallback إلى LocationManager المباشر
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            for (provider in providers) {
                if (lm.isProviderEnabled(provider)) {
                    val loc = lm.getLastKnownLocation(provider)
                    if (loc != null) {
                        return UserGpsPoint(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracyMeters = loc.accuracy
                        )
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Int {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].roundToInt().coerceAtLeast(0)
    }

    suspend fun verifyUserAtMosque(
        context: Context,
        approvedMosques: List<MosqueLocationEntity>,
        overrideUserLocation: UserGpsPoint? = null
    ): MosqueVerificationResult {
        if (approvedMosques.isEmpty()) {
            return MosqueVerificationResult(
                isVerifiedInsideMosque = false,
                matchedMosque = null,
                distanceMeters = null,
                userLocation = overrideUserLocation,
                message = "⚠️ يرجى إضافة موقع مسجدك المعتمد أولاً ليتم التحقق المكاني تلقائياً."
            )
        }

        val userPoint = overrideUserLocation ?: fetchCurrentUserLocation(context)
        if (userPoint == null) {
            return MosqueVerificationResult(
                isVerifiedInsideMosque = false,
                matchedMosque = null,
                distanceMeters = null,
                userLocation = null,
                message = "⚠️ تعذر قراءة إحداثيات GPS. تأكد من تفعيل الموقع (Location) ومنح الصلاحية."
            )
        }

        val mosquesWithDistances = approvedMosques.map { mosque ->
            val dist = calculateDistanceMeters(
                userPoint.latitude,
                userPoint.longitude,
                mosque.latitude,
                mosque.longitude
            )
            mosque to dist
        }.sortedBy { it.second }

        val (nearestMosque, minDistance) = mosquesWithDistances.first()
        val effectiveRadius = nearestMosque.radiusMeters.coerceAtLeast(40)

        return if (minDistance <= effectiveRadius) {
            MosqueVerificationResult(
                isVerifiedInsideMosque = true,
                matchedMosque = nearestMosque,
                distanceMeters = minDistance,
                userLocation = userPoint,
                message = "✅ تم التحقق المكاني: أنت داخل نطاق «${nearestMosque.name}» (على بُعد $minDistance متر)"
            )
        } else {
            MosqueVerificationResult(
                isVerifiedInsideMosque = false,
                matchedMosque = nearestMosque,
                distanceMeters = minDistance,
                userLocation = userPoint,
                message = "❌ أنت خارج نطاق «${nearestMosque.name}» بمسافة $minDistance متر (النطاق المسموح ${effectiveRadius}م). لا يمكن تسجيل الصلاة إلا داخل المسجد."
            )
        }
    }
}
