package com.universalrp.smsguard.data

/** Safe phase labels for support; never contain SMS text, addresses, or provider exception messages. */
enum class InboxLoadFailureStage(val code: String) {
    SETTINGS("PREF"),
    PROVIDER_QUERY("QUERY"),
    CONTACT_LOOKUP("CONTACT"),
    MESSAGE_ENRICHMENT("ENRICH"),
    FINANCE_LINKING("FINANCE"),
}

/** Failed provider/enrichment reads are not an authoritative empty inbox. */
sealed class InboxLoadResult<out T> {
    data class Success<T>(val messages: List<T>, val hasMore: Boolean) : InboxLoadResult<T>()
    data class Unavailable(val stage: InboxLoadFailureStage) : InboxLoadResult<Nothing>()
    data object PermissionRequired : InboxLoadResult<Nothing>()
}

fun <T> InboxLoadResult<T>.messagesOrPrevious(previous: List<T>): List<T> = when (this) {
    is InboxLoadResult.Success -> messages
    is InboxLoadResult.Unavailable -> previous
    InboxLoadResult.PermissionRequired -> emptyList() // Do not retain SMS after access is revoked.
}
