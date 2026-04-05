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
import jp.riverapp.blockpanic.engine.C
import jp.riverapp.blockpanic.i18n.L
import jp.riverapp.blockpanic.network.RoomInfo
import jp.riverapp.blockpanic.network.SignalingClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun RoomListScreen(
    playerName: String,
    onCreateRoom: (String) -> Unit,
    onJoinRoom: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var rooms by remember { mutableStateOf<List<RoomInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val signalingClient = remember { SignalingClient() }

    LaunchedEffect(Unit) {
        while (isActive) {
            try { rooms = signalingClient.listRooms() } catch (_: Exception) {}
            loading = false
            delay(5000)
        }
    }

    // Full screen tap-to-dismiss backdrop
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onBack() }
    ) {
        // Compact centered panel (matching iOS: maxWidth 500, maxHeight 350)
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .heightIn(max = 350.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC1A1A2E))
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { /* prevent dismiss */ }
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
                    text = L("online"),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GameColors.playButton)
                        .clickable { onCreateRoom(playerName) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(L("create"), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Room list content
            if (loading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Text(L("loading_rooms"), color = GameColors.textSecondary, fontSize = 14.sp)
                }
            } else if (rooms.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(L("no_rooms"), color = Color.White, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(L("no_rooms_hint"), color = GameColors.textMuted, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(rooms) { room ->
                        val isFull = room.playerCount >= C.maxRoomPlayers
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x0DFFFFFF))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    room.hostName + L("room_suffix"),
                                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${room.playerCount} / ${C.maxRoomPlayers} ${L("players_suffix")}",
                                    color = GameColors.textMuted, fontSize = 11.sp
                                )
                            }
                            if (isFull) {
                                Text(L("full"), color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GameColors.onlineButton)
                                        .clickable { onJoinRoom(room.roomId, playerName, room.hostName) }
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(L("join"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
