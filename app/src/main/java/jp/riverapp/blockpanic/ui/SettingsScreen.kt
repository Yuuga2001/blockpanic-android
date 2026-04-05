package jp.riverapp.blockpanic.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.riverapp.blockpanic.i18n.L
import jp.riverapp.blockpanic.model.GameRecordStore

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLanguage: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val appVersion = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        "${pInfo.versionName} (${pInfo.longVersionCode})"
    } catch (_: Exception) { "1.0" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .heightIn(max = 360.dp)
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
                    text = L("settings"),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(60.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Information
                SectionHeader(L("information"))
                SettingsLink(L("about_app"), Color(0xFF4488FF)) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://riverapp.jp/app-document/blockpanic/about")))
                }
                SettingsLink(L("privacy_policy"), Color(0xFF4DDB4D)) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://riverapp.jp/app-document/blockpanic/privacy-policy")))
                }
                SettingsLink(L("terms_of_service"), Color(0xFFE8943A)) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://riverapp.jp/app-document/blockpanic/terms-of-service")))
                }
                SettingsLink(L("contact"), Color(0xFFA64DDB)) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://riverapp.jp/app-document/blockpanic/contact")))
                }

                // Links
                SectionHeader(L("links"))
                SettingsLink(L("play_on_web"), Color(0xFF00D4D4)) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://blockpanic.riverapp.jp/")))
                }
                SettingsLink(L("app_homepage"), Color(0xFF4488FF)) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://riverapp.jp/apps/blockpanic")))
                }

                // Language
                SectionHeader(L("language"))
                SettingsLink(L("language"), Color(0xFF00D4D4)) { onLanguage() }

                // Data
                SectionHeader(L("data"))
                SettingsLink(L("delete_data"), Color(0xFFE94560)) { showDeleteConfirm = true }

                // Version
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${L("version")} $appVersion",
                    color = Color(0x66808080),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(L("delete_confirm_title")) },
            text = { Text(L("delete_confirm_msg")) },
            confirmButton = {
                TextButton(onClick = {
                    GameRecordStore.clear()
                    showDeleteConfirm = false
                }) {
                    Text(L("delete"), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(L("cancel"))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsLink(label: String, color: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x0DFFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = label, color = Color(0xD9FFFFFF), fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = ">", color = Color(0x4DFFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(2.dp))
}
