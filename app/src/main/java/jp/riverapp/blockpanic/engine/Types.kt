package jp.riverapp.blockpanic.engine

// Block Panic Types -- 1:1 port from shared/types.ts

import com.google.gson.annotations.SerializedName

enum class MysteryEffect(val value: String) {
    @SerializedName("points") POINTS("points"),
    @SerializedName("shrink") SHRINK("shrink"),
    @SerializedName("grow") GROW("grow"),
    @SerializedName("superjump") SUPERJUMP("superjump");

    companion object {
        fun fromValue(value: String): MysteryEffect? = entries.find { it.value == value }
    }
}

enum class PlayerType(val value: String) {
    @SerializedName("user") USER("user"),
    @SerializedName("cpu") CPU("cpu");
}

enum class PlayerColor(val value: String) {
    @SerializedName("red") RED("red"),
    @SerializedName("blue") BLUE("blue"),
    @SerializedName("black") BLACK("black");
}

data class InputState(
    var left: Boolean = false,
    var right: Boolean = false,
    var jump: Boolean = false
)

data class AABB(
    var x: Double,
    var y: Double,
    var width: Double,
    var height: Double
)

// MARK: - Network state (Gson serialization)

data class PlayerState(
    val id: String,
    val name: String,
    val x: Double,
    val y: Double,
    val vx: Double,
    val vy: Double,
    val type: PlayerType,
    val color: PlayerColor,
    val alive: Boolean,
    val onGround: Boolean,
    val score: Int,
    val heightMultiplier: Double,
    val jumpMultiplier: Double,
    @JvmField val inputVx: Double = 0.0,
    val activeEffect: MysteryEffect? = null,
    @JvmField val effectEndTime: Double = 0.0
)

data class BlockState(
    val id: String,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val falling: Boolean,
    val pieceType: Int
)

data class CoinState(
    val id: String,
    val x: Double,
    val y: Double,
    val active: Boolean,
    val collected: Boolean
)

data class MysteryItemState(
    val id: String,
    val x: Double,
    val y: Double,
    val active: Boolean,
    val collected: Boolean,
    val effect: MysteryEffect? = null
)

// MARK: - Network messages

data class StateMessage(
    val type: String = "state",
    val playerId: String,
    val players: List<PlayerState>,
    val blocks: List<BlockState>,
    val coins: List<CoinState>,
    val mysteryItems: List<MysteryItemState>,
    val roomElapsed: Int,
    val timestamp: Double
)

data class GameOverMessage(
    val type: String = "gameover",
    val score: Int
)

data class JoinedMessage(
    val type: String = "joined",
    val playerId: String
)
