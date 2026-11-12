package com.universalrp.smsguard.engine

/** SMS-only default mode is an informed choice, not a claim of MMS support or Google approval. */
object DefaultSmsSetupPolicy {
    enum class Step { ALREADY_DEFAULT, DISCLOSE_MMS_LIMIT, REQUEST_ROLE, LEGACY_REQUEST, SYSTEM_SETTINGS }

    fun next(isDefault: Boolean, acknowledged: Boolean, sdk: Int, roleAvailable: Boolean): Step = when {
        isDefault -> Step.ALREADY_DEFAULT
        !acknowledged -> Step.DISCLOSE_MMS_LIMIT
        sdk < 29 -> Step.LEGACY_REQUEST
        roleAvailable -> Step.REQUEST_ROLE
        else -> Step.SYSTEM_SETTINGS
    }
}
