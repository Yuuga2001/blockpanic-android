package jp.riverapp.blockpanic.engine

// 1:1 port from shared/game/CPUController.ts

import kotlin.random.Random

private val cpuNames = arrayOf(
    "Bot_Alpha", "Bot_Bravo", "Bot_Charlie", "Bot_Delta",
    "Bot_Echo", "Bot_Foxtrot", "Bot_Golf", "Bot_Hotel"
)

class CPUController {
    private var timer: Double = 0.0
    private var nameIndex: Int = 0

    fun getNextName(): String {
        val name = cpuNames[nameIndex % cpuNames.size]
        nameIndex += 1
        return name
    }

    fun update(cpuPlayers: List<ServerPlayer>, dtMs: Double) {
        timer += dtMs
        if (timer < C.cpuUpdateInterval) return
        timer = 0.0

        for (cpu in cpuPlayers) {
            if (!cpu.alive) continue

            // Randomly change direction
            if (Random.nextDouble() < 0.3) {
                val dir = Random.nextDouble()
                if (dir < 0.33) {
                    cpu.input.left = true
                    cpu.input.right = false
                } else if (dir < 0.66) {
                    cpu.input.left = false
                    cpu.input.right = true
                } else {
                    cpu.input.left = false
                    cpu.input.right = false
                }
            }

            // Randomly jump
            if (Random.nextDouble() < 0.1) {
                cpu.input.jump = true
            } else {
                cpu.input.jump = false
            }
        }
    }

    fun resetNameIndex() {
        nameIndex = 0
    }
}
