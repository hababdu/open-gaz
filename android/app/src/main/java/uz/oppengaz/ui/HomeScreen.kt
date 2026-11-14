package uz.oppengaz.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.oppengaz.data.Fuel
import uz.oppengaz.data.Region
import uz.oppengaz.data.Station
import uz.oppengaz.data.fuelInfo
import uz.oppengaz.data.statusFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onLogin: () -> Unit,
) {
    val stations by vm.stations.collectAsState()
    val filters by vm.filters.collectAsState()
    val loading by vm.loading.collectAsState()
    val user by vm.user.collectAsState()
    val me by vm.location.collectAsState()
    var showMap by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Oppen gaz") },
                actions = {
                    IconButton(onClick = { vm.refresh() }) { Icon(Icons.Filled.Refresh, "Yangilash") }
                    val u = user
                    if (u == null) {
                        TextButton(onClick = onLogin) { Text("Kirish") }
                    } else {
                        TextButton(onClick = { menuOpen = true }) { Text(u.displayName) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Chiqish") }, onClick = { menuOpen = false; vm.logout() })
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Shahobcha qo'shish") },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = !showMap, onClick = { showMap = false },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text("Ro'yxat") },
                )
                NavigationBarItem(
                    selected = showMap, onClick = { showMap = true },
                    icon = { Icon(Icons.Filled.Map, null) }, label = { Text("Xarita") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filters.onlyOpen,
                    onClick = { vm.setOnlyOpen(!filters.onlyOpen) },
                    label = { Text("Faqat ochiq") },
                )
                Fuel.entries.forEach { f ->
                    FilterChip(
                        selected = filters.fuel == f,
                        onClick = { vm.setFuel(if (filters.fuel == f) null else f) },
                        label = { Text(f.label) },
                    )
                }
                Region.entries.forEach { r ->
                    FilterChip(
                        selected = filters.region == r,
                        onClick = { vm.setRegion(if (filters.region == r) null else r) },
                        label = { Text(r.label) },
                    )
                }
            }
            if (showMap) {
                StationMap(
                    stations = stations, fuel = filters.fuel, me = me,
                    onClick = { onOpen(it.id) }, modifier = Modifier.fillMaxSize(),
                )
            } else {
                StationList(stations, filters.fuel, onOpen, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun StationList(stations: List<Station>, fuel: Fuel?, onOpen: (String) -> Unit, modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (stations.isEmpty()) {
            item {
                Text(
                    "Shahobcha topilmadi. Birinchi bo'lib qo'shing yoki filtrni o'zgartiring.",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        items(stations, key = { it.id }) { s -> StationCard(s, fuel, onClick = { onOpen(s.id) }) }
    }
}

@Composable
private fun StationCard(s: Station, fuel: Fuel?, onClick: () -> Unit) {
    val status = s.statusFor(fuel)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                )
                Text(status.label(), color = status.color(), fontWeight = FontWeight.Bold)
            }
            Text(
                regionLabel(s.region) +
                    (if (s.address.isNotBlank()) " · ${s.address}" else "") +
                    (s.distanceKm?.let { " · %.1f km".format(it) } ?: ""),
                style = MaterialTheme.typography.bodySmall,
            )
            VerificationBadge(s)
            val shown = if (fuel == null) Fuel.entries else listOf(fuel)
            shown.forEach { f ->
                val info = s.fuelInfo(f) ?: return@forEach
                val queue = info.queueCars?.let { " · navbat ~${it.toInt()} ta" } ?: ""
                Text(
                    "${f.label}: ${info.status.label()}$queue (${info.reportCount} ta xabar, ${timeAgo(info.lastUpdatedMs)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = info.status.color(),
                )
            }
        }
    }
}
