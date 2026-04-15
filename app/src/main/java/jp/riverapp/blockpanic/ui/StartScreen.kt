package jp.riverapp.blockpanic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.riverapp.blockpanic.i18n.L

@Composable
fun StartScreen(
    playerName: String,
    onNameChange: (String) -> Unit,
    onPlay: (String) -> Unit,
    onOnline: () -> Unit,
    onRecords: () -> Unit,
    onLeaderboard: () -> Unit,
    onHowToPlay: () -> Unit,
    onSettings: () -> Unit,
    onLanguage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Block letter logo
            BlockLetterLogo()

            Spacer(modifier = Modifier.height(24.dp))

            // Name input
            BasicTextField(
                value = playerName,
                onValueChange = { onNameChange(it.take(20)) },
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .width(240.dp)
                    .background(Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center) {
                        if (playerName.isEmpty()) {
                            Text(
                                text = L("your_name"),
                                color = Color(0x66FFFFFF),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PLAY button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GameColors.playButton)
                    .clickable { onPlay(playerName) }
            ) {
                Text(
                    text = L("play"),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ONLINE button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GameColors.onlineButton)
                    .clickable { onOnline() }
            ) {
                Text(
                    text = L("online"),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Right-side icon buttons (flat design, Material Icons)
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
        ) {
            FlatIconButton(icon = Icons.Filled.MenuBook, onClick = onRecords)
            Spacer(modifier = Modifier.height(16.dp))
            FlatIconButton(icon = Icons.Filled.EmojiEvents, onClick = onLeaderboard)
            Spacer(modifier = Modifier.height(16.dp))
            FlatIconButton(icon = Icons.Filled.HelpOutline, onClick = onHowToPlay)
            Spacer(modifier = Modifier.height(16.dp))
            FlatIconButton(icon = Icons.Filled.Settings, onClick = onSettings)
            Spacer(modifier = Modifier.height(16.dp))
            FlatIconButton(icon = Icons.Filled.Language, onClick = onLanguage)
        }
    }
}

@Composable
private fun FlatIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun BlockLetterLogo() {
    data class LetterDef(val char: String, val bg: Color, val fg: Color)

    val letters = listOf(
        LetterDef("B", Color(0xFF00D4D4), Color(0xFF003838)),
        LetterDef("L", Color(0xFFE8D44D), Color(0xFF3A3500)),
        LetterDef("O", Color(0xFFA64DDB), Color(0xFF1E0033)),
        LetterDef("C", Color(0xFF4DDB4D), Color(0xFF003A00)),
        LetterDef("K", Color(0xFFE94560), Color(0xFF2A0008)),
        LetterDef(" ", Color.Transparent, Color.Transparent),
        LetterDef("P", Color(0xFFE8943A), Color(0xFF3A2000)),
        LetterDef("A", Color(0xFF4488FF), Color(0xFF001A40)),
        LetterDef("N", Color(0xFFE94560), Color(0xFF2A0008)),
        LetterDef("I", Color(0xFF00D4D4), Color(0xFF003838)),
        LetterDef("C", Color(0xFFE8D44D), Color(0xFF3A3500)),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (letter in letters) {
            if (letter.char == " ") {
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .shadow(3.dp, RoundedCornerShape(4.dp), clip = false)
                        .clip(RoundedCornerShape(4.dp))
                        .background(letter.bg)
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = letter.char,
                        color = letter.fg,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
