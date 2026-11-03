package com.oorarivippu.voiceannouncer.family

/** Persistent local consent, short renewable server leases, no indefinite remote grant. */
object FamilySharingPolicy {
    const val LEASE = 7 * 60 * 60_000L // Below the existing server's 8-hour ceiling, including clock tolerance.
    const val RENEW_BEFORE = 5 * 60_000L
    fun renew(enabled: Boolean, approved: Boolean, tracking: Boolean, pending: Boolean, until: Long, now: Long) =
        enabled && approved && tracking && !pending && until-now < RENEW_BEFORE
    fun acceptLease(enabled: Boolean, revision: Long, current: Long, pending: Boolean, tracking: Boolean) =
        enabled && FamilyProtocol.canAcceptStart(revision,current,pending,tracking)
    fun send(enabled: Boolean,revision: Long,snapshotRevision: Long,pending: Boolean,tracking: Boolean,until: Long,now: Long) =
        enabled && revision==snapshotRevision && !pending && tracking && until>now
    fun fresh(at: Long,now: Long) = at>0 && now-at in 0 until FamilyProtocol.TTL
    fun announce(approved: Boolean,at: Long,previous: Long,now: Long) = approved && fresh(at,now) && at>previous
    fun audioAllowed(normalRinger: Boolean,dndClear: Boolean,call: Boolean,mediaVolume: Int,speechVolume: Int) =
        normalRinger && dndClear && !call && mediaVolume>0 && speechVolume>0
    fun sentence(name: String,town: String,tamil: Boolean) = if(tamil)
        "$name. கடைசியாகத் தெரிவித்த ஊர் $town." else "$name. Last reported town: $town."
}
