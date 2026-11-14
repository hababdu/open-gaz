package uz.oppengaz.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

/** Backend JSON javoblarini model obyektlarga aylantiradi. */
object Parser {

    fun millis(iso: String?): Long? =
        if (iso == null) null else runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()

    private fun JSONObject.strOrNull(key: String): String? = if (isNull(key)) null else optString(key)
    private fun JSONObject.dblOrNull(key: String): Double? = if (isNull(key)) null else optDouble(key)

    private fun availability(s: String?): Availability = when (s) {
        "open" -> Availability.OPEN
        "closed" -> Availability.CLOSED
        else -> Availability.UNKNOWN
    }

    fun fuel(o: JSONObject) = FuelInfo(
        fuel = o.getString("fuel_type_id"),
        status = availability(o.strOrNull("status")),
        reportCount = o.optInt("report_count", 0),
        lastUpdatedMs = millis(o.strOrNull("last_updated")),
        queueCars = o.dblOrNull("queue_cars"),
        queueReports = o.optInt("queue_report_count", 0),
    )

    fun station(o: JSONObject): Station {
        val fuels = o.optJSONArray("fuels") ?: JSONArray()
        return Station(
            id = o.getString("id"),
            name = o.getString("name"),
            address = o.optString("address", ""),
            region = o.optString("region", ""),
            lat = o.getDouble("latitude"),
            lon = o.getDouble("longitude"),
            isCommunity = o.optString("source", "admin") == "community",
            confirmations = o.optInt("confirmations_count", 0),
            missing = o.optInt("missing_count", 0),
            confirmationsNeeded = o.optInt("confirmations_needed", 0),
            isVerified = o.optBoolean("is_verified", true),
            commentsCount = o.optInt("comments_count", 0),
            fuels = (0 until fuels.length()).map { fuel(fuels.getJSONObject(it)) },
        )
    }

    fun stations(text: String): List<Station> {
        val arr = JSONArray(text)
        return (0 until arr.length()).map { station(arr.getJSONObject(it)) }
    }

    fun user(o: JSONObject) = User(
        id = o.getString("id"),
        email = o.getString("email"),
        displayName = o.getString("display_name"),
    )

    fun comment(o: JSONObject) = Comment(
        id = o.getString("id"),
        author = o.optString("author_name", ""),
        text = o.optString("text", ""),
        createdMs = millis(o.strOrNull("created_at")),
    )

    fun comments(text: String): List<Comment> {
        val arr = JSONArray(text)
        return (0 until arr.length()).map { comment(arr.getJSONObject(it)) }
    }

    /** FastAPI xato javobidan foydalanuvchiga tushunarli matn va (bo'lsa) station_id ni oladi. */
    fun error(status: Int, body: String): ApiException {
        val detail: Any? = runCatching { JSONObject(body).opt("detail") }.getOrNull()
        var stationId: String? = null
        val raw: String? = when (detail) {
            is String -> detail
            is JSONObject -> {
                stationId = if (detail.isNull("station_id")) null else detail.optString("station_id")
                detail.optString("message")
            }
            is JSONArray -> "Kiritilgan ma'lumot noto'g'ri"
            else -> null
        }
        return ApiException(status, translate(status, raw), stationId)
    }

    private fun translate(status: Int, raw: String?): String {
        val m = raw.orEmpty()
        return when {
            m.contains("too far", ignoreCase = true) -> "Siz shahobchadan juda uzoqdasiz"
            m.contains("already reported", ignoreCase = true) -> "Yaqinda xabar bergansiz, biroz kuting"
            m.contains("account with this email", ignoreCase = true) -> "Bu email bilan hisob allaqachon bor"
            m.contains("incorrect", ignoreCase = true) -> "Email yoki parol noto'g'ri"
            m.contains("Invalid or expired", ignoreCase = true) -> "Qayta kiring"
            m.isNotBlank() -> m
            status == 429 -> "Juda tez-tez urinyapsiz, biroz kuting"
            status >= 500 -> "Serverda xatolik, keyinroq urinib ko'ring"
            else -> "Xatolik ($status)"
        }
    }
}
