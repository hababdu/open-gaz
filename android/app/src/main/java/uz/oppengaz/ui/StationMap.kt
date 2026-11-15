package uz.oppengaz.ui

import android.graphics.Color as AColor
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import uz.oppengaz.data.Availability
import uz.oppengaz.data.Fuel
import uz.oppengaz.data.LatLon
import uz.oppengaz.data.Station
import uz.oppengaz.data.statusFor

/** Marker: rangi holatni bildiradi; tasdiqlanmagan shahobchaning chegarasi to'q sariq. */
private fun dot(station: Station, fuel: Fuel?): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setSize(48, 48)
    setStroke(6, if (station.isVerified) AColor.WHITE else AColor.parseColor("#EF6C00"))
    setColor(
        when (station.statusFor(fuel)) {
            Availability.OPEN -> AColor.parseColor("#2E7D32")
            Availability.CLOSED -> AColor.parseColor("#C62828")
            Availability.UNKNOWN -> AColor.parseColor("#757575")
        }
    )
}

@Composable
fun StationMap(
    stations: List<Station>,
    fuel: Fuel?,
    me: LatLon?,
    onClick: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    LifecycleResumeEffect(mapView) {
        mapView?.onResume()
        onPauseOrDispose { mapView?.onPause() }
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                mapView = this
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                if (me != null) {
                    controller.setZoom(12.0)
                    controller.setCenter(GeoPoint(me.lat, me.lon))
                } else {
                    controller.setZoom(9.0)
                    controller.setCenter(GeoPoint(40.65, 71.8)) // Farg'ona vodiysi markazi (taxminan)
                }
            }
        },
        update = { map ->
            map.overlays.clear()
            stations.forEach { s ->
                val marker = Marker(map)
                marker.position = GeoPoint(s.lat, s.lon)
                marker.title = s.name
                marker.icon = dot(s, fuel)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                marker.setOnMarkerClickListener { _, _ -> onClick(s); true }
                map.overlays.add(marker)
            }
            map.invalidate()
        },
        onRelease = { it.onDetach(); mapView = null },
    )
}
