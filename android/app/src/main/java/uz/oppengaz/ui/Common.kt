package uz.oppengaz.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import uz.oppengaz.data.Availability
import uz.oppengaz.data.Station

val GreenOpen = Color(0xFF2E7D32)
val RedClosed = Color(0xFFC62828)
val GrayUnknown = Color(0xFF757575)
val OrangeWarn = Color(0xFFEF6C00)

fun Availability.color(): Color = when (this) {
    Availability.OPEN -> GreenOpen
    Availability.CLOSED -> RedClosed
    Availability.UNKNOWN -> GrayUnknown
}

fun Availability.label(): String = when (this) {
    Availability.OPEN -> "Ochiq"
    Availability.CLOSED -> "Yopiq"
    Availability.UNKNOWN -> "Noma'lum"
}

fun timeAgo(ms: Long?): String {
    if (ms == null) return "ma'lumot yo'q"
    val min = (System.currentTimeMillis() - ms) / 60_000
    return when {
        min < 1 -> "hozirgina"
        min < 60 -> "$min daqiqa oldin"
        else -> "${min / 60} soat oldin"
    }
}

fun regionLabel(code: String): String =
    uz.oppengaz.data.Region.entries.firstOrNull { it.code == code }?.label ?: code

/** "Necha kishi tasdiqladi" belgisi: ishonch uchun har doim ko'rinadi. */
@Composable
fun VerificationBadge(s: Station) {
    if (s.isVerified) {
        val text = if (s.isCommunity) "✓ ${s.confirmations} haydovchi tasdiqladi" else "✓ Tasdiqlangan"
        Text(text, color = GreenOpen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    } else {
        Text(
            "⚠ Tasdiqlanmagan · ${s.confirmations}/${s.confirmationsNeeded} haydovchi tasdiqladi",
            color = OrangeWarn,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun VerificationProgress(s: Station) {
    if (!s.isVerified && s.confirmationsNeeded > 0) {
        Column(Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { (s.confirmations.toFloat() / s.confirmationsNeeded).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = OrangeWarn,
            )
        }
    }
}
