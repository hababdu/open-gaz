package uz.oppengaz.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oppengaz.BuildConfig
import uz.oppengaz.data.ApiClient
import uz.oppengaz.data.ApiException
import uz.oppengaz.data.Availability
import uz.oppengaz.data.Comment
import uz.oppengaz.data.Fuel
import uz.oppengaz.data.LatLon
import uz.oppengaz.data.LocationProvider
import uz.oppengaz.data.Region
import uz.oppengaz.data.Station
import uz.oppengaz.data.User
import uz.oppengaz.data.distanceKm
import uz.oppengaz.data.statusFor

data class Filters(val region: Region? = null, val fuel: Fuel? = null, val onlyOpen: Boolean = false)

sealed interface AddResult {
    data object Success : AddResult
    /** 100 m ichida shahobcha allaqachon bor: uni tasdiqlash kerak. */
    data class Duplicate(val stationId: String) : AddResult
    data class Failed(val message: String) : AddResult
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val api = ApiClient(app, BuildConfig.API_BASE_URL)
    private val locationProvider = LocationProvider(app)

    private val _user = MutableStateFlow(api.savedUser())
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _all = MutableStateFlow<List<Station>>(emptyList())
    val all: StateFlow<List<Station>> = _all.asStateFlow()

    private val _filters = MutableStateFlow(Filters())
    val filters: StateFlow<Filters> = _filters.asStateFlow()

    private val _location = MutableStateFlow<LatLon?>(null)
    val location: StateFlow<LatLon?> = _location.asStateFlow()

    private val _permission = MutableStateFlow(locationProvider.hasPermission())
    val permissionGranted: StateFlow<Boolean> = _permission.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    /** Filtrlangan va saralangan ro'yxat: ochiqlari tepada, keyin yaqinlari. */
    val stations: StateFlow<List<Station>> =
        combine(_all, _filters, _location) { all, f, loc ->
            all.asSequence()
                .filter { f.region == null || it.region == f.region.code }
                .map { s -> s.copy(distanceKm = loc?.let { distanceKm(it.lat, it.lon, s.lat, s.lon) }) }
                .filter { !f.onlyOpen || it.statusFor(f.fuel) == Availability.OPEN }
                .sortedWith(
                    compareBy<Station> { rank(it.statusFor(f.fuel)) }
                        .thenBy { it.distanceKm ?: Double.MAX_VALUE }
                        .thenBy { it.name }
                )
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
        viewModelScope.launch { refreshLocation() }
    }

    private fun rank(a: Availability) = when (a) {
        Availability.OPEN -> 0
        Availability.UNKNOWN -> 1
        Availability.CLOSED -> 2
    }

    private fun say(text: String) { messageChannel.trySend(text) }

    /** API chaqiruvini xavfsiz bajaradi: xatoni xabar qilib ko'rsatadi, 401 da chiqarib yuboradi. */
    private fun launchApi(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: ApiException) {
                if (e.status == 401) {
                    api.logout()
                    _user.value = null
                    say("Qayta kiring")
                } else {
                    say(e.message ?: "Xatolik")
                }
            }
        }
    }

    fun setRegion(r: Region?) = _filters.update { it.copy(region = r) }
    fun setFuel(f: Fuel?) = _filters.update { it.copy(fuel = f) }
    fun setOnlyOpen(v: Boolean) = _filters.update { it.copy(onlyOpen = v) }

    fun checkPermission() { _permission.value = locationProvider.hasPermission() }

    /** Joylashuvni qo'lda aniqlash (xarita tugmasi). */
    fun locate() {
        viewModelScope.launch {
            checkPermission()
            if (refreshLocation() == null) say("Joylashuvni aniqlab bo'lmadi. GPS va ruxsatni tekshiring")
        }
    }

    suspend fun refreshLocation(): LatLon? {
        val loc = locationProvider.current()
        if (loc != null) _location.value = loc
        return loc
    }

    private suspend fun requireLocation(): LatLon? {
        val loc = refreshLocation()
        if (loc == null) say("Joylashuvni aniqlab bo'lmadi. GPS va ruxsatni tekshiring")
        return loc
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _loading.value = true
            try {
                _all.value = api.overview()
            } catch (e: ApiException) {
                if (!silent) say(e.message ?: "Yuklab bo'lmadi")
            } finally {
                _loading.value = false
            }
        }
    }

    // ---- Auth ----
    fun login(email: String, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                _user.value = api.login(email, password)
                onResult(null)
            } catch (e: ApiException) {
                onResult(e.message ?: "Xatolik")
            }
        }
    }

    fun register(email: String, name: String, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                _user.value = api.register(email, name, password)
                onResult(null)
            } catch (e: ApiException) {
                onResult(e.message ?: "Xatolik")
            }
        }
    }

    fun logout() {
        api.logout()
        _user.value = null
    }

    // ---- Tasdiqlash ----
    /** vote: "exists" yoki "missing". */
    fun vote(stationId: String, vote: String) = launchApi {
        val me = requireLocation() ?: return@launchApi
        api.vote(stationId, vote, me)
        say(if (vote == "exists") "Rahmat! Tasdiqingiz hisobga olindi" else "Rahmat! Ma'lumot yuborildi")
        refresh(silent = true)
    }

    // ---- Hisobotlar ----
    fun reportStatus(stationId: String, fuel: Fuel, status: String) = launchApi {
        val me = requireLocation() ?: return@launchApi
        api.reportStatus(stationId, fuel.id, status, me)
        say("${fuel.label}: holat yuborildi")
        refresh(silent = true)
    }

    fun reportQueue(stationId: String, fuel: Fuel, cars: Int) = launchApi {
        val me = requireLocation() ?: return@launchApi
        api.reportQueue(stationId, fuel.id, cars, me)
        say("${fuel.label}: navbat yuborildi")
        refresh(silent = true)
    }

    // ---- Izohlar ----
    fun loadComments(stationId: String) {
        _comments.value = emptyList()
        launchApi { _comments.value = api.comments(stationId) }
    }

    fun addComment(stationId: String, text: String, onDone: () -> Unit) = launchApi {
        api.addComment(stationId, text)
        _comments.value = api.comments(stationId)
        refresh(silent = true)
        onDone()
    }

    // ---- Yangi shahobcha ----
    fun addStation(name: String, address: String, region: Region, at: LatLon, onResult: (AddResult) -> Unit) {
        viewModelScope.launch {
            val me = requireLocation()
            if (me == null) {
                onResult(AddResult.Failed("Joylashuvni aniqlab bo'lmadi"))
                return@launch
            }
            try {
                api.addStation(name, address, region.code, at, me)
                refresh(silent = true)
                onResult(AddResult.Success)
            } catch (e: ApiException) {
                when {
                    e.status == 409 && e.stationId != null -> onResult(AddResult.Duplicate(e.stationId))
                    e.status == 401 -> { api.logout(); _user.value = null; onResult(AddResult.Failed("Qayta kiring")) }
                    else -> onResult(AddResult.Failed(e.message ?: "Xatolik"))
                }
            }
        }
    }
}
