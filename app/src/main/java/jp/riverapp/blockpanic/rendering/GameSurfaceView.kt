package jp.riverapp.blockpanic.rendering

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import jp.riverapp.blockpanic.engine.*
import jp.riverapp.blockpanic.game.GameCoordinator
import jp.riverapp.blockpanic.i18n.L
import kotlin.math.*

/**
 * SurfaceView with dedicated game thread running at 60fps.
 * Renders: background grid, board separators, blocks (with 3D bevel), players (stick figures with 4-pose animation),
 * coins, mystery items (rainbow HSL cycling), HUD text, crush animations, effect popups.
 *
 * Port from iOS GameScene.swift + BlockRenderer + PlayerRenderer + ItemRenderer + AnimationManager + HUDNode
 */
class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    var coordinator: GameCoordinator? = null

    private val camera = CameraController()
    private var gameThread: GameThread? = null

    // Paints (pre-allocated for performance)
    private val bgPaint = Paint().apply { color = Color.rgb(26, 26, 46) } // #1A1A2E
    private val separatorPaint = Paint().apply {
        color = Color.argb(38, 255, 255, 255) // white 15% opacity
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val boundaryPaint = Paint().apply {
        color = Color.argb(64, 255, 255, 255) // white 25% opacity
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val blockPaint = Paint().apply { style = Paint.Style.FILL }
    private val bevelPaint = Paint().apply { style = Paint.Style.FILL }
    private val playerBodyPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    private val playerOutlinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.BLACK
        isAntiAlias = true
    }
    private val headPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        isAntiAlias = true
    }
    private val labelPaint = Paint().apply {
        color = Color.WHITE
        textSize = 10f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
    }
    private val coinFillPaint = Paint().apply {
        color = Color.rgb(255, 215, 0) // gold
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val coinStrokePaint = Paint().apply {
        color = Color.rgb(171, 102, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        isAntiAlias = true
    }
    private val coinTextPaint = Paint().apply {
        color = Color.rgb(140, 105, 20)
        textSize = 12f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
    }
    private val mysteryFillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val mysteryStrokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val mysteryTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 13f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
    }
    private val hudPaint = Paint().apply {
        color = Color.WHITE
        textSize = 18f * resources.displayMetrics.density
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
    }
    private val hudSmallPaint = Paint().apply {
        color = Color.GRAY
        textSize = 14f * resources.displayMetrics.density
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        isAntiAlias = true
    }
    private val effectPopupPaint = Paint().apply {
        textSize = 16f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
    }

    // Tetromino colors: base, light (highlight), dark (shadow)
    private val pieceColors = arrayOf(
        Triple(Color.rgb(0, 240, 240), Color.rgb(94, 255, 255), Color.rgb(0, 153, 153)),    // I cyan
        Triple(Color.rgb(240, 240, 0), Color.rgb(255, 255, 94), Color.rgb(153, 153, 0)),    // O yellow
        Triple(Color.rgb(161, 0, 240), Color.rgb(207, 94, 255), Color.rgb(97, 0, 153)),     // T purple
        Triple(Color.rgb(0, 240, 0), Color.rgb(94, 255, 94), Color.rgb(0, 153, 0)),         // S green
        Triple(Color.rgb(240, 0, 0), Color.rgb(255, 94, 94), Color.rgb(153, 0, 0)),         // Z red
        Triple(Color.rgb(240, 161, 0), Color.rgb(255, 207, 94), Color.rgb(153, 100, 0)),    // L orange
        Triple(Color.rgb(0, 0, 240), Color.rgb(94, 94, 255), Color.rgb(0, 0, 153)),         // J blue
    )
    private val bevel = 3f

    // Animation state
    private val knownDeadIds = mutableSetOf<String>()
    private data class CrushAnim(
        val cx: Float, val footY: Float, val color: Int, val pH: Float,
        val headR: Float, val startTime: Long
    )
    private val crushAnims = mutableListOf<CrushAnim>()

    private data class EffectPopup(
        val text: String, val color: Int, val x: Float, val y: Float, val startTime: Long
    )
    private val effectPopups = mutableListOf<EffectPopup>()
    private val prevEffectEndTimes = mutableMapOf<String, Double>()

    // Item disappearance animations
    private val knownCoinPositions = mutableMapOf<String, Pair<Float, Float>>()
    private val knownMysteryPositions = mutableMapOf<String, Pair<Float, Float>>()
    private val itemAnimatingIds = mutableSetOf<String>()

    private data class ItemAnim(
        val x: Float, val y: Float, val type: ItemAnimType,
        val collected: Boolean, val startTime: Long
    )
    private enum class ItemAnimType { COIN, MYSTERY }
    private val itemAnims = mutableListOf<ItemAnim>()

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameThread = GameThread(holder).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        camera.viewWidth = width.toFloat()
        camera.viewHeight = height.toFloat()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        gameThread?.running = false
        gameThread?.join()
        gameThread = null
    }

    private inner class GameThread(private val surfaceHolder: SurfaceHolder) : Thread("GameRenderThread") {
        @Volatile var running = true

        override fun run() {
            val targetFrameTime = 1_000_000_000L / 60 // 60fps in nanoseconds
            while (running) {
                val frameStart = System.nanoTime()

                // Tick game state
                coordinator?.tickIfHost()

                var canvas: Canvas? = null
                try {
                    canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) {
                        synchronized(surfaceHolder) {
                            drawFrame(canvas)
                        }
                    }
                } finally {
                    if (canvas != null) {
                        try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
                    }
                }

                val elapsed = System.nanoTime() - frameStart
                val sleepNanos = targetFrameTime - elapsed
                if (sleepNanos > 0) {
                    sleep(sleepNanos / 1_000_000, (sleepNanos % 1_000_000).toInt())
                }
            }
        }
    }

    private fun drawFrame(canvas: Canvas) {
        val coord = coordinator ?: return

        // Clear entire canvas
        canvas.drawColor(Color.rgb(15, 15, 30)) // #0F0F1E

        canvas.save()
        camera.applyTransform(canvas)

        // Background
        canvas.drawRect(0f, 0f, C.gameWidth.toFloat(), C.gameHeight.toFloat(), bgPaint)

        // Stage boundaries (x=0 and x=900)
        canvas.drawLine(0f, 0f, 0f, C.gameHeight.toFloat(), boundaryPaint)
        canvas.drawLine(C.gameWidth.toFloat(), 0f, C.gameWidth.toFloat(), C.gameHeight.toFloat(), boundaryPaint)

        // Board separators (x=300 and x=600)
        for (i in 1 until C.boardCount) {
            val x = (i * C.boardWidth).toFloat()
            canvas.drawLine(x, 0f, x, C.gameHeight.toFloat(), separatorPaint)
        }

        // Draw blocks
        val blocks = coord.blocks
        for (block in blocks) {
            drawBlock(canvas, block)
        }

        // Draw coins + track disappearance
        val coins = coord.coins
        val activeCoinIds = mutableSetOf<String>()
        for (coin in coins) {
            if (coin.active) {
                activeCoinIds.add(coin.id)
                knownCoinPositions[coin.id] = Pair(coin.x.toFloat(), coin.y.toFloat())
                drawCoin(canvas, coin)
            } else {
                val pos = knownCoinPositions.remove(coin.id)
                if (pos != null && coin.id !in itemAnimatingIds) {
                    itemAnimatingIds.add(coin.id)
                    itemAnims.add(ItemAnim(pos.first, pos.second,
                        ItemAnimType.COIN, coin.collected, System.currentTimeMillis()))
                }
            }
        }
        // Clean up coins no longer in state
        knownCoinPositions.keys.removeAll { it !in activeCoinIds && coins.none { c -> c.id == it } }

        // Draw mystery items + track disappearance
        val mysteryItems = coord.mysteryItems
        val activeMysteryIds = mutableSetOf<String>()
        for (item in mysteryItems) {
            if (item.active) {
                activeMysteryIds.add(item.id)
                knownMysteryPositions[item.id] = Pair(item.x.toFloat(), item.y.toFloat())
                drawMysteryItem(canvas, item)
            } else {
                val pos = knownMysteryPositions.remove(item.id)
                if (pos != null && item.id !in itemAnimatingIds) {
                    itemAnimatingIds.add(item.id)
                    itemAnims.add(ItemAnim(pos.first, pos.second,
                        ItemAnimType.MYSTERY, item.collected, System.currentTimeMillis()))
                }
            }
        }
        knownMysteryPositions.keys.removeAll { it !in activeMysteryIds && mysteryItems.none { m -> m.id == it } }

        // Draw players
        val players = coord.players
        val selfId = coord.effectivePlayerId
        for (player in players) {
            if (!player.alive) continue
            drawPlayer(canvas, player, player.id == selfId)
        }

        // Process death animations
        processDeaths(canvas, players, coord.effectiveLastSelfId)

        // Draw crush animations
        drawCrushAnimations(canvas)

        // Effect popups (mystery item acquired)
        processEffectPopups(players)
        drawEffectPopups(canvas)

        // Item disappearance animations
        drawItemAnims(canvas)

        canvas.restore()

        // HUD (drawn in screen space, not game space)
        drawHUD(canvas, coord)
    }

    // MARK: - Block rendering with 3D bevel

    private fun drawBlock(canvas: Canvas, block: BlockState) {
        val x = block.x.toFloat()
        val y = block.y.toFloat()
        val w = block.width.toFloat()
        val h = block.height.toFloat()
        val type = block.pieceType.coerceIn(0, pieceColors.size - 1)
        val (base, light, dark) = pieceColors[type]

        // Base fill
        blockPaint.color = base
        canvas.drawRect(x, y, x + w, y + h, blockPaint)

        // Top highlight
        bevelPaint.color = light
        canvas.drawRect(x, y, x + w, y + bevel, bevelPaint)
        // Left highlight
        canvas.drawRect(x, y, x + bevel, y + h, bevelPaint)
        // Bottom shadow
        bevelPaint.color = dark
        canvas.drawRect(x, y + h - bevel, x + w, y + h, bevelPaint)
        // Right shadow
        canvas.drawRect(x + w - bevel, y, x + w, y + h, bevelPaint)
        // Inner face
        blockPaint.color = base
        canvas.drawRect(x + bevel, y + bevel, x + w - bevel, y + h - bevel, blockPaint)
    }

    // MARK: - Player stick figure rendering

    private fun drawPlayer(canvas: Canvas, player: PlayerState, isSelf: Boolean) {
        val pH = (C.playerHeight * player.heightMultiplier).toFloat()
        val cx = (player.x + C.playerWidth / 2).toFloat()
        val topY = player.y.toFloat()

        val color = if (isSelf) Color.rgb(255, 69, 69) // red for self
        else if (player.type == PlayerType.CPU) Color.GRAY
        else Color.rgb(69, 135, 255) // blue for others

        // 頭のスケール: 体ほど大きくならない（sqrt で緩やか。BIG=1.41x, MINI=0.71x）
        val headScale = max(Math.sqrt(player.heightMultiplier), 0.7)
        val headR = (6.0 * headScale).toFloat()
        val headY = topY + headR
        val neckY = headY + headR
        val shoulderY = neckY + 4
        val bodyEndY = topY + pH * 0.6f
        val legEndY = topY + pH
        val hipY = bodyEndY
        val midLegY = hipY + (legEndY - hipY) * 0.5f
        val midArmY = shoulderY + (legEndY - shoulderY) * 0.15f

        // Limb positions
        var elbowLx: Float; var elbowLy: Float; var handLx: Float; var handLy: Float
        var elbowRx: Float; var elbowRy: Float; var handRx: Float; var handRy: Float
        var kneeLx: Float; var kneeLy: Float; var footLx: Float; var footLy: Float
        var kneeRx: Float; var kneeRy: Float; var footRx: Float; var footRy: Float

        val moveIntent = player.inputVx.toFloat()
        val vy = player.vy.toFloat()
        val onGround = player.onGround

        if (!onGround && vy < -2) {
            // JUMP: arms raised with bent elbows, knees tucked up
            // iOS: shoulderY+1 = below shoulder (SpriteKit Y up, -offset=down)
            // Android: shoulderY-1 = above shoulder (Canvas Y down, -offset=up)
            elbowLx = cx - 7; elbowLy = shoulderY - 1; handLx = cx - 4; handLy = shoulderY - 5
            elbowRx = cx + 7; elbowRy = shoulderY - 1; handRx = cx + 4; handRy = shoulderY - 5
            kneeLx = cx - 6; kneeLy = midLegY - 3; footLx = cx - 8; footLy = midLegY + 4
            kneeRx = cx + 6; kneeRy = midLegY - 3; footRx = cx + 8; footRy = midLegY + 4
        } else if (!onGround) {
            // FALL: arms spread wide, legs dangling
            elbowLx = cx - 8; elbowLy = shoulderY; handLx = cx - 10; handLy = shoulderY + 4
            elbowRx = cx + 8; elbowRy = shoulderY; handRx = cx + 10; handRy = shoulderY + 4
            kneeLx = cx - 4; kneeLy = midLegY + 2; footLx = cx - 6; footLy = legEndY
            kneeRx = cx + 4; kneeRy = midLegY + 2; footRx = cx + 6; footRy = legEndY
        } else if (moveIntent != 0f) {
            // RUN: arms pumping, legs with knee lift
            val t = (System.nanoTime() / 1_000_000_000.0 / 0.12).toFloat()
            val s = sin(t)
            val armLen = 5f
            val legLen = 8f

            elbowLx = cx - 3; elbowLy = shoulderY + 3 - s * 3
            handLx = cx - s * armLen; handLy = shoulderY + 2 - s * 2
            elbowRx = cx + 3; elbowRy = shoulderY + 3 + s * 3
            handRx = cx + s * armLen; handRy = shoulderY + 2 + s * 2

            val kneeLift = max(s, 0f) * 6
            val kneeBack = max(-s, 0f) * 6
            kneeLx = cx - s * 4; kneeLy = midLegY - kneeLift
            footLx = cx - s * legLen; footLy = legEndY
            kneeRx = cx + s * 4; kneeRy = midLegY - kneeBack
            footRx = cx + s * legLen; footRy = legEndY
        } else {
            // IDLE: straight arms down, legs apart
            elbowLx = cx - 4; elbowLy = midArmY; handLx = cx - 7; handLy = midArmY + 5
            elbowRx = cx + 4; elbowRy = midArmY; handRx = cx + 7; handRy = midArmY + 5
            kneeLx = cx - 4; kneeLy = midLegY; footLx = cx - 8; footLy = legEndY
            kneeRx = cx + 4; kneeRy = midLegY; footRx = cx + 8; footRy = legEndY
        }

        // Build path
        val path = Path()

        // Body line (neck -> hip)
        path.moveTo(cx, neckY)
        path.lineTo(cx, bodyEndY)

        // Left arm
        path.moveTo(cx, shoulderY)
        path.lineTo(elbowLx, elbowLy)
        path.lineTo(handLx, handLy)

        // Right arm
        path.moveTo(cx, shoulderY)
        path.lineTo(elbowRx, elbowRy)
        path.lineTo(handRx, handRy)

        // Left leg
        path.moveTo(cx, hipY)
        path.lineTo(kneeLx, kneeLy)
        path.lineTo(footLx, footLy)

        // Right leg
        path.moveTo(cx, hipY)
        path.lineTo(kneeRx, kneeRy)
        path.lineTo(footRx, footRy)

        // Draw outline then body
        canvas.drawPath(path, playerOutlinePaint)
        playerBodyPaint.color = color
        canvas.drawPath(path, playerBodyPaint)

        // Head
        headPaint.color = color
        headPaint.style = Paint.Style.FILL
        canvas.drawCircle(cx, headY, headR, headPaint)
        headPaint.color = Color.BLACK
        headPaint.style = Paint.Style.STROKE
        headPaint.strokeWidth = 1f
        canvas.drawCircle(cx, headY, headR, headPaint)

        // Name label
        labelPaint.textSize = if (isSelf) 13f else 10f
        labelPaint.color = if (isSelf) Color.WHITE else Color.argb(204, 255, 255, 255)
        canvas.drawText(player.name, cx, topY - 4, labelPaint)
    }

    // MARK: - Coin rendering

    private fun drawCoin(canvas: Canvas, coin: CoinState) {
        val x = coin.x.toFloat()
        val y = coin.y.toFloat()
        val r = C.coinRadius.toFloat()
        canvas.drawCircle(x, y, r, coinFillPaint)
        canvas.drawCircle(x, y, r, coinStrokePaint)
        canvas.drawText("$", x, y + coinTextPaint.textSize * 0.35f, coinTextPaint)
    }

    // MARK: - Mystery item rendering (rainbow HSL cycling)

    private fun drawMysteryItem(canvas: Canvas, item: MysteryItemState) {
        val x = item.x.toFloat()
        val y = item.y.toFloat()
        val r = C.mysteryRadius.toFloat()

        val t = System.nanoTime() / 1_000_000_000.0
        val hue = ((t * 60.0) % 360.0).toFloat()

        val hsv = floatArrayOf(hue, 0.9f, 0.9f)
        mysteryFillPaint.color = Color.HSVToColor(hsv)

        val hsv2 = floatArrayOf((hue + 120f) % 360f, 0.8f, 1.0f)
        mysteryStrokePaint.color = Color.HSVToColor(230, hsv2)

        canvas.drawCircle(x, y, r, mysteryFillPaint)
        canvas.drawCircle(x, y, r, mysteryStrokePaint)
        canvas.drawText("?", x, y + mysteryTextPaint.textSize * 0.35f, mysteryTextPaint)
    }

    // MARK: - Death/crush animations

    private fun processDeaths(canvas: Canvas, players: List<PlayerState>, selfId: String) {
        for (player in players) {
            if (!player.alive && player.id !in knownDeadIds) {
                knownDeadIds.add(player.id)
                val isSelf = player.id == selfId
                val color = if (isSelf) Color.rgb(255, 69, 69)
                else if (player.type == PlayerType.CPU) Color.GRAY
                else Color.rgb(69, 135, 255)

                val pH = (C.playerHeight * player.heightMultiplier).toFloat()
                val cx = (player.x + C.playerWidth / 2).toFloat()
                val footY = (player.y + pH).toFloat()
                val headR = (6.0 * max(Math.sqrt(player.heightMultiplier), 0.7)).toFloat()

                crushAnims.add(CrushAnim(cx, footY, color, pH, headR, System.currentTimeMillis()))
            }
        }

        // Remove from known when player respawns
        val aliveIds = players.filter { it.alive }.map { it.id }.toSet()
        knownDeadIds.removeAll { it in aliveIds }
    }

    private fun drawCrushAnimations(canvas: Canvas) {
        val now = System.currentTimeMillis()
        val toRemove = mutableListOf<CrushAnim>()

        for (anim in crushAnims) {
            val elapsed = (now - anim.startTime) / 1000f
            if (elapsed > 0.6f) {
                toRemove.add(anim)
                continue
            }

            canvas.save()
            canvas.translate(anim.cx, anim.footY)

            val scaleX: Float
            val scaleY: Float
            val alpha: Int

            if (elapsed < 0.3f) {
                // Phase 1: squish
                val t = elapsed / 0.3f
                scaleX = 1f + 0.8f * t
                scaleY = 1f - 0.85f * t
                alpha = 255
            } else {
                // Phase 2: fade
                val t = (elapsed - 0.3f) / 0.3f
                scaleX = 1.8f
                scaleY = 0.15f
                alpha = (255 * (1f - t)).toInt().coerceIn(0, 255)
            }

            canvas.scale(scaleX, scaleY)

            // Draw simplified stick figure
            val paint = Paint().apply {
                color = anim.color
                this.alpha = alpha
                style = Paint.Style.STROKE
                strokeWidth = 2f
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
            }
            val outPaint = Paint().apply {
                color = Color.BLACK
                this.alpha = alpha
                style = Paint.Style.STROKE
                strokeWidth = 4f
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
            }

            val pH = anim.pH
            val neckY = -pH + anim.headR * 2
            val shoulderY = neckY + 4
            val hipY = -pH * 0.4f

            val path = Path().apply {
                moveTo(0f, neckY); lineTo(0f, hipY)
                moveTo(0f, shoulderY); lineTo(-4f, shoulderY - 5); lineTo(-7f, shoulderY - 10)
                moveTo(0f, shoulderY); lineTo(4f, shoulderY - 5); lineTo(7f, shoulderY - 10)
                moveTo(0f, hipY); lineTo(-4f, hipY * 0.5f); lineTo(-8f, 0f)
                moveTo(0f, hipY); lineTo(4f, hipY * 0.5f); lineTo(8f, 0f)
            }
            canvas.drawPath(path, outPaint)
            canvas.drawPath(path, paint)

            // Head
            val headFill = Paint().apply {
                color = anim.color; this.alpha = alpha; style = Paint.Style.FILL; isAntiAlias = true
            }
            canvas.drawCircle(0f, -pH + anim.headR, anim.headR, headFill)

            canvas.restore()
        }
        crushAnims.removeAll(toRemove)
    }

    // MARK: - Effect popups

    private fun processEffectPopups(players: List<PlayerState>) {
        for (player in players) {
            if (!player.alive) continue
            val prevEnd = prevEffectEndTimes[player.id] ?: 0.0
            if (player.effectEndTime != prevEnd && player.effectEndTime > 0) {
                val effect = player.activeEffect
                if (effect != null) {
                    val text: String
                    val color: Int
                    when (effect) {
                        MysteryEffect.POINTS -> { text = L("effect_points"); color = Color.rgb(255, 215, 0) }
                        MysteryEffect.SHRINK -> { text = L("effect_mini"); color = Color.rgb(77, 204, 255) }
                        MysteryEffect.GROW -> { text = L("effect_big"); color = Color.rgb(255, 102, 102) }
                        MysteryEffect.SUPERJUMP -> { text = L("effect_jump"); color = Color.rgb(102, 255, 102) }
                    }
                    effectPopups.add(EffectPopup(
                        text = text, color = color,
                        x = (player.x + C.playerWidth / 2).toFloat(),
                        y = player.y.toFloat() - 10,
                        startTime = System.currentTimeMillis()
                    ))
                }
            }
            prevEffectEndTimes[player.id] = player.effectEndTime
        }
    }

    private fun drawEffectPopups(canvas: Canvas) {
        val now = System.currentTimeMillis()
        val toRemove = mutableListOf<EffectPopup>()

        for (popup in effectPopups) {
            val elapsed = (now - popup.startTime) / 1000f
            if (elapsed > 0.8f) {
                toRemove.add(popup)
                continue
            }

            val yOffset = -25f * elapsed / 0.8f
            val alpha = if (elapsed < 0.48f) 255
            else (255 * (1f - (elapsed - 0.48f) / 0.32f)).toInt().coerceIn(0, 255)

            effectPopupPaint.color = popup.color
            effectPopupPaint.alpha = alpha
            canvas.drawText(popup.text, popup.x, popup.y + yOffset, effectPopupPaint)
        }
        effectPopups.removeAll(toRemove)
    }

    // MARK: - Item disappearance animations

    private fun drawItemAnims(canvas: Canvas) {
        val now = System.currentTimeMillis()
        val duration = 500L // 500ms
        val toRemove = mutableListOf<ItemAnim>()
        val paint = Paint().apply { isAntiAlias = true }

        for (anim in itemAnims) {
            val elapsed = now - anim.startTime
            if (elapsed > duration) { toRemove.add(anim); continue }
            val progress = elapsed.toFloat() / duration // 0→1

            if (anim.collected) {
                // Collect: particles burst + floating text
                drawCollectEffect(canvas, anim, progress, paint)
            } else {
                // Expire/crushed: shrink + fade
                drawExpireEffect(canvas, anim, progress, paint)
            }
        }
        itemAnims.removeAll(toRemove)
    }

    private fun drawExpireEffect(canvas: Canvas, anim: ItemAnim, progress: Float, paint: Paint) {
        val scale = 1f - progress
        val alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
        if (scale <= 0 || alpha <= 0) return

        val r = C.coinRadius.toFloat() * scale
        paint.style = Paint.Style.FILL

        if (anim.type == ItemAnimType.COIN) {
            paint.color = Color.rgb(255, 215, 0)
            paint.alpha = alpha
            canvas.drawCircle(anim.x, anim.y, r, paint)
        } else {
            // Mystery: rainbow cycling while shrinking
            val hue = ((System.currentTimeMillis() * 100L / 1000) % 360).toFloat()
            val hsv = floatArrayOf(hue, 1f, 0.6f)
            paint.color = Color.HSVToColor(alpha, hsv)
            canvas.drawCircle(anim.x, anim.y, r, paint)
        }
    }

    private fun drawCollectEffect(canvas: Canvas, anim: ItemAnim, progress: Float, paint: Paint) {
        val alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)

        if (anim.type == ItemAnimType.COIN) {
            // Floating "+100" text
            val textPaint = Paint().apply {
                color = Color.rgb(255, 215, 0)
                this.alpha = alpha
                textSize = 14f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val yOffset = -progress * 25f
            canvas.drawText("+${C.coinScore}", anim.x, anim.y + yOffset, textPaint)

            // Particles
            paint.style = Paint.Style.FILL
            for (i in 0 until 6) {
                val angle = (i.toDouble() / 6 * Math.PI * 2).toFloat()
                val dist = 20f * progress
                val px = anim.x + cos(angle) * dist
                val py = anim.y + sin(angle) * dist
                val pScale = 1f - progress
                paint.color = Color.rgb(255, 215, 0)
                paint.alpha = alpha
                canvas.drawCircle(px, py, 2.5f * pScale, paint)
            }
        } else {
            // Mystery: rainbow particles
            paint.style = Paint.Style.FILL
            for (i in 0 until 8) {
                val angle = (i.toDouble() / 8 * Math.PI * 2).toFloat()
                val dist = 25f * progress
                val px = anim.x + cos(angle) * dist
                val py = anim.y + sin(angle) * dist
                val pScale = 1f - progress
                val hue = (i.toFloat() / 8f * 360f)
                val hsv = floatArrayOf(hue, 1f, 1f)
                paint.color = Color.HSVToColor(alpha, hsv)
                canvas.drawCircle(px, py, 3f * pScale, paint)
            }
        }
    }

    // MARK: - HUD rendering (screen space)

    private fun drawHUD(canvas: Canvas, coord: GameCoordinator) {
        val density = resources.displayMetrics.density
        val padding = 24f * density  // extra right offset for rounded corners
        val safeTop = 20f * density  // extra down offset for rounded corners

        val selfPlayer = coord.players.find { it.id == coord.effectivePlayerId }
        if (coord.effectivePlayerId.isNotEmpty() && selfPlayer != null) {
            val time = coord.survivalTime
            val lineH = 24f * density
            canvas.drawText("${L("hud_time")}: ${time}s", padding, safeTop, hudPaint)
            canvas.drawText("${L("hud_score")}: ${selfPlayer.score}", padding, safeTop + lineH, hudPaint)
            canvas.drawText("${L("hud_players")}: ${coord.playerCount}", padding, safeTop + lineH * 2.2f, hudSmallPaint)
            canvas.drawText("${L("hud_room")}: ${coord.roomElapsed}s", padding, safeTop + lineH * 3f, hudSmallPaint)
        } else {
            val lineH = 20f * density
            canvas.drawText("${L("hud_players")}: ${coord.playerCount}", padding, safeTop, hudSmallPaint)
            canvas.drawText("${L("hud_room")}: ${coord.roomElapsed}s", padding, safeTop + lineH, hudSmallPaint)
        }
    }
}
