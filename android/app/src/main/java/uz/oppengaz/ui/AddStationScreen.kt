package uz.oppengaz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import uz.oppengaz.data.LatLon
import uz.oppengaz.data.Region

/**
 * Yangi shahobcha qo'shish. Joylashuv: xarita surilib, markazdagi pin shahobcha ustiga keltiriladi.
 * Server yangi shahobchani "tasdiqlanmagan" deb saqlaydi, boshqa haydovchilar tasdiqlaydi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStationScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onDuplicate: (String) -> Unit,
    onSaved: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val me by vm.location.collectAsState()

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var region by remember { mutableStateOf<Region?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shahobcha qo'shish") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Faqat shahobcha yonida turgan bo'lsangiz qo'sha olasiz (500 m ichida). Qo'shganingizdan keyin boshqa haydovchilar tasdiqlaydi.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Shahobcha nomi") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = address, onValueChange = { address = it },
                label = { Text("Manzil / mo'ljal (ixtiyoriy)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )

            Text("Viloyat", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Region.entries.forEach { r ->
                    FilterChip(selected = region == r, onClick = { region = r }, label = { Text(r.label) })
                }
            }

            Text("Joylashuv", style = MaterialTheme.typography.titleSmall)
            Text("Xaritani suring: markazdagi pin shahobcha ustiga kelsin.", style = MaterialTheme.typography.bodySmall)
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            val start = me
                            controller.setZoom(if (start != null) 17.0 else 9.0)
                            controller.setCenter(if (start != null) GeoPoint(start.lat, start.lon) else GeoPoint(40.65, 71.8))
                            // Ro'yxat ichida aylantirilganda xarita surilishi uchun
                            setOnTouchListener { v, _ ->
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                false
                            }
                            mapView = this
                        }
                    },
                    onRelease = { it.onDetach(); mapView = null },
                )
                Icon(
                    Icons.Filled.LocationOn, contentDescription = "Tanlangan joy",
                    tint = RedClosed,
                    modifier = Modifier.align(Alignment.Center).padding(bottom = 36.dp).height(48.dp),
                )
            }
            val here = me
            if (here != null) {
                OutlinedButton(
                    onClick = {
                        mapView?.controller?.setZoom(17.0)
                        mapView?.controller?.animateTo(GeoPoint(here.lat, here.lon))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Mening joylashuvim") }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val c = mapView?.mapCenter
                    val reg = region
                    error = when {
                        name.trim().length < 2 -> "Shahobcha nomini kiriting"
                        reg == null -> "Viloyatni tanlang"
                        c == null -> "Joylashuvni tanlang"
                        else -> null
                    }
                    if (error == null && reg != null && c != null) {
                        busy = true
                        vm.addStation(name, address, reg, LatLon(c.latitude, c.longitude)) { result ->
                            busy = false
                            when (result) {
                                AddResult.Success -> onSaved()
                                is AddResult.Duplicate -> onDuplicate(result.stationId)
                                is AddResult.Failed -> error = result.message
                            }
                        }
                    }
                },
            ) { Text("Saqlash") }
        }
    }
}
