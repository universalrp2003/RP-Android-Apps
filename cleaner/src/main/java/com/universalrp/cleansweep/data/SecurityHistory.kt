package com.universalrp.cleansweep.data

import java.security.MessageDigest

data class ObservedPackage(
    val packageName: String, val label: String, val installedAtMs: Long = 0,
    val updatedAtMs: Long = 0, val isSystem: Boolean = false,
    val identity: PackageIdentity? = null, val installer: String? = null,
)

data class SecurityObservation(
    val key: String, val findingId: String, val title: String, val detail: String,
    val severity: Severity, val app: FindingApp?, val fingerprint: String,
)

data class SecuritySnapshot(
    val atMs: Long,
    val observations: List<SecurityObservation>,
    val packages: List<ObservedPackage>,
    val unavailableChecks: Set<String> = emptySet(),
)

enum class SecurityChangeKind { BASELINE, NEW_APP, APP_NO_LONGER_LISTED, NEW_FINDING, CHANGED, RESOLVED, REVIEWED, REVIEW_CLEARED }

data class SecurityChange(
    val id: String, val atMs: Long, val kind: SecurityChangeKind, val key: String,
    val title: String, val appLabel: String? = null, val packageName: String? = null,
    val detail: String = "",
)

data class SecurityHistoryState(
    val latest: SecuritySnapshot? = null,
    val events: List<SecurityChange> = emptyList(),
    /** Acknowledgement is tied to evidence AND app update time, not just a display name. */
    val reviewed: Map<String, String> = emptyMap(),
    val baselinedChecks: Set<String> = emptySet(),
)

object SecurityHistoryPolicy {
    const val MAX_EVENTS = 200
    private val APP_CHECKS = setOf(
        "install_other_apps", "usage_access", "overlay", "sideloaded", "sms_readers",
        "banking_sms_readers", "facebook_stubs", "odd_installer", "root_tools", "installed_apps",
    )

    private val ALL_CHECKS = APP_CHECKS + SecurityIdentityPolicy.combinations.keys + setOf("accessibility", "notification_listeners", "device_admin", "no_lock", "developer_options", "old_patch")

    fun snapshot(report: SecurityReport, packages: List<ObservedPackage>): SecuritySnapshot {
        val byPackage = packages.associateBy { it.packageName }
        val observations = report.findings.filterNot { it.id == "no_hash_lookup" }.flatMap { finding ->
            val targets: List<FindingApp?> = if (finding.affectedApps.isEmpty()) listOf(null) else finding.affectedApps
            targets.map { app ->
                val key = key(finding.id, app)
                val version = app?.let { byPackage[it.packageName]?.updatedAtMs } ?: 0
                SecurityObservation(
                    key, finding.id, finding.title, finding.detail, finding.severity, app,
                    evidenceFingerprint("${finding.id}|${finding.severity}|${finding.detail}|${app?.packageName}|${app?.componentName}|$version"),
                )
            }
        }.distinctBy { it.key }.sortedBy { it.key }
        return SecuritySnapshot(report.scannedAtMs, observations, packages, report.unavailableChecks)
    }

    fun key(findingId: String, app: FindingApp?): String =
        "$findingId|${app?.packageName.orEmpty()}|${app?.componentName.orEmpty()}"

    fun evidenceFingerprint(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }

