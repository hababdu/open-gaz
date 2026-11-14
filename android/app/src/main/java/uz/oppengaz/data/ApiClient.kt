package uz.oppengaz.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Open Gaz backend (FastAPI) bilan gaplashadi. Barcha xatolar ApiException sifatida chiqadi. */
class ApiClient(context: Context, baseUrl: String) {

    private val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    private val json = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        // Bepul serverlar uxlab qoladi: birinchi so'rov ~1 daqiqagacha cho'zilishi mumkin.
        .readTimeout(75, TimeUnit.SECONDS)
        .build()
    private val prefs = context.getSharedPreferences("oppengaz_session", Context.MODE_PRIVATE)

    private var token: String?
        get() = prefs.getString("token", null)
        set(v) { prefs.edit().apply { if (v == null) remove("token") else putString("token", v) }.apply() }

    fun savedUser(): User? {
        if (token == null) return null
        val id = prefs.getString("uid", null) ?: return null
        return User(id, prefs.getString("email", "") ?: "", prefs.getString("name", "") ?: "")
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    private fun saveSession(o: JSONObject): User {
        val user = Parser.user(o.getJSONObject("user"))
        prefs.edit()
            .putString("token", o.getString("access_token"))
            .putString("uid", user.id)
            .putString("email", user.email)
            .putString("name", user.displayName)
            .apply()
        return user
    }

    private suspend fun request(method: String, path: String, body: JSONObject? = null): String =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(base + path)
            token?.let { builder.header("Authorization", "Bearer $it") }
            if (method == "POST") {
                builder.post((body?.toString() ?: "{}").toRequestBody(json))
            } else {
                builder.get()
            }
            try {
                http.newCall(builder.build()).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) throw Parser.error(resp.code, text)
                    text
                }
            } catch (e: IOException) {
                throw ApiException(0, "Internet bilan aloqa yo'q yoki server javob bermayapti")
            }
        }

    // ---- Auth ----
    suspend fun login(email: String, password: String): User {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        return saveSession(JSONObject(request("POST", "api/v1/auth/login", body)))
    }

    suspend fun register(email: String, name: String, password: String): User {
        val body = JSONObject().put("email", email.trim()).put("display_name", name.trim()).put("password", password)
        return saveSession(JSONObject(request("POST", "api/v1/auth/register", body)))
    }

    // ---- Stations ----
    suspend fun overview(): List<Station> = Parser.stations(request("GET", "api/v1/stations/overview"))

    suspend fun addStation(name: String, address: String, region: String, at: LatLon, me: LatLon): Station {
        val body = JSONObject()
            .put("name", name.trim()).put("address", address.trim()).put("region", region)
            .put("latitude", at.lat).put("longitude", at.lon)
            .put("reporter_latitude", me.lat).put("reporter_longitude", me.lon)
        return Parser.station(JSONObject(request("POST", "api/v1/stations/community", body)))
    }

    /** vote: "exists" yoki "missing". */
    suspend fun vote(stationId: String, vote: String, me: LatLon) {
        val body = JSONObject().put("vote", vote).put("latitude", me.lat).put("longitude", me.lon)
        request("POST", "api/v1/stations/$stationId/votes", body)
    }

    // ---- Reports ----
    /** status: "open", "closed" yoki "no_fuel". */
    suspend fun reportStatus(stationId: String, fuel: String, status: String, me: LatLon) {
        val body = JSONObject().put("fuel_type_id", fuel).put("status", status)
            .put("latitude", me.lat).put("longitude", me.lon)
        request("POST", "api/v1/stations/$stationId/reports/status", body)
    }

    suspend fun reportQueue(stationId: String, fuel: String, cars: Int, me: LatLon) {
        val body = JSONObject().put("fuel_type_id", fuel).put("cars_count", cars)
            .put("latitude", me.lat).put("longitude", me.lon)
        request("POST", "api/v1/stations/$stationId/reports/queue", body)
    }

    // ---- Comments ----
    suspend fun comments(stationId: String): List<Comment> =
        Parser.comments(request("GET", "api/v1/stations/$stationId/comments?limit=50"))

    suspend fun addComment(stationId: String, text: String) {
        request("POST", "api/v1/stations/$stationId/comments", JSONObject().put("text", text))
    }
}
