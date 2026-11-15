package uz.oppengaz

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import uz.oppengaz.ui.AddStationScreen
import uz.oppengaz.ui.AppViewModel
import uz.oppengaz.ui.AuthScreen
import uz.oppengaz.ui.DetailScreen
import uz.oppengaz.ui.HomeScreen
import java.io.File

private sealed interface Screen {
    data object Home : Screen
    data class Detail(val id: String) : Screen
    data object Add : Screen
    data object Auth : Screen
}

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    /** Foydalanuvchi "Ruxsat berish" tugmasini bosganmi (ilova ochilgandagi avtomatik so'rov emas). */
    private var manualRequest = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            vm.checkPermission()
            if (result.values.any { it }) {
                lifecycleScope.launch { vm.refreshLocation() }
            } else if (manualRequest && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                // "Boshqa so'rama" tanlangan: tizim oynasi chiqmaydi, sozlamalarga olib boramiz.
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                )
            }
            manualRequest = false
        }

    private fun requestLocation(manual: Boolean) {
        manualRequest = manual
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    override fun onResume() {
        super.onResume()
        // Sozlamalardan qaytganda holatni yangilaymiz.
        vm.checkPermission()
        if (vm.permissionGranted.value) lifecycleScope.launch { vm.refreshLocation() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // osmdroid: kesh ilova papkasida (saqlash ruxsati kerak emas) va OSM siyosati uchun user-agent
        Configuration.getInstance().apply {
            load(this@MainActivity, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
        // Ilova ochilishi bilan joylashuv ruxsatini so'raymiz.
        if (!vm.permissionGranted.value) requestLocation(manual = false)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1565C0))) {
                App(vm, onRequestLocation = { requestLocation(manual = true) })
            }
        }
    }
}

@Composable
private fun App(vm: AppViewModel, onRequestLocation: () -> Unit) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var returnTo by remember { mutableStateOf<Screen>(Screen.Home) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    // Ma'lumotlarni vaqti-vaqti bilan yangilab turamiz (boshqa haydovchilarning xabarlari uchun)
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            vm.refresh(silent = true)
        }
    }

    fun openAuth() { returnTo = screen; screen = Screen.Auth }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        when (val s = screen) {
            Screen.Home -> HomeScreen(
                vm = vm,
                onOpen = { screen = Screen.Detail(it) },
                onAdd = { if (vm.user.value == null) openAuth() else screen = Screen.Add },
                onLogin = { openAuth() },
                onRequestLocation = onRequestLocation,
            )
            is Screen.Detail -> DetailScreen(vm, s.id, onBack = { screen = Screen.Home }, onLogin = { openAuth() })
            Screen.Add -> AddStationScreen(
                vm = vm,
                onBack = { screen = Screen.Home },
                onDuplicate = { id -> screen = Screen.Detail(id) },
                onSaved = { screen = Screen.Home },
            )
            Screen.Auth -> AuthScreen(vm, onDone = { screen = returnTo }, onBack = { screen = returnTo })
        }
    }
}
