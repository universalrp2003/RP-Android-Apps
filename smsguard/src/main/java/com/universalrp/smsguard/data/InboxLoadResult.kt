package com.universalrp.smsguard.data

/** Failed provider/enrichment reads are not an authoritative empty inbox. */
sealed class InboxLoadResult<out T> {
    data class Success<T>(val messages: List<T>, val hasMore: Boolean) : InboxLoadResult<T>()
    data object Unavailable : InboxLoadResult<Nothing>()
    data object PermissionRequired : InboxLoadResult<Nothing>()
}

fun <T> InboxLoadResult<T>.messagesOrPrevious(previous: List<T>): List<T> = when (this) {
    is InboxLoadResult.Success -> messages
    InboxLoadResult.Unavailable -> previous
    InboxLoadResult.PermissionRequired -> emptyList() // Do not retain SMS after access is revoked.
}
