package jp.riverapp.blockpanic.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.riverapp.blockpanic.game.GameCoordinator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameControlsOverlay(coordinator: GameCoordinator) {
    val buttonSize = 60.dp
    val buttonOpacity = 0.15f
    val scope = rememberCoroutineScope()

    var activeDirection by remember { mutableStateOf<Direction?>(null) }
    var jumpPressed by remember { mutableStateOf(false) }

    val jumpScale by animateFloatAsState(if (jumpPressed) 0.88f else 1f, tween(80))
    val leftScale by animateFloatAsState(if (activeDirection == Direction.LEFT) 0.88f else 1f, tween(80))
    val rightScale by animateFloatAsState(if (activeDirection == Direction.RIGHT) 0.88f else 1f, tween(80))

    Box(modifier = Modifier.fillMaxSize()) {
        // Direction pad (left side, vertically centered)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .pointerInput(Unit) {
                    val totalWidthPx = (buttonSize * 2 + 12.dp).toPx()
                    val midX = totalWidthPx / 2

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val newDir = if (down.position.x < midX) Direction.LEFT else Direction.RIGHT
                        activeDirection = newDir
                        when (newDir) {
                            Direction.LEFT -> coordinator.applyInput(left = true, right = false, jump = false)
                            Direction.RIGHT -> coordinator.applyInput(left = false, right = true, jump = false)
                        }

                        // Track movement until release
                        var up = false
                        while (!up) {
                            val event = awaitPointerEvent()
                            val pos = event.changes.firstOrNull()?.position
                            if (pos != null) {
                                val currentDir = if (pos.x < midX) Direction.LEFT else Direction.RIGHT
                                if (currentDir != activeDirection) {
                                    activeDirection = currentDir
                                    when (currentDir) {
                                        Direction.LEFT -> coordinator.applyInput(left = true, right = false, jump = false)
                                        Direction.RIGHT -> coordinator.applyInput(left = false, right = true, jump = false)
                                    }
                                }
                            }
                            up = event.changes.all { !it.pressed }
                        }

                        activeDirection = null
                        coordinator.applyInput(left = false, right = false, jump = false)
                    }
                }
        ) {
            // Left button visual
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(buttonSize)
                    .scale(leftScale)
                    .clip(CircleShape)
                    .background(
                        Color.White.copy(alpha = if (activeDirection == Direction.LEFT) 0.35f else buttonOpacity)
                    )
            ) {
                Text("◀", color = Color.White.copy(alpha = if (activeDirection == Direction.LEFT) 0.9f else 0.5f),
                    fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            // Right button visual
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(buttonSize)
                    .scale(rightScale)
                    .clip(CircleShape)
                    .background(
                        Color.White.copy(alpha = if (activeDirection == Direction.RIGHT) 0.35f else buttonOpacity)
                    )
            ) {
                Text("▶", color = Color.White.copy(alpha = if (activeDirection == Direction.RIGHT) 0.9f else 0.5f),
                    fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Jump button (right side, vertically centered)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(buttonSize * 1.2f)
                .scale(jumpScale)
                .clip(CircleShape)
                .background(
                    GameColors.playButton.copy(alpha = if (jumpPressed) 0.4f else buttonOpacity)
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        if (!jumpPressed) {
                            jumpPressed = true
                            coordinator.applyInput(
                                left = activeDirection == Direction.LEFT,
                                right = activeDirection == Direction.RIGHT,
                                jump = true
                            )
                            scope.launch {
                                delay(50)
                                coordinator.applyInput(
                                    left = activeDirection == Direction.LEFT,
                                    right = activeDirection == Direction.RIGHT,
                                    jump = false
                                )
                            }
                        }
                        waitForUpOrCancellation()
                        jumpPressed = false
                    }
                }
        ) {
            Text("▲", color = Color.White.copy(alpha = if (jumpPressed) 0.9f else 0.5f),
                fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private enum class Direction { LEFT, RIGHT }
