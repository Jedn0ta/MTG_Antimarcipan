package com.example.antimarcipan.friends

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Můj profil: jedinečné ID zařízení, jméno, barva a avatar. Bez účtu a serveru. */
object Me {
    data class Profile(val uid: String, val name: String, val color: Int, val avatar: String?)

    val profile = MutableStateFlow(Profile("", "", 0, null))

    private fun prefs(c: Context) = c.getSharedPreferences("friends", Context.MODE_PRIVATE)

    fun init(c: Context) {
        val p = prefs(c)
        var uid = p.getString("myUid", null)
        if (uid == null) {
            uid = (1..12).map { "abcdefghjkmnpqrstuvwxyz23456789".random() }.joinToString("")
            p.edit().putString("myUid", uid).apply()
        }
        profile.value = Profile(uid, p.getString("myName", "")?.trim().orEmpty(), p.getInt("myColor", (0 until FRIEND_COLORS.size).random()), p.getString("myAvatar", null))
        if (!p.contains("myColor")) p.edit().putInt("myColor", profile.value.color).apply()
    }

    fun save(c: Context, name: String, color: Int, avatar: String?) {
        prefs(c).edit().putString("myName", name.trim()).putInt("myColor", color).putString("myAvatar", avatar).apply()
        profile.value = profile.value.copy(name = name.trim(), color = color, avatar = avatar)
    }

    val uid: String get() = profile.value.uid
    val name: String get() = profile.value.name.ifBlank { "Hráč" }
}

/** Kód přítele (QR / odkaz): AMFRIEND1:<base64 JSON>. */
@Serializable
data class FriendCard(val u: String, val n: String, val c: Int = 0, val a: String? = null)

object FriendCode {
    private const val PREFIX = "AMFRIEND1:"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun mine(): String {
        val p = Me.profile.value
        val raw = json.encodeToString(FriendCard.serializer(), FriendCard(p.uid, Me.name, p.color, p.avatar))
        return PREFIX + Base64.encodeToString(raw.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
    }

    fun inviteText(): String =
        "Přidej si mě jako přítele v Grimoaru (${Me.name}). Zkopíruj tuto zprávu a otevři Grimoar → Přátelé.\n" + mine()

    fun decode(text: String): FriendCard? = runCatching {
        val i = text.indexOf(PREFIX); if (i < 0) return null
        val code = text.substring(i + PREFIX.length).takeWhile { it.isLetterOrDigit() || it == '-' || it == '_' || it == '=' }
        json.decodeFromString(FriendCard.serializer(), String(Base64.decode(code, Base64.NO_WRAP or Base64.URL_SAFE)))
    }.getOrNull()

    /** QR kód jako bitmapa (zlatá na tmavé). */
    fun qr(text: String, size: Int = 720): Bitmap {
        val m = com.google.zxing.qrcode.QRCodeWriter().encode(
            text, com.google.zxing.BarcodeFormat.QR_CODE, size, size,
            mapOf(com.google.zxing.EncodeHintType.MARGIN to 1),
        )
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val dark = android.graphics.Color.rgb(20, 15, 36)
        for (x in 0 until size) for (y in 0 until size) bmp.setPixel(x, y, if (m[x, y]) dark else android.graphics.Color.WHITE)
        return bmp
    }
}

val FRIEND_COLORS = listOf(
    Color(0xFFE2BC62), Color(0xFF6FA8DC), Color(0xFFE07A5F), Color(0xFF81B29A),
    Color(0xFFB388EB), Color(0xFFF2A65A), Color(0xFF4ECDC4), Color(0xFFD4A5A5),
)
val FRIEND_AVATARS = listOf("🐉", "🧙", "🔥", "💀", "🌳", "💧", "☀️", "⚔️", "🦉", "🐺", "🛡️", "👑")

/** Kulatý avatar: emoji nebo iniciála na barevném pozadí. */
@Composable
fun FriendAvatar(name: String, color: Int, avatar: String?, size: Dp = 40.dp, online: Boolean = false, modifier: Modifier = Modifier) {
    val c = FRIEND_COLORS[color.mod(FRIEND_COLORS.size)]
    Box(modifier.size(size)) {
        Box(
            Modifier.size(size).clip(CircleShape)
                .background(Brush.linearGradient(listOf(c, c.copy(alpha = 0.55f))))
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                avatar ?: name.trim().take(1).uppercase().ifBlank { "?" },
                fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1633),
            )
        }
        if (online) Box(
            Modifier.size(size * 0.3f).align(Alignment.BottomEnd).clip(CircleShape)
                .background(Color(0xFF4CD964)).border(2.dp, Color(0xFF140F24), CircleShape),
        )
    }
}
