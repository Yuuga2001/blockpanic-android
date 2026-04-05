package jp.riverapp.blockpanic.engine

// 1:1 port from shared/game/GameState.ts

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class ServerCoin(
    var id: String,
    var x: Double,
    var y: Double,
    var vy: Double,
    var active: Boolean,
    var collected: Boolean,
    var spawnedAt: Double,
    var markedInactiveAt: Double
)

data class ServerMysteryItem(
    var id: String,
    var x: Double,
    var y: Double,
    var vy: Double,
    var active: Boolean,
    var collected: Boolean,
    var effect: MysteryEffect?,
    var spawnedAt: Double,
    var markedInactiveAt: Double
)

data class WorldState(
    val players: List<PlayerState>,
    val blocks: List<BlockState>,
    val coins: List<CoinState>,
    val mysteryItems: List<MysteryItemState>,
    val roomElapsed: Int
)

class GameState {
    val players: LinkedHashMap<String, ServerPlayer> = LinkedHashMap()
    val tetris: TetrisSystem = TetrisSystem()
    val cpuController: CPUController = CPUController()
    val coins: LinkedHashMap<String, ServerCoin> = LinkedHashMap()
    private var coinSpawnTimer: Double = 0.0
    val mysteryItems: LinkedHashMap<String, ServerMysteryItem> = LinkedHashMap()
    private var mysterySpawnTimer: Double = C.mysterySpawnOffset // start at 5s so first spawn is 5s after coins
    val roomStartedAt: Double = System.currentTimeMillis().toDouble()

    init {
        rebalanceCPU()
    }

    fun addPlayer(id: String, name: String): ServerPlayer {
        val spawnX = Random.nextDouble() * (C.gameWidth - C.playerWidth * 2) + C.playerWidth
        val spawnY = 0.0 // Spawn at top -- gravity will pull player down
        val player = createPlayer(id = id, name = name, type = PlayerType.USER, spawnX = spawnX, spawnY = spawnY)
        players[id] = player
        rebalanceCPU()
        return player
    }

    fun removePlayer(id: String) {
        players.remove(id)
        rebalanceCPU()
    }

    fun getPlayer(id: String): ServerPlayer? {
        return players[id]
    }

    fun getUserCount(): Int {
        return players.values.toList().count { it.type == PlayerType.USER }
    }

    fun getCPUCount(): Int {
        return players.values.toList().count { it.type == PlayerType.CPU }
    }

    fun getAlivePlayers(): List<ServerPlayer> {
        return players.values.toList().filter { it.alive }
    }

    fun getCPUPlayers(): List<ServerPlayer> {
        return players.values.toList().filter { it.type == PlayerType.CPU }
    }

    /** Ensure minimum 5 players by adding/removing CPUs */
    fun rebalanceCPU() {
        val userCount = getUserCount()
        val cpuNeeded = max(0, C.minPlayers - userCount)
        val currentCPU = getCPUCount()

        if (currentCPU < cpuNeeded) {
            // Add CPUs
            for (i in 0 until (cpuNeeded - currentCPU)) {
                val now = System.currentTimeMillis().toDouble()
                val rand = Random.nextInt(0xFFFF).toString(36)
                val cpuId = "cpu-${now.toLong()}-$rand"
                val name = cpuController.getNextName()
                val spawnX = Random.nextDouble() * (C.gameWidth - C.playerWidth * 2) + C.playerWidth
                val spawnY = 0.0 // Spawn at top
                val cpu = createPlayer(id = cpuId, name = name, type = PlayerType.CPU, spawnX = spawnX, spawnY = spawnY)
                cpu.color = PlayerColor.BLACK
                players[cpuId] = cpu
            }
        } else if (currentCPU > cpuNeeded) {
            // Remove excess CPUs
            var toRemove = currentCPU - cpuNeeded
            val keys = ArrayList(players.keys)
            for (key in keys) {
                if (toRemove <= 0) break
                val player = players[key]
                if (player != null && player.type == PlayerType.CPU) {
                    players.remove(key)
                    toRemove -= 1
                }
            }
        }
    }

