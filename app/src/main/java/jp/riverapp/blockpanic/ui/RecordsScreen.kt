package jp.riverapp.blockpanic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import jp.riverapp.blockpanic.model.GameRecord
import jp.riverapp.blockpanic.model.GameRecordStore

@Composable
fun RecordsScreen(onBack: () -> Unit) {
    var sortByScore by remember { mutableStateOf(false) }
    val records = remember { GameRecordStore.load() }
    val sorted = if (sortByScore) records.sortedByDescending { it.score } else records

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.sceneBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .heightIn(max = 360.dp)
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
                    text = L("my_records"),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                // Sort toggle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x14FFFFFF))
                        .clickable { sortByScore = !sortByScore }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (sortByScore) L("hud_score") else L("recent"),
                        color = Color(0xB3FFFFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (records.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Text(text = L("no_records"), color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = L("no_records_hint"), color = GameColors.textMuted, fontSize = 13.sp)
                }
            } else {
                // Table header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(text = L("date"), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.weight(2f))
                    Text(text = L("hud_time"), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Text(text = L("hud_score"), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Text(text = L("mode"), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.weight(1.5f))
                    Text(text = L("name"), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.weight(1.2f))
                }

                LazyColumn {
                    items(sorted) { record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                        ) {
                            Text(text = record.formattedDate, color = Color(0xCCFFFFFF), fontSize = 11.sp, modifier = Modifier.weight(2f))
                            Text(text = "${record.survivalTime}s", color = Color(0xCCFFFFFF), fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text(text = "${record.score}", color = Color(0xCCFFFFFF), fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text(text = record.mode, color = Color(0xCCFFFFFF), fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                            Text(text = record.playerName, color = Color(0xCCFFFFFF), fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                        }
                    }
                }
            }
        }
    }
}
