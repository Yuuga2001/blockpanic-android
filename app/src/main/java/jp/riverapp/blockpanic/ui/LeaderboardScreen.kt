package jp.riverapp.blockpanic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.riverapp.blockpanic.i18n.L
import jp.riverapp.blockpanic.model.GameRecordStore
import jp.riverapp.blockpanic.network.LeaderboardRecord
import jp.riverapp.blockpanic.network.SignalingClient
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    var records by remember { mutableStateOf<List<LeaderboardRecord>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            records = SignalingClient().getLeaderboard(GameRecordStore.deviceId)
        } catch (_: Exception) {
            records = emptyList()
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .heightIn(max = 420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x33000000))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "< ${L("back")}",
                    color = Color(0xB3FFFFFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = L("world_records"),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                // Placeholder for alignment
                Box(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    CircularProgressIndicator(color = GameColors.accent)
                }
            } else if (records.isNullOrEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Text(text = L("no_records"), color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = L("no_records_hint"), color = GameColors.textMuted, fontSize = 13.sp)
                }
            } else {
                val scrollState = rememberScrollState()
                Box(modifier = Modifier.horizontalScroll(scrollState)) {
                    Column {
                        // Table header
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(text = L("rank"), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(28.dp))
                            Text(text = L("name"), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(80.dp))
                            Text(text = L("hud_score"), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(56.dp))
                            Text(text = L("hud_time"), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(48.dp))
                            Text(text = L("mode"), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(72.dp))
                            Text(text = L("platform_label"), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(56.dp))
                            Text(text = L("date"), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(90.dp))
                        }

                        LazyColumn {
                            items(records!!) { record ->
                                val bgColor = if (record.isYou) Color(0x26E94560) else Color.Transparent
                                Row(
                                    modifier = Modifier
                                        .background(bgColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 3.dp)
                                ) {
                                    Text(text = "${record.rank}", color = Color(0xCCFFFFFF), fontSize = 11.sp, modifier = Modifier.width(28.dp))
                                    Row(modifier = Modifier.width(80.dp)) {
                                        Text(text = record.playerName, color = Color(0xCCFFFFFF), fontSize = 11.sp)
                                        if (record.isYou) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(text = L("you"), color = Color(0xFFE94560), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(text = "${record.score}", color = Color(0xCCFFFFFF), fontSize = 11.sp, modifier = Modifier.width(56.dp))
                                    Text(text = "${record.survivalTime}s", color = Color(0xCCFFFFFF), fontSize = 11.sp, modifier = Modifier.width(48.dp))
                                    Text(text = record.mode, color = Color(0xCCFFFFFF), fontSize = 11.sp, modifier = Modifier.width(72.dp))
                                    Text(text = record.platform, color = Color(0xCCFFFFFF), fontSize = 11.sp, modifier = Modifier.width(56.dp))
                                    Text(
                                        text = formatLeaderboardDate(record.createdAt),
                                        color = Color(0xCCFFFFFF),
                                        fontSize = 11.sp,
                                        modifier = Modifier.width(90.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatLeaderboardDate(epochSeconds: Double): String {
    return try {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        sdf.format(Date((epochSeconds * 1000).toLong()))
    } catch (_: Exception) {
        ""
    }
}