    /** Main physics tick */
    fun tick(dtMs: Double) {
        // Update CPU AI
        cpuController.update(cpuPlayers = getCPUPlayers(), dtMs = dtMs)

        // Update tetris
        tetris.update(dtMs)

        val allBlocks = tetris.getAllBlocks()
        val staticBlocks = allBlocks // All blocks are collision targets

        // Process all alive players
        for (player in players.values.toList()) {
            if (!player.alive) continue

            // Update score (survival time + coin bonus)
            val now = System.currentTimeMillis().toDouble()
            val survivalTime = floor((now - player.joinedAt) / 1000.0).toInt()
            player.score = survivalTime + player.coinScore

            // Apply input (jump is consumed after single use)
            applyInput(player, player.input)
            player.input.jump = false

            // Physics update
            updatePlayerPhysics(player)

            // Player vs Block collision (returns true if crushed)
            val crushed = resolvePlayerBlockCollisions(player, staticBlocks)
            if (crushed) {
                player.alive = false
            }
        }

        // Player vs Player collision
        resolvePlayerPlayerCollisions(getAlivePlayers())

        // === Coin system ===
        updateCoins(dtMs)

        // === Mystery item system ===
        updateMysteryItems(dtMs)

        // === Effect expiry ===
        updateEffects()

        // Respawn dead CPUs after a delay (so crush animation plays on clients)
        val now = System.currentTimeMillis().toDouble()
        for ((_, player) in players.toList()) {
            if (player.type == PlayerType.CPU && !player.alive) {
                if (player.diedAt == 0.0) {
                    player.diedAt = now
                } else if (now - player.diedAt >= 1000) {
                    player.x = Random.nextDouble() * (C.gameWidth - C.playerWidth * 2) + C.playerWidth
                    player.y = 0.0
                    player.vx = 0.0
                    player.vy = 0.0
                    player.alive = true
                    player.diedAt = 0.0
                    player.joinedAt = now
                    player.heightMultiplier = 1.0
                    player.jumpMultiplier = 1.0
                    player.activeEffect = null
                    player.effectEndTime = 0.0
                }
            }
        }
    }

    private fun updateCoins(dtMs: Double) {
        val now = System.currentTimeMillis().toDouble()

        // Spawn timer
        coinSpawnTimer += dtMs
        if (coinSpawnTimer >= C.coinSpawnInterval) {
            coinSpawnTimer -= C.coinSpawnInterval
            spawnCoin(now)
        }

        // Update each coin
        val coinKeys = ArrayList(coins.keys)
        for (id in coinKeys) {
            val coin = coins[id] ?: continue

            if (!coin.active) {
                // Keep inactive coins for 500ms so clients receive at least one broadcast
                if (coin.markedInactiveAt > 0 && now - coin.markedInactiveAt >= 500) {
                    coins.remove(id)
                }
                continue
            }

            // --- Coin physics: gravity + block/floor collision ---
            val allBlocks = tetris.getAllBlocks()

            // Always apply gravity
            coin.vy += 0.5 // gravity acceleration
            if (coin.vy > 10) coin.vy = 10.0 // terminal velocity

            val prevY = coin.y
            coin.y += coin.vy

            // Floor collision
            if (coin.y + C.coinRadius >= C.gameHeight) {
                coin.y = C.gameHeight - C.coinRadius
                coin.vy = 0.0
            }

            // Land on top of LANDED blocks only (not falling ones)
            for (block in allBlocks) {
                if (block.falling) continue // Ignore falling blocks

                // Check if coin overlaps block
                val closestX = max(block.x, min(coin.x, block.x + block.width))
                val closestY = max(block.y, min(coin.y, block.y + block.height))
                val dx = coin.x - closestX
                val dy = coin.y - closestY

                if (dx * dx + dy * dy <= C.coinRadius * C.coinRadius) {
                    // Only resolve as "landing on top" if coin was above before
                    if (prevY + C.coinRadius <= block.y + 4) {
                        coin.y = block.y - C.coinRadius
                        coin.vy = 0.0
                        break
                    }
                }
            }

            // Crushed check: if a landed block overlaps the coin from above, destroy it
            var crushed = false
            for (block in allBlocks) {
                if (block.falling) continue

                val closestX = max(block.x, min(coin.x, block.x + block.width))
                val closestY = max(block.y, min(coin.y, block.y + block.height))
                val dx = coin.x - closestX
                val dy = coin.y - closestY

                if (dx * dx + dy * dy <= C.coinRadius * C.coinRadius) {
                    // Block is above or overlapping the coin center -> crushed
                    if (block.y < coin.y) {
                        crushed = true
                        break
                    }
                }
            }
            if (crushed) {
                coin.active = false
                coin.collected = false
                coin.markedInactiveAt = now
                continue
            }

            // Lifetime expiry
            if (now - coin.spawnedAt >= C.coinLifetime) {
                coin.active = false
                coin.collected = false
                coin.markedInactiveAt = now
                continue
            }

            // Player-coin collision (circle vs AABB, all players including CPU)
            var collected = false
            for (player in players.values.toList()) {
                if (!player.alive) continue

                // Circle-AABB collision (use player's effective height)
                val pH = C.playerHeight * player.heightMultiplier
                val closestX = max(player.x, min(coin.x, player.x + C.playerWidth))
                val closestY = max(player.y, min(coin.y, player.y + pH))
                val dx = coin.x - closestX
                val dy = coin.y - closestY

                if (dx * dx + dy * dy <= C.coinRadius * C.coinRadius) {
                    // Collected!
                    coin.active = false
                    coin.collected = true
                    coin.markedInactiveAt = now
                    player.coinScore += C.coinScore
                    collected = true
                    break // First come, first served
                }
            }

            if (collected) continue
        }
    }

