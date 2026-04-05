package jp.riverapp.blockpanic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.riverapp.blockpanic.i18n.L

@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.sceneBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
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
                    text = L("how_to_play"),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(60.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal scroll cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TipCard(
                    title = L("tip_move"),
                    lines = listOf(L("tip_move_1"), L("tip_move_2")),
                    color = Color(0xFF4488FF)
                )
                TipCard(
                    title = L("tip_jump"),
                    lines = listOf(L("tip_jump_1"), L("tip_jump_2"), L("tip_jump_3")),
                    color = Color(0xFFE94560)
                )
                TipCard(
                    title = L("tip_survive"),
                    lines = listOf(L("tip_survive_1"), L("tip_survive_2"), L("tip_survive_3"), L("tip_survive_4")),
                    color = Color(0xFF4DDB4D)
                )
                TipCard(
                    title = L("tip_coins"),
                    lines = listOf(L("tip_coins_1"), L("tip_coins_2"), L("tip_coins_3")),
                    color = Color(0xFFFFD700)
                )
                TipCard(
                    title = L("tip_mystery"),
                    lines = listOf(L("tip_mystery_1"), L("tip_mystery_2"), L("tip_mystery_3"), L("tip_mystery_4"), L("tip_mystery_5")),
                    color = Color(0xFFA64DDB)
                )
                TipCard(
                    title = L("tip_online"),
                    lines = listOf(L("tip_online_1"), L("tip_online_2"), L("tip_online_3"), L("tip_online_4")),
                    color = Color(0xFF00D4D4)
                )
            }
        }
    }
}

@Composable
private fun TipCard(title: String, lines: List<String>, color: Color) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14FFFFFF))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        for (line in lines) {
            Text(
                text = line,
                color = Color(0xCCFFFFFF),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}
