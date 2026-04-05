package jp.riverapp.blockpanic.rendering

import android.graphics.Canvas
import jp.riverapp.blockpanic.engine.C

/**
 * Simple viewport: translate and scale canvas so game world (900x600) fits screen height.
 * Port from iOS CameraController.swift
 */
class CameraController {
    var viewWidth: Float = 0f
    var viewHeight: Float = 0f

    /** Scale factor to fit game height to screen height */
    val scale: Float
        get() = if (viewHeight > 0) viewHeight / C.gameHeight.toFloat() else 1f

    /** Horizontal offset to center the game world */
    val offsetX: Float
        get() = (viewWidth - C.gameWidth.toFloat() * scale) / 2f

    /** Vertical offset (0 when fitting to height) */
    val offsetY: Float
        get() = 0f

    /** Visible width in game coordinates */
    val visibleWidth: Float
        get() = if (scale > 0) viewWidth / scale else C.gameWidth.toFloat()

    /** Visible height in game coordinates */
    val visibleHeight: Float
        get() = C.gameHeight.toFloat()

    /** Apply camera transform to canvas */
    fun applyTransform(canvas: Canvas) {
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
    }

    /** Convert screen coordinates to game coordinates */
    fun screenToGame(screenX: Float, screenY: Float): Pair<Float, Float> {
        val gx = (screenX - offsetX) / scale
        val gy = (screenY - offsetY) / scale
        return gx to gy
    }
}