    private fun spawnCoin(now: Double) {
        val rand = Random.nextInt(0xFFFF).toString(36)
        val id = "coin-${now.toLong()}-$rand"
        val x = C.coinRadius + Random.nextDouble() * (C.gameWidth - C.coinRadius * 2)
        val coin = ServerCoin(
            id = id,
            x = x,
            y = -C.coinRadius * 2, // Start above screen
            vy = C.coinFallSpeed,
            active = true,
            collected = false,
            spawnedAt = now,
            markedInactiveAt = 0.0
        )
        coins[id] = coin
    }

    private fun updateMysteryItems(dtMs: Double) {
        val now = System.currentTimeMillis().toDouble()

        // Spawn timer
        mysterySpawnTimer += dtMs
        if (mysterySpawnTimer >= C.mysterySpawnInterval) {
            mysterySpawnTimer -= C.mysterySpawnInterval
            spawnMysteryItem(now)
        }

        // Update each mystery item
        val itemKeys = ArrayList(mysteryItems.keys)
        for (id in itemKeys) {
            val item = mysteryItems[id] ?: continue

            if (!item.active) {
                if (item.markedInactiveAt > 0 && now - item.markedInactiveAt >= 500) {
                    mysteryItems.remove(id)
                }
                continue
            }

            val allBlocks = tetris.getAllBlocks()

            // Gravity
            item.vy += 0.5
            if (item.vy > 10) item.vy = 10.0

            val prevY = item.y
            item.y += item.vy

            // Floor collision
            if (item.y + C.mysteryRadius >= C.gameHeight) {
                item.y = C.gameHeight - C.mysteryRadius
                item.vy = 0.0
            }

            // Land on LANDED blocks
            for (block in allBlocks) {
                if (block.falling) continue
                val closestX = max(block.x, min(item.x, block.x + block.width))
                val closestY = max(block.y, min(item.y, block.y + block.height))
                val dx = item.x - closestX
                val dy = item.y - closestY
                if (dx * dx + dy * dy <= C.mysteryRadius * C.mysteryRadius) {
                    if (prevY + C.mysteryRadius <= block.y + 4) {
                        item.y = block.y - C.mysteryRadius
                        item.vy = 0.0
                        break
                    }
                }
            }

            // Crush check
            var crushed = false
            for (block in allBlocks) {
                if (block.falling) continue
                val closestX = max(block.x, min(item.x, block.x + block.width))
                val closestY = max(block.y, min(item.y, block.y + block.height))
                val dx = item.x - closestX
                val dy = item.y - closestY
                if (dx * dx + dy * dy <= C.mysteryRadius * C.mysteryRadius) {
                    if (block.y < item.y) { crushed = true; break }
                }
            }
            if (crushed) {
                item.active = false
                item.collected = false
                item.markedInactiveAt = now
                continue
            }

            // Lifetime expiry
            if (now - item.spawnedAt >= C.mysteryLifetime) {
                item.active = false
                item.collected = false
                item.markedInactiveAt = now
                continue
            }

            // Player collision
            var collected = false
            for (player in players.values.toList()) {
                if (!player.alive) continue

                val pH = C.playerHeight * player.heightMultiplier
                val closestX = max(player.x, min(item.x, player.x + C.playerWidth))
                val closestY = max(player.y, min(item.y, player.y + pH))
                val dx = item.x - closestX
                val dy = item.y - closestY

                if (dx * dx + dy * dy <= C.mysteryRadius * C.mysteryRadius) {
                    // Collected -- apply random effect
                    val effect = randomEffect()
                    item.active = false
                    item.collected = true
                    item.effect = effect
                    item.markedInactiveAt = now
                    applyEffect(player, effect, now)
                    collected = true
                    break
                }
            }

            if (collected) continue
        }
    }

