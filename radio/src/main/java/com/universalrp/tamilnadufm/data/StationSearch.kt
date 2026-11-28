package com.universalrp.tamilnadufm.data

/** Local discovery only. A reference label is a possible match, not proof of identity. */
object StationSearch {
    fun matches(station: RadioStation, terms: List<String>): Boolean =
        terms.isEmpty() || terms.any { query ->
            listOf(station.name, station.referenceName, station.tags, station.country, station.language)
                .any { it.contains(query, ignoreCase = true) }
        }
}
