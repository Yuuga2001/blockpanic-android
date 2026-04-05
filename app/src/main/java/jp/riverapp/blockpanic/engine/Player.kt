package jp.riverapp.blockpanic.engine

// Player model -- 1:1 port from shared/game/Player.ts

class ServerPlayer(
    var id: String,
    var name: String,
    var type: PlayerType,
    var x: Double,
    var y: Double
) {
    var vx: Double = 0.0
    var vy: Double = 0.0
    var color: PlayerColor = if (type == PlayerType.CPU) PlayerColor.BLACK else PlayerColor.BLUE
    var alive: Boolean = true
    var onGround: Boolean = false
    var score: Int = 0
    var coinScore: Int = 0
    var input: InputState = InputState()
    var joinedAt: Double = System.currentTimeMillis().toDouble()
    var diedAt: Double = 0.0
    var heightMultiplier: Double = 1.0
    var jumpMultiplier: Double = 1.0
    var activeEffect: MysteryEffect? = null
    var effectEndTime: Double = 0.0
}

fun createPlayer(id: String, name: String, type: PlayerType, spawnX: Double, spawnY: Double): ServerPlayer {
    return ServerPlayer(id = id, name = name, type = type, x = spawnX, y = spawnY)
}

fun toPlayerState(player: ServerPlayer): PlayerState {
    // inputVx: movement intent from input (for walk animation even when vx=0 from collision)
    var inputVx = 0.0
    if (player.input.left) inputVx = -C.moveSpeed
    if (player.input.right) inputVx = C.moveSpeed

    return PlayerState(
        id = player.id, name = player.name,
        x = player.x, y = player.y, vx = player.vx, vy = player.vy,
        type = player.type, color = player.color,
        alive = player.alive, onGround = player.onGround, score = player.score,
        heightMultiplier = player.heightMultiplier, jumpMultiplier = player.jumpMultiplier,
        inputVx = inputVx,
        activeEffect = player.activeEffect,
        effectEndTime = player.effectEndTime
    )
}
