package com.universalrp.tamilanstore

/** In-memory only. No persisted approvals and no continuation after process death. */
enum class BatchStage { IDLE, DOWNLOADING, READY, INSTALLING, PAUSED, DONE }
data class UpdateBatchState(val ids: List<String> = emptyList(), val index: Int = 0,
    val stage: BatchStage = BatchStage.IDLE) {
    val active: Boolean get() = stage != BatchStage.IDLE && stage != BatchStage.DONE
    val currentId: String? get() = ids.getOrNull(index)
    val remaining: Int get() = (ids.size - index).coerceAtLeast(0)
    fun ready(): UpdateBatchState { check(stage == BatchStage.DOWNLOADING); return copy(stage = BatchStage.READY) }
    fun handedOff(): UpdateBatchState { check(stage == BatchStage.READY); return copy(stage = BatchStage.INSTALLING) }
    private fun next(): UpdateBatchState = copy(index = index + 1,
        stage = if (index + 1 == ids.size) BatchStage.DONE else BatchStage.DOWNLOADING)
    fun alreadyUpdated(): UpdateBatchState { check(stage == BatchStage.DOWNLOADING); return next() }
    fun returned(installed: Boolean): UpdateBatchState {
        check(stage == BatchStage.INSTALLING)
        return if (installed) next() else copy(stage = BatchStage.PAUSED)
    }
    fun pause(): UpdateBatchState = if (active) copy(stage = BatchStage.PAUSED) else this
    fun resume(): UpdateBatchState { check(stage == BatchStage.PAUSED); return copy(stage = BatchStage.DOWNLOADING) }
    companion object {
        fun start(ids: List<String>): UpdateBatchState {
            require(ids.isNotEmpty() && ids.size <= 13 && ids.distinct().size == ids.size)
            require(ids.all { it.matches(Regex("[a-z][a-z0-9]*")) })
            return UpdateBatchState(ids.toList(), stage = BatchStage.DOWNLOADING)
        }
    }
}
