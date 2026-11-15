package uz.oppengaz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import uz.oppengaz.data.LatLon
import uz.oppengaz.data.Region
import uz.oppengaz.data.distanceKm

private const val MAX_DISTANCE_M = 500

/**
 * Yangi shahobcha qo'shish. Xarita tepada (sahifa aylantirilmaydi, shuning uchun xarita bilan
 * to'qnashmaydi), forma pastda. Markazdagi pin shahobcha ustiga keltiriladi.
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
    var center by remember { mutableStateOf<GeoPoint?>(null) }
    var centeredOnMe by remember { mutableStateOf(me != null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    // Joylashuv xaritadan keyin aniqlansa, xaritani bir marta shu joyga olib boramiz.
    LaunchedEffect(me, mapView) {
        val m = me
        val v = mapView
        if (m != null && v != null && !centeredOnMe) {
            v.controller.setZoom(17.0)
            v.controller.setCenter(GeoPoint(m.lat, m.lon))
            centeredOnMe = true
        }
    }
    LifecycleResumeEffect(mapView) {
        mapView?.onResume()
        onPauseOrDispose { mapView?.onPause() }
    }

    val distanceM: Double? = run {
        val m = me
        val c = center
        if (m != null && c != null) distanceKm(m.lat, m.lon, c.latitude, c.longitude) * 1000 else null
    }

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
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val view = MapView(ctx)
                        view.setTileSource(TileSourceFactory.MAPNIK)
                        view.setMultiTouchControls(true)
                        val start = me
                        view.controller.setZoom(if (start != null) 17.0 else 9.0)
                        val first = if (start != null) GeoPoint(start.lat, start.lon) else GeoPoint(40.65, 71.8)
                        view.controller.setCenter(first)
                        center = first
                        view.addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                center = GeoPoint(view.mapCenter.latitude, view.mapCenter.longitude)
                                return false
                            }

                            override fun onZoom(event: ZoomEvent?): Boolean = false
                        })
                        mapView = view
                        view
                    },
                    onRelease = { it.onDetach(); mapView = null },
                )
                // Pin: uchi xaritaning aniq markazida bo'lishi uchun yarmi yuqoriga suriladi.
                Icon(
                    Icons.Filled.LocationOn, contentDescription = "Tanlangan joy",
                    tint = RedClosed,
                    modifier = Modifier.align(Alignment.Center).offset(y = (-24).dp).size(48.dp),
                )
                FilledIconButton(
                    onClick = {
                        val m = me
                        if (m == null) vm.locate()
                        else {
                            mapView?.controller?.setZoom(17.0)
                            mapView?.controller?.animateTo(GeoPoint(m.lat, m.lon))
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                ) { Icon(Icons.Filled.MyLocation, "Mening joylashuvim") }
            }

            Column(
                Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    me == null -> Text(
                        "Joylashuvingiz aniqlanmadi. Xaritadagi tugma bilan aniqlang (GPS va ruxsat kerak).",
                        style = MaterialTheme.typography.bodySmall, color = OrangeWarn,
                    )
                    distanceM != null && distanceM > MAX_DISTANCE_M -> Text(
                        "Pin siz turgan joydan ${distanceM.toInt()} m uzoqda. Qo'shish uchun $MAX_DISTANCE_M m ichida bo'lishingiz kerak.",
                        style = MaterialTheme.typography.bodySmall, color = RedClosed,
                    )
                    distanceM != null -> Text(
                        "✓ Pin siz turgan joydan ${distanceM.toInt()} m masofada",
                        style = MaterialTheme.typography.bodySmall, color = GreenOpen,
                    )
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Shahobcha nomi") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = address, onValueChange = { address = it },
                    label = { Text("Manzil / mo'ljal (ixtiyoriy)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Region.entries.forEach { r ->
                        FilterChip(selected = region == r, onClick = { region = r }, label = { Text(r.label) })
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val c = center
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
}
