package jp.riverapp.blockpanic.engine

// Collision detection -- 1:1 port from shared/game/Collision.ts

import kotlin.math.abs

data class OverlapResult(
    val overlapX: Double,
    val overlapY: Double,
    val pushX: Double, // +1 or -1
    val pushY: Double
)

fun aabbOverlap(a: AABB, b: AABB): Boolean {
    return a.x < b.x + b.width &&
            a.x + a.width > b.x &&
            a.y < b.y + b.height &&
            a.y + a.height > b.y
}

fun getOverlap(a: AABB, b: AABB): OverlapResult? {
    if (!aabbOverlap(a, b)) return null

    val aCx = a.x + a.width / 2
    val bCx = b.x + b.width / 2
    val aCy = a.y + a.height / 2
    val bCy = b.y + b.height / 2

    val overlapX = (a.width + b.width) / 2 - abs(aCx - bCx)
    val overlapY = (a.height + b.height) / 2 - abs(aCy - bCy)
    val pushX: Double = if (aCx < bCx) -1.0 else 1.0
    val pushY: Double = if (aCy < bCy) -1.0 else 1.0

    return OverlapResult(overlapX = overlapX, overlapY = overlapY, pushX = pushX, pushY = pushY)
}

fun playerAABB(x: Double, y: Double, width: Double, height: Double): AABB {
    return AABB(x = x, y = y, width = width, height = height)
}
