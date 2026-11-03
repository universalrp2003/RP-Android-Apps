package com.universalrp.cleansweep.ui

import com.universalrp.updates.SisterApps

/** Live Guard-only owner choice. Shared sibling About lists retain their existing links. */
object LiveGuardAboutPolicy {
    fun projectLinks(links: List<Pair<String, String>>) = links.filterNot { (_, url) ->
        url == SisterApps.LEGACY_MIRROR_URL || url == SisterApps.releaseUrl(SisterApps.byId("dailyvault")!!)
    }
    fun releaseLinks(links: List<Pair<String, String>>) = links.filterNot { (_, url) ->
        url == SisterApps.releaseUrl(SisterApps.byId("liveguard")!!)
    }
}
