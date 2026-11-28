package com.universalrp.tamilnadufm.ui

/** Keep names readable on narrow phones and when the user enlarges system text. */
object StationGridLayout {
    fun columns(widthDp: Int, fontScale: Float): Int = when {
        widthDp < 260 || fontScale >= 1.7f -> 1
        widthDp < 360 || fontScale >= 1.25f -> 2
        else -> 3
    }
}
