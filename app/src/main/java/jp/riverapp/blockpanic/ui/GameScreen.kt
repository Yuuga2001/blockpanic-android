package jp.riverapp.blockpanic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.viewinterop.AndroidView
import jp.riverapp.blockpanic.game.GameCoordinator
import jp.riverapp.blockpanic.game.GameScreen as GameScreenEnum
import jp.riverapp.blockpanic.i18n.L
import jp.riverapp.blockpanic.model.GameMode
import jp.riverapp.blockpanic.rendering.GameSurfaceView

/**
 * Main screen router. Uses GameCoordinator's stateFlow for screen switching.
 * Integrates GameSurfaceView via AndroidView.
 */
@Composable
fun GameScreen(coordinator: GameCoordinator) {
    val screen by coordinator.screenState.collectAsState()
    var playerName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.sceneBg)
    ) {
        // Background game rendering (always visible — shows bot play on start screen)
        key("gameSurface") {
            AndroidView(
                factory = { ctx ->
                    GameSurfaceView(ctx).apply {
                        this.coordinator = coordinator
                    }
                },
                update = { view ->
                    view.coordinator = coordinator
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        when (screen) {
            GameScreenEnum.START -> {
                StartScreen(
                    playerName = playerName,
                    onNameChange = { playerName = it },
                    onPlay = { coordinator.startSolo(it) },
                    onOnline = { coordinator.showRoomList() },
                    onRecords = { coordinator.showRecords() },
                    onHowToPlay = { coordinator.showHowToPlay() },
                    onSettings = { coordinator.showSettings() },
                    onLanguage = { coordinator.showLanguageSelect() }
                )
            }

            GameScreenEnum.GAME -> {
                // Controls overlay
                GameControlsOverlay(coordinator)

                // Top bar: room name (center) + RETRY button (right)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    // Room name (online modes)
                    if ((coordinator.mode == GameMode.P2P_HOST || coordinator.mode == GameMode.P2P_CLIENT)
                        && coordinator.roomName.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xCC0F3460))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Box(modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Green))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${coordinator.roomName}${L("room_suffix")}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // RETRY button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { coordinator.giveUp() }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = L("retry"),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            GameScreenEnum.GAME_OVER -> {

                GameOverOverlay(
                    survivalTime = coordinator.survivalTime,
                    finalScore = coordinator.finalScore,
                    mode = coordinator.mode,
                    onPlayAgain = { coordinator.retry() },
                    onExitRoom = when (coordinator.mode) {
                        GameMode.P2P_CLIENT -> {{ coordinator.exitRoom() }}
                        GameMode.P2P_HOST -> {{ coordinator.leaveRoom() }}
                        else -> null
                    }
                )
            }

            GameScreenEnum.CONNECTING -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GameColors.sceneBg)
                ) {
                    Text(
                        text = L("connecting"),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            GameScreenEnum.HOST_DISCONNECTED -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GameColors.sceneBg)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = L("host_disconnected"),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = L("host_left"),
                            color = GameColors.textSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(GameColors.playButton)
                                .clickable { coordinator.exitRoom() }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = L("back_to_menu"),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            GameScreenEnum.ROOM_LIST -> {
                RoomListScreen(
                    playerName = playerName,
                    onCreateRoom = { coordinator.startHost(it) },
                    onJoinRoom = { roomId, name, hostName ->
                        coordinator.joinRoom(roomId, name, hostName)
                    },
                    onBack = { coordinator.currentScreen = GameScreenEnum.START }
                )
            }

            GameScreenEnum.RECORDS -> {
                RecordsScreen(onBack = { coordinator.currentScreen = GameScreenEnum.START })
            }

            GameScreenEnum.HOW_TO_PLAY -> {
                HowToPlayScreen(onBack = { coordinator.currentScreen = GameScreenEnum.START })
            }

            GameScreenEnum.SETTINGS -> {
                SettingsScreen(
                    onBack = { coordinator.currentScreen = GameScreenEnum.START },
                    onLanguage = { coordinator.showLanguageSelect() }
                )
            }

            GameScreenEnum.LANGUAGE_SELECT -> {
                LanguageSelectScreen(onBack = { coordinator.currentScreen = GameScreenEnum.SETTINGS })
            }
        }
    }
}
