package jp.riverapp.blockpanic.engine

// Physics -- 1:1 port from shared/game/Physics.ts

import kotlin.math.abs

fun applyInput(player: ServerPlayer, input: InputState) {
    val speed = C.moveSpeed * player.speedMultiplier
    player.vx = 0.0
    if (input.left) player.vx = -speed
    if (input.right) player.vx = speed

    if (input.jump && player.onGround) {
        player.vy = C.jumpPower
        player.onGround = false
    }
}

fun updatePlayerPhysics(player: ServerPlayer) {
    val pH = C.playerHeight * player.heightMultiplier

    // Superjump: reduce gravity while ascending
    val gravity = if (player.vy < 0 && player.jumpMultiplier > 1)
        C.gravity / player.jumpMultiplier
    else
        C.gravity
    player.vy += gravity
    if (player.vy > C.maxFallSpeed) player.vy = C.maxFallSpeed

    player.x += player.vx
    player.y += player.vy

    // Horizontal bounds
    if (player.x < 0) { player.x = 0.0; player.vx = 0.0 }
    if (player.x + C.playerWidth > C.gameWidth) {
        player.x = C.gameWidth - C.playerWidth; player.vx = 0.0
    }

    // Ceiling
    if (player.y < 0) {
        player.y = 0.0
        if (player.vy < 0) player.vy = 0.0
    }

    // Floor
    if (player.y + pH >= C.gameHeight) {
        player.y = C.gameHeight - pH
        player.vy = 0.0
        player.onGround = true
    }

    if (player.vy != 0.0) player.onGround = false
}

/**
 * Resolve collisions between player and blocks.
 * Returns true if the player is crushed (couldn't be freed).
 */
fun resolvePlayerBlockCollisions(player: ServerPlayer, blocks: List<BlockState>): Boolean {
    val maxIterations = 3
    val pH = C.playerHeight * player.heightMultiplier

    for (iter in 0 until maxIterations) {
        var hadCollision = false
        var pAABB = playerAABB(x = player.x, y = player.y, width = C.playerWidth, height = pH)

        for (block in blocks) {
            val bAABB = AABB(x = block.x, y = block.y, width = block.width, height = block.height)
            val overlap = getOverlap(pAABB, bAABB) ?: continue
            hadCollision = true

            // Falling block hitting from above: always resolve vertically regardless of
            // overlap ratio (prevents jumping players from escaping crush via horizontal push)
            val fallingBlockFromAbove = block.falling && overlap.pushY == 1.0
            if (overlap.overlapY <= overlap.overlapX || fallingBlockFromAbove) {
                val blockAtFeet = block.y >= player.y + pH / 2
                if (overlap.pushY == -1.0 && (player.vy >= 0 || blockAtFeet)) {
                    // Landing on top of block (falling, or block is at feet level during wall jump)
                    player.y = block.y - pH
                    player.vy = 0.0
                    player.onGround = true
                } else if (overlap.overlapY < 4 && overlap.overlapX > overlap.overlapY * 2) {
                    // Wall-jump graze detection: small vertical overlap during jump
                    // -> resolve horizontally to preserve jump velocity
                    if (overlap.pushX == -1.0) {
                        player.x = block.x - C.playerWidth
                    } else {
                        player.x = block.x + block.width
                    }
                    player.vx = 0.0
                } else {
                    // Hit block from below (head collision)
                    player.y = block.y + block.height
                    if (player.vy < 0) player.vy = 0.0
                }
            } else {
                // Resolve horizontally
                if (overlap.pushX == -1.0) {
                    player.x = block.x - C.playerWidth
                } else {
                    player.x = block.x + block.width
                }
                player.vx = 0.0
            }

            // Re-apply bounds after push
            if (player.x < 0) player.x = 0.0
            if (player.x + C.playerWidth > C.gameWidth) player.x = C.gameWidth - C.playerWidth
            if (player.y + pH > C.gameHeight) { player.y = C.gameHeight - pH; player.onGround = true }

            pAABB = AABB(x = player.x, y = player.y, width = pAABB.width, height = pAABB.height)
        }

        if (!hadCollision) break
    }

    // Crush check: feet grounded + still overlapping with a landed block above = crushed.
    if (player.onGround) {
        val finalAABB = playerAABB(x = player.x, y = player.y, width = C.playerWidth, height = pH)
        for (block in blocks) {
            if (block.falling) continue
            val bAABB = AABB(x = block.x, y = block.y, width = block.width, height = block.height)
            val overlap = getOverlap(finalAABB, bAABB)
            if (overlap != null && overlap.overlapX > 2 && overlap.overlapY > 2) {
                return true
            }
        }
    }

    // Also check: pushed above the screen
    if (player.y < -pH) return true

    return false
}

/** Check if player is crushed (legacy, now integrated into resolvePlayerBlockCollisions) */
fun checkCrushed(player: ServerPlayer, blocks: List<BlockState>): Boolean {
    return false
}

/** Resolve collisions between all pairs of players */
fun resolvePlayerPlayerCollisions(players: List<ServerPlayer>) {
    val alive = players.filter { it.alive }
    for (i in alive.indices) {
        for (j in (i + 1) until alive.size) {
            val a = alive[i]
            val b = alive[j]
            val aH = C.playerHeight * a.heightMultiplier
            val bH = C.playerHeight * b.heightMultiplier
            val aAABB = playerAABB(x = a.x, y = a.y, width = C.playerWidth, height = aH)
            val bAABB = playerAABB(x = b.x, y = b.y, width = C.playerWidth, height = bH)
            val overlap = getOverlap(aAABB, bAABB) ?: continue

            if (overlap.overlapY < overlap.overlapX) {
                if (a.y < b.y) {
                    a.y = b.y - aH; a.vy = 0.0; a.onGround = true
                } else {
                    b.y = a.y - bH; b.vy = 0.0; b.onGround = true
                }
            } else {
                val half = overlap.overlapX / 2
                if (a.x < b.x) { a.x -= half; b.x += half }
                else { a.x += half; b.x -= half }
            }
        }
    }
}
