package com.universalrp.animalvoice

/** Keep both messages so changing language also updates an already displayed status. */
data class BilingualText(val tamil: String, val english: String) {
    fun display(useTamil: Boolean) = if (useTamil) tamil else english
    companion object { val EMPTY = BilingualText("", "") }
}
