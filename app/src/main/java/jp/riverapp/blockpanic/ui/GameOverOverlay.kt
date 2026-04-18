package jp.riverapp.blockpanic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import jp.riverapp.blockpanic.model.GameMode

@Composable
fun GameOverOverlay(
    survivalTime: Int,
    finalScore: Int,
    mode: GameMode,
    onPlayAgain: () -> Unit,
    onExitRoom: (() -> Unit)?,
    onBackToTitle: (() -> Unit)? = null
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC1A1A2E))
                .padding(32.dp)
        ) {
            Text(
                text = L("game_over"),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            // TIME (small, secondary)
            Text(
                text = "${L("time_label")}: ${survivalTime}s",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            // SCORE (large, primary)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = L("score_label"),
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${finalScore} pt",
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // PLAY AGAIN button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GameColors.playButton)
                    .clickable { onPlayAgain() }
            ) {
                Text(
                    text = L("play_again"),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // EXIT ROOM button (for online modes)
            if (onExitRoom != null && mode != GameMode.LOCAL) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(200.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable { onExitRoom() }
                ) {
                    Text(
                        text = L(if (mode == GameMode.P2P_HOST) "close_room" else "exit_room"),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // BACK TO TITLE button (solo mode only)
            if (mode == GameMode.LOCAL && onBackToTitle != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(200.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable { onBackToTitle() }
                ) {
                    Text(
                        text = L("back_to_title"),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
