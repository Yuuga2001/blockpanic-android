package jp.riverapp.blockpanic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.riverapp.blockpanic.i18n.L
import jp.riverapp.blockpanic.network.AppErrorKind

// 絵文字はフォント差によるモノクロ化を防ぐため variation selector を付けない。
// iOS と同じ pictograph を使用。
private fun contentFor(kind: AppErrorKind): Triple<String, String, String> = when (kind) {
    AppErrorKind.OFFLINE          -> Triple("📡", "err_offline_title",         "err_offline_msg")
    AppErrorKind.TIMEOUT          -> Triple("⏳", "err_timeout_title",         "err_timeout_msg")
    AppErrorKind.ROOM_NOT_FOUND   -> Triple("🚪", "err_room_not_found_title",  "err_room_not_found_msg")
    AppErrorKind.ROOM_EXPIRED     -> Triple("⌛", "err_room_expired_title",    "err_room_expired_msg")
    AppErrorKind.ROOM_FULL        -> Triple("🚷", "err_room_full_title",       "err_room_full_msg")
    AppErrorKind.HOST_UNAVAILABLE -> Triple("👻", "err_host_unavailable_title","err_host_unavailable_msg")
    AppErrorKind.SERVER_ERROR     -> Triple("🧰", "err_server_title",          "err_server_msg")
    AppErrorKind.GENERIC          -> Triple("❗", "err_generic_title",         "err_generic_msg")
}

@Composable
fun ErrorDialog(kind: AppErrorKind, onClose: () -> Unit) {
    val (icon, titleKey, msgKey) = contentFor(kind)
    // 背景タップでの誤タッチ閉じは無効 (CLOSE ボタンのみ許可).
    // indication=null + MutableInteractionSource で ripple 無しに背後へのタップ透過を防ぐ
    val bgInteraction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable(
                interactionSource = bgInteraction,
                indication = null,
                onClick = { /* swallow */ }
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xEE1F1F3A))
                .padding(horizontal = 32.dp, vertical = 28.dp)
                .widthIn(max = 340.dp)
        ) {
            Text(text = icon, fontSize = 48.sp)
            Text(
                text = L(titleKey),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = L(msgKey),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .widthIn(min = 140.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE84560))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = L("close"),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
