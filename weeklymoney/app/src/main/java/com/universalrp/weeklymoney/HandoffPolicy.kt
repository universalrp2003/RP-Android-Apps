package com.universalrp.weeklymoney

/** Conservative single-writer handoff: existing append-only history must be an exact prefix.
 * This is not a merge and cannot prevent an owner from editing an old copy afterwards.
 */
object HandoffPolicy {
    enum class Decision { SAME, FORWARD, CONFLICT }
    private fun <T> prefix(local: List<T>, incoming: List<T>) =
        local.size <= incoming.size && local.indices.all { local[it] == incoming[it] }
    fun compare(local: NotebookState, incoming: NotebookState): Decision {
        if (local == incoming) return Decision.SAME
        if (local.owner != incoming.owner || !prefix(local.books, incoming.books) ||
            !prefix(local.loans, incoming.loans) || !prefix(local.audits, incoming.audits) ||
            !prefix(local.archiveHistory, incoming.archiveHistory) || !prefix(local.loanChanges, incoming.loanChanges) ||
            !prefix(local.collectionDays, incoming.collectionDays) || !prefix(local.continuations, incoming.continuations) ||
            !prefix(local.longUnpaid, incoming.longUnpaid) || !prefix(local.longReceiptAudits, incoming.longReceiptAudits) || !prefix(local.longDebtChanges, incoming.longDebtChanges) ||
            !prefix(local.longTransfers, incoming.longTransfers) || !prefix(local.rowOrders, incoming.rowOrders)) return Decision.CONFLICT
        return Decision.FORWARD
    }
}