    private fun randomEffect(): MysteryEffect {
        val effects = MysteryEffect.entries.toTypedArray()
        return effects[Random.nextInt(effects.size)]
    }

    private fun applyEffect(player: ServerPlayer, effect: MysteryEffect, now: Double) {
        // Reset previous effect (adjust position to keep feet anchored)
        setHeightMultiplier(player, 1.0)
        player.jumpMultiplier = 1.0
        player.activeEffect = effect

        when (effect) {
            MysteryEffect.POINTS -> {
                player.coinScore += 200
                // Instant effect but set effectEndTime briefly for popup detection
                player.effectEndTime = now + 100
                player.activeEffect = MysteryEffect.POINTS
            }
            MysteryEffect.SHRINK -> {
                setHeightMultiplier(player, 0.5)
                player.effectEndTime = now + C.mysteryEffectDuration
            }
            MysteryEffect.GROW -> {
                setHeightMultiplier(player, 2.0)
                player.effectEndTime = now + C.mysteryEffectDuration
            }
            MysteryEffect.SUPERJUMP -> {
                player.jumpMultiplier = 2.0
                player.effectEndTime = now + C.mysteryEffectDuration
            }
        }
    }

    /** Change player height multiplier while keeping feet at the same position */
    private fun setHeightMultiplier(player: ServerPlayer, newMul: Double) {
        val oldH = C.playerHeight * player.heightMultiplier
        val newH = C.playerHeight * newMul
        // Adjust y so feet (y + height) stay at the same position
        player.y += (oldH - newH)
        player.heightMultiplier = newMul

        // Clamp: don't let head go above screen
        if (player.y < 0) player.y = 0.0
        // Clamp: don't let feet go below floor
        if (player.y + newH > C.gameHeight) {
            player.y = C.gameHeight - newH
        }
    }

    private fun updateEffects() {
        val now = System.currentTimeMillis().toDouble()
        for (player in players.values.toList()) {
            if (player.effectEndTime > 0 && now >= player.effectEndTime) {
                setHeightMultiplier(player, 1.0)
                player.jumpMultiplier = 1.0
                player.activeEffect = null
                player.effectEndTime = 0.0
            }
        }
    }

    private fun spawnMysteryItem(now: Double) {
        val rand = Random.nextInt(0xFFFF).toString(36)
        val id = "mystery-${now.toLong()}-$rand"
        val x = C.mysteryRadius + Random.nextDouble() * (C.gameWidth - C.mysteryRadius * 2)
        val item = ServerMysteryItem(
            id = id,
            x = x,
            y = -C.mysteryRadius * 2,
            vy = C.mysteryFallSpeed,
            active = true,
            collected = false,
            effect = null,
            spawnedAt = now,
            markedInactiveAt = 0.0
        )
        mysteryItems[id] = item
    }

    fun getWorldState(): WorldState {
        val playerStates = players.values.toList().map { toPlayerState(it) }
        val blocks = tetris.getAllBlocks()
        val coinStates = coins.values.toList().map { c ->
            CoinState(id = c.id, x = c.x, y = c.y, active = c.active, collected = c.collected)
        }
        val mysteryItemStates = mysteryItems.values.toList().map { m ->
            MysteryItemState(id = m.id, x = m.x, y = m.y, active = m.active, collected = m.collected, effect = m.effect)
        }
        val roomElapsed = floor((System.currentTimeMillis().toDouble() - roomStartedAt) / 1000.0).toInt()
        return WorldState(
            players = playerStates,
            blocks = blocks,
            coins = coinStates,
            mysteryItems = mysteryItemStates,
            roomElapsed = roomElapsed
        )
    }
}
