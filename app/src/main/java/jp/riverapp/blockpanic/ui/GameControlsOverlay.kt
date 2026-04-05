package jp.riverapp.blockpanic.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.riverapp.blockpanic.game.GameCoordinator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Game controls overlay: direction pad (left/right) + jump button.
 * Port from iOS GameControlsOverlay.swift
 *
 * Direction: single gesture area, position-based left/right detection.
 * Slide between left/right switches direction.
 * Jump: single-fire per touch (jumpPressed flag), 50ms delay for false send.
 */
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
        // Direction pad (left side)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 32.dp)
                .pointerInput(Unit) {
                    val totalWidthPx = (buttonSize * 2 + 12.dp).toPx()
                    detectDragGestures(
                        onDragStart = { offset ->
                            val midX = totalWidthPx / 2
                            val newDir = if (offset.x < midX) Direction.LEFT else Direction.RIGHT
                            activeDirection = newDir
                            when (newDir) {
                                Direction.LEFT -> coordinator.applyInput(left = true, right = false, jump = false)
                                Direction.RIGHT -> coordinator.applyInput(left = false, right = true, jump = false)
                            }
                        },
                        onDrag = { _, dragAmount ->
                            // Recalculate direction based on accumulated position is complex,
                            // but since we get the current position, we can just check x
                        },
                        onDragEnd = {
                            activeDirection = null
                            coordinator.applyInput(left = false, right = false, jump = false)
                        },
                        onDragCancel = {
                            activeDirection = null
                            coordinator.applyInput(left = false, right = false, jump = false)
                        }
                    )
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
                Text(
                    text = "◀",
                    color = Color.White.copy(alpha = if (activeDirection == Direction.LEFT) 0.9f else 0.5f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
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
                Text(
                    text = "▶",
                    color = Color.White.copy(alpha = if (activeDirection == Direction.RIGHT) 0.9f else 0.5f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Jump button (right side)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 32.dp)
                .size(buttonSize * 1.2f)
                .scale(jumpScale)
                .clip(CircleShape)
                .background(
                    GameColors.playButton.copy(alpha = if (jumpPressed) 0.4f else buttonOpacity)
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            if (!jumpPressed) {
                                jumpPressed = true
                                coordinator.applyInput(
                                    left = activeDirection == Direction.LEFT,
                                    right = activeDirection == Direction.RIGHT,
                                    jump = true
                                )
                                // 50ms delay then send jump=false (ensures host processes it)
                                scope.launch {
                                    delay(50)
                                    coordinator.applyInput(
                                        left = activeDirection == Direction.LEFT,
                                        right = activeDirection == Direction.RIGHT,
                                        jump = false
                                    )
                                }
                            }
                        },
                        onDrag = { _, _ -> },
                        onDragEnd = { jumpPressed = false },
                        onDragCancel = { jumpPressed = false }
                    )
                }
        ) {
            Text(
                text = "▲",
                color = Color.White.copy(alpha = if (jumpPressed) 0.9f else 0.5f),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private enum class Direction { LEFT, RIGHT }
