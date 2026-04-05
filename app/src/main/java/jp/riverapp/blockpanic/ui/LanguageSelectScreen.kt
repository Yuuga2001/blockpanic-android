package jp.riverapp.blockpanic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import jp.riverapp.blockpanic.i18n.LocalizationManager

@Composable
fun LanguageSelectScreen(onBack: () -> Unit) {
    var selectedCode by remember { mutableStateOf(LocalizationManager.selectedCode) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.sceneBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .heightIn(max = 400.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x33000000))
                .padding(20.dp)
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
                    text = L("language"),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(60.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // System Default
                item {
                    LanguageRow(
                        name = L("auto_device"),
                        isSelected = selectedCode == LocalizationManager.AUTO_CODE,
                        onClick = {
                            LocalizationManager.setLanguage(LocalizationManager.AUTO_CODE)
                            selectedCode = LocalizationManager.AUTO_CODE
                        }
                    )
                }

                items(LocalizationManager.supportedLanguages) { (code, name) ->
                    LanguageRow(
                        name = name,
                        isSelected = selectedCode == code,
                        onClick = {
                            LocalizationManager.setLanguage(code)
                            selectedCode = code
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0x1AFFFFFF) else Color(0x0DFFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = name,
            color = if (isSelected) Color.White else Color(0xB3FFFFFF),
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isSelected) {
            Text(text = "✓", color = GameColors.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
