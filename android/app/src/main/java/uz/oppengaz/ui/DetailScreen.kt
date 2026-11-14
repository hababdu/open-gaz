package uz.oppengaz.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.oppengaz.data.Fuel
import uz.oppengaz.data.Station
import uz.oppengaz.data.fuelInfo

/** Navbat variantlari: (yorliq, taxminiy mashina soni). Kam bosish = haydovchi uchun xavfsizroq. */
private val queueOptions = listOf("Navbat yo'q" to 0, "1-5" to 3, "6-15" to 10, "16-30" to 23, "30+" to 40)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: AppViewModel, stationId: String, onBack: () -> Unit, onLogin: () -> Unit) {
    BackHandler(onBack = onBack)
    val all by vm.all.collectAsState()
    val user by vm.user.collectAsState()
    val comments by vm.comments.collectAsState()
    val station = all.firstOrNull { it.id == stationId }

    LaunchedEffect(stationId) { vm.loadComments(stationId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(station?.name ?: "Shahobcha") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga") }
                },
            )
        },
    ) { padding ->
        if (station == null) {
            Text("Shahobcha topilmadi (yashirilgan yoki o'chirilgan bo'lishi mumkin).", Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val loggedIn = user != null
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Header(station) }
            item { VerifyCard(station, loggedIn, onLogin, onVote = { vm.vote(station.id, it) }) }
            items(Fuel.entries.toList(), key = { it.id }) { fuel ->
                FuelCard(station, fuel, loggedIn, onLogin, vm)
            }
            item {
                Text(
                    "Izohlar (${comments.size.coerceAtLeast(station.commentsCount)})",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                )
            }
            item { CommentInput(loggedIn, onLogin) { text, done -> vm.addComment(station.id, text, done) } }
            if (comments.isEmpty()) {
                item { Text("Hozircha izoh yo'q.", style = MaterialTheme.typography.bodySmall) }
            }
            items(comments, key = { it.id }) { c ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row {
                        Text(c.author, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(timeAgo(c.createdMs), style = MaterialTheme.typography.labelSmall)
                    }
                    Text(c.text)
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(s: Station) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(s.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            regionLabel(s.region) + (if (s.address.isNotBlank()) " · ${s.address}" else "") +
                (s.distanceKm?.let { " · %.1f km".format(it) } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun VerifyCard(s: Station, loggedIn: Boolean, onLogin: () -> Unit, onVote: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VerificationBadge(s)
            VerificationProgress(s)
            if (s.missing > 0) {
                Text("${s.missing} haydovchi \"bu yerda shahobcha yo'q\" degan", style = MaterialTheme.typography.bodySmall, color = RedClosed)
            }
            if (s.isCommunity && !s.isVerified) {
                Text(
                    "Bu shahobchani haydovchi qo'shgan. Siz shu yerda bo'lsangiz, bor-yo'qligini tasdiqlang.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (loggedIn) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onVote("exists") }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenOpen),
                    ) { Text("Bu yerda bor") }
                    OutlinedButton(onClick = { onVote("missing") }, modifier = Modifier.weight(1f)) { Text("Bu yerda yo'q") }
                }
                Text("Tasdiqlash uchun shahobchadan 500 m ichida bo'lishingiz kerak.", style = MaterialTheme.typography.labelSmall)
            } else {
                TextButton(onClick = onLogin) { Text("Tasdiqlash uchun kiring") }
            }
        }
    }
}

@Composable
private fun FuelCard(s: Station, fuel: Fuel, loggedIn: Boolean, onLogin: () -> Unit, vm: AppViewModel) {
    val info = s.fuelInfo(fuel)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fuel.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (info != null) Text(info.status.label(), color = info.status.color(), fontWeight = FontWeight.Bold)
                else Text("Ma'lumot yo'q", color = GrayUnknown)
            }
            if (info != null) {
                Text(
                    "${info.reportCount} ta xabar · oxirgisi ${timeAgo(info.lastUpdatedMs)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                val q = info.queueCars
                if (q != null) {
                    Text(
                        "Navbat: taxminan ${q.toInt()} ta mashina (${info.queueReports} haydovchi aytgan)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (loggedIn) {
                Text("Hozir shu yerdamisiz? Holatni yangilang:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { vm.reportStatus(s.id, fuel, "open") }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenOpen),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) { Text("Ochiq") }
                    Button(
                        onClick = { vm.reportStatus(s.id, fuel, "closed") }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RedClosed),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) { Text("Yopiq") }
                    OutlinedButton(
                        onClick = { vm.reportStatus(s.id, fuel, "no_fuel") }, modifier = Modifier.weight(1.4f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) { Text("Gaz yo'q") }
                }
                Text("Navbat qancha?", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    queueOptions.forEach { (label, cars) ->
                        AssistChip(onClick = { vm.reportQueue(s.id, fuel, cars) }, label = { Text(label) })
                    }
                }
            } else {
                TextButton(onClick = onLogin) { Text("Holat yuborish uchun kiring") }
            }
        }
    }
}

@Composable
private fun CommentInput(loggedIn: Boolean, onLogin: () -> Unit, onSend: (String, () -> Unit) -> Unit) {
    if (!loggedIn) {
        TextButton(onClick = onLogin) { Text("Izoh yozish uchun kiring") }
        return
    }
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text, onValueChange = { if (it.length <= 500) text = it },
            placeholder = { Text("Izoh yozing (masalan: gaz 18:00 gacha bor)") },
            modifier = Modifier.weight(1f), maxLines = 3,
        )
        IconButton(
            enabled = text.isNotBlank() && !sending,
            onClick = {
                sending = true
                onSend(text) { text = ""; sending = false }
            },
        ) { Icon(Icons.AutoMirrored.Filled.Send, "Yuborish") }
    }
}
