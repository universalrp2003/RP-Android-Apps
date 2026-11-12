package com.universalrp.updates

/** Finite local queue policy; -1 means stop. Random mode avoids immediate repeats. */
object PlaybackOrder {
    const val OFF = 0
    const val ONE = 1
    const val ALL = 2
    fun next(size: Int, current: Int, repeat: Int = OFF, random: Boolean = false, played: Set<Int> = emptySet(),
             draw: (Int) -> Int = { kotlin.random.Random.nextInt(it) }): Int {
        if (size <= 0 || current !in 0 until size) return -1
        if (repeat == ONE) return current
        if (size == 1) return if (repeat == ALL) 0 else -1
        if (random) {
            val remaining = (0 until size).filter { it != current && it !in played }
            val choices = if (remaining.isNotEmpty()) remaining else if (repeat == ALL) (0 until size).filter { it != current } else return -1
            return choices[draw(choices.size).coerceIn(0, choices.lastIndex)]
        }
        return if (current + 1 < size) current + 1 else if (repeat == ALL) 0 else -1
    }
}
