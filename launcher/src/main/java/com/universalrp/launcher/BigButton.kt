package com.universalrp.launcher

/**
 * One of the eight big buttons on the home screen.
 *
 * The order and the count are fixed on purpose. This screen is for somebody who does not read small
 * text and does not navigate menus, so there is no list to scroll and nothing to configure: the same
 * eight buttons are in the same places every time, and the four most-used ones are the ones nearest
 * the thumb.
 */
object BigButtons {

    /** What a button does when pressed. */
    enum class Action {
        DIAL,
        MESSAGES,
        CAMERA,
        CONTACTS,
        TORCH,
        VOLUME_UP,
        VOLUME_DOWN,
        SETTINGS,
        EMERGENCY,
        APP_LIST,
    }

    data class Button(
        val action: Action,
        val labelRes: Int,
    )

    /**
     * The home screen, in order.
     *
     * Phone, messages, camera and contacts come first because they are what a phone is for. The
     * torch and the two volume buttons are here because reaching the real volume slider on a modern
     * Android phone is a gesture, and a gesture is exactly what this screen exists to avoid.
     */
    fun home(): List<Button> = listOf(
        Button(Action.DIAL, R.string.big_phone),
        Button(Action.MESSAGES, R.string.big_messages),
        Button(Action.CAMERA, R.string.big_camera),
        Button(Action.CONTACTS, R.string.big_contacts),
        Button(Action.TORCH, R.string.big_torch),
        Button(Action.VOLUME_UP, R.string.volume_up),
        Button(Action.VOLUME_DOWN, R.string.volume_down),
        Button(Action.SETTINGS, R.string.big_settings),
    )

    /** The emergency button sits alone, below the grid, and is a different colour. */
    fun emergency(): Button = Button(Action.EMERGENCY, R.string.big_emergency)

    /** The button that opens the list of every installed app. */
    fun appList(): Button = Button(Action.APP_LIST, R.string.all_apps)

    /** Actions that are satisfied by an intent rather than by acting on the phone directly. */
    fun isLaunchable(action: Action): Boolean = action in LAUNCHABLE

    private val LAUNCHABLE = setOf(
        Action.DIAL, Action.MESSAGES, Action.CAMERA, Action.CONTACTS, Action.SETTINGS,
    )
}