    fun observe(state: SecurityHistoryState, current: SecuritySnapshot): SecurityHistoryState {
        val previous = state.latest
        // Do not allow a late/stale asynchronous result to resolve newer evidence.
        if (previous != null && current.atMs <= previous.atMs) return state
        val validCurrent = current.copy(observations = current.observations.filterNot { it.findingId in current.unavailableChecks })
        val events = state.events.toMutableList()
        val identityChanged = mutableSetOf<String>()
        fun add(kind: SecurityChangeKind, observation: SecurityObservation? = null, pkg: ObservedPackage? = null,
                title: String = observation?.title ?: pkg?.label.orEmpty(), detail: String = "") {
            val key = observation?.key ?: pkg?.packageName.orEmpty()
            events += SecurityChange(
                "${current.atMs}-${events.size}-${kind.name}-$key", current.atMs, kind, key, title,
                observation?.app?.label ?: pkg?.label, observation?.app?.packageName ?: pkg?.packageName, detail,
            )
        }
        if (previous == null) {
            add(SecurityChangeKind.BASELINE, title = "Initial security baseline", detail = "Existing observations are not treated as newly granted access.")
        } else {
            val old = previous.observations.associateBy { it.key }
            val fresh = validCurrent.observations.associateBy { it.key }
            for (item in validCurrent.observations) {
                val before = old[item.key]
                when {
                    before == null && item.findingId in state.baselinedChecks ->
                        add(SecurityChangeKind.NEW_FINDING, item, detail = "First observed since the previous comparable check; not the exact grant time or proof of malware.")
                    before != null && item.fingerprint != before.fingerprint ->
                        add(SecurityChangeKind.CHANGED, item, detail = "Observed evidence or app update changed; review the current state again.")
                }
            }
            for (item in previous.observations) {
                if (item.key !in fresh && item.findingId !in current.unavailableChecks) {
                    add(SecurityChangeKind.RESOLVED, item, detail = "No longer observed by this check. This is not a malware-free verdict.")
                }
            }
            if ("installed_apps" in state.baselinedChecks && "installed_apps" !in current.unavailableChecks) {
                val oldPackages = previous.packages.filterNot { it.isSystem }.associateBy { it.packageName }
                val newPackages = current.packages.filterNot { it.isSystem }.associateBy { it.packageName }
                for ((key, pkg) in newPackages) if (key !in oldPackages) {
                    add(SecurityChangeKind.NEW_APP, pkg = pkg, title = "App newly observed", detail = "First seen in this inventory; not necessarily newly installed or unsafe.")
                }
                val oldIdentityPackages = previous.packages.associateBy { it.packageName }
                for ((key, pkg) in current.packages.associateBy { it.packageName }) {
                    val oldPackage = oldIdentityPackages[key] ?: continue
                    when (SecurityIdentityPolicy.compare(oldPackage.identity, pkg.identity)) {
                        SecurityIdentityPolicy.Change.PLATFORM_ROTATION -> { identityChanged += key; add(SecurityChangeKind.CHANGED, pkg = pkg,
                            title = "Signing identity rotated", detail = "Android reports a signing lineage including the previous signer. Rotation is not proof of malware or safety.") }
                        SecurityIdentityPolicy.Change.DIFFERENT -> { identityChanged += key; add(SecurityChangeKind.CHANGED, pkg = pkg,
                            title = "Signing identity changed", detail = "No continuity with the previous signer was observed. Review the package and source; this alone does not prove malware.") }
                        else -> Unit
                    }
                    if (oldPackage.installer != null && pkg.installer != null && oldPackage.installer != pkg.installer)
                        add(SecurityChangeKind.CHANGED, pkg = pkg, title = "Reported installer changed",
                            detail = "${oldPackage.installer} → ${pkg.installer}. Installer attribution is not a verified trust verdict.")
                }
                for ((key, pkg) in oldPackages) if (key !in newPackages) {
                    add(SecurityChangeKind.APP_NO_LONGER_LISTED, pkg = pkg, title = "App no longer listed", detail = "May have been removed or become unavailable to this inventory.")
                }
            }
        }
        // Preserve unavailable categories rather than turning a denied read into a resolution.
        val carried = previous?.observations.orEmpty().filter { old ->
            old.findingId in current.unavailableChecks
        }
        val packages = if ("installed_apps" in current.unavailableChecks) previous?.packages ?: current.packages else current.packages.map { pkg ->
            val old = previous?.packages?.firstOrNull { it.packageName == pkg.packageName }
            if (pkg.identity?.valid() != true) pkg.copy(identity = old?.identity) else pkg
        }
        val merged = current.copy(observations = (validCurrent.observations + carried).distinctBy { it.key }, packages = packages)
        val fingerprints = merged.observations.associate { it.key to it.fingerprint }
        val reviewed = state.reviewed.filter { (key, value) ->
            val pkg = merged.observations.firstOrNull { it.key == key }?.app?.packageName
            (key.startsWith("local:") || fingerprints[key] == value) && pkg !in identityChanged
        }
        return SecurityHistoryState(merged, events.takeLast(MAX_EVENTS), reviewed, state.baselinedChecks + (ALL_CHECKS - current.unavailableChecks))
    }

    fun review(state: SecurityHistoryState, observation: SecurityObservation, expected: Boolean, atMs: Long): SecurityHistoryState {
        val reviewed = state.reviewed.toMutableMap()
        if (expected) reviewed[observation.key] = observation.fingerprint else reviewed.remove(observation.key)
        val event = SecurityChange(
            "$atMs-review-${state.events.size}-${observation.key}", atMs,
            if (expected) SecurityChangeKind.REVIEWED else SecurityChangeKind.REVIEW_CLEARED,
            observation.key, observation.title, observation.app?.label, observation.app?.packageName,
            if (expected) "User marked this exact observation reviewed/expected. Access was not revoked; safety is not certified."
            else "User removed the reviewed/expected acknowledgement.",
        )
        return state.copy(reviewed = reviewed, events = (state.events + event).takeLast(MAX_EVENTS))
    }

    fun isReviewed(state: SecurityHistoryState, item: SecurityObservation): Boolean = state.reviewed[item.key] == item.fingerprint

    fun appChecks(): Set<String> = APP_CHECKS
}
