package uz.oppengaz.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class Fuel(val id: String, val label: String) {
    CNG("cng", "Metan"),
    LPG("lpg", "Propan"),
    GASOLINE("gasoline", "Benzin"),
    DIESEL("diesel", "Dizel"),
}

enum class Region(val code: String, val label: String) {
    FERGANA("fergana", "Farg'ona"),
    ANDIJAN("andijan", "Andijon"),
    NAMANGAN("namangan", "Namangan"),
}

enum class Availability { OPEN, CLOSED, UNKNOWN }

/** Bitta yoqilg'i turi bo'yicha holat va navbat (backend /stations/overview). */
data class FuelInfo(
    val fuel: String,
    val status: Availability,
    val reportCount: Int,
    val lastUpdatedMs: Long?,
    val queueCars: Double?,
    val queueReports: Int,
)

data class Station(
    val id: String,
    val name: String,
    val address: String,
    val region: String,
    val lat: Double,
    val lon: Double,
    val isCommunity: Boolean,
    /** Necha haydovchi "bu yerda shahobcha bor" degan. */
    val confirmations: Int,
    val missing: Int,
    /** Tasdiqlangan bo'lishi uchun kerakli tasdiqlar soni (admin qo'shganlar uchun 0). */
    val confirmationsNeeded: Int,
    val isVerified: Boolean,
    val commentsCount: Int,
    val fuels: List<FuelInfo>,
    val distanceKm: Double? = null,
)

data class Comment(val id: String, val author: String, val text: String, val createdMs: Long?)

data class User(val id: String, val email: String, val displayName: String)

data class LatLon(val lat: Double, val lon: Double)

class ApiException(val status: Int, message: String, val stationId: String? = null) : Exception(message)

fun Station.fuelInfo(fuel: Fuel): FuelInfo? = fuels.firstOrNull { it.fuel == fuel.id }

/** Tanlangan yoqilg'i (yoki hammasi) bo'yicha umumiy holat: bittasi ochiq bo'lsa - ochiq. */
fun Station.statusFor(fuel: Fuel?): Availability {
    val list = if (fuel == null) fuels else fuels.filter { it.fuel == fuel.id }
    return when {
        list.any { it.status == Availability.OPEN } -> Availability.OPEN
        list.any { it.status == Availability.CLOSED } -> Availability.CLOSED
        else -> Availability.UNKNOWN
    }
}

fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}
