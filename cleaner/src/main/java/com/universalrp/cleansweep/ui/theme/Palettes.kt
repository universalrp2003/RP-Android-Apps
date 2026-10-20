package com.universalrp.cleansweep.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color

/**
 * 2.51 — a colour theme is a palette, not a repaint.
 *
 * Why this file exists: every screen used to import eleven fixed colours from Theme.kt, so the
 * whole app was one blue. The owner asked for themes ("same color are boring"). The colours are
 * still exposed under the same names — [Bg], [AccentCyan] and the rest — but each one is now a
 * getter that reads [Palette.current], which means **no screen had to change** and a new theme is
 * a new entry in [Palettes.all], nothing else.
 *
 * Rules kept from earlier builds:
 *  * the app stays dark-first — every palette is dark, because the owner reads it at night and
 *    the LED/health cards were designed against dark surfaces;
 *  * semantic colours stay semantic — [good], [warn] and [danger] must still read as green,
 *    amber and red in every theme, so a warning never loses its meaning to decoration;
 *  * contrast is chosen, not generated: each palette states its own text colours.
 */
data class AppPalette(
    val id: String,
    val name: String,
    val tamilName: String,
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val accent: Color,
    val accentAlt: Color,
    val good: Color,
    val warn: Color,
    val danger: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val onAccent: Color,
)

object Palettes {

    /** Midnight — the original Live Guard blue. Default, and what every existing user has. */
    val MIDNIGHT = AppPalette(
        id = "midnight",
        name = "Midnight",
        tamilName = "நள்ளிரவு",
        bg = Color(0xFF070D1A),
        surface = Color(0xFF0E1627),
        surfaceHigh = Color(0xFF16213A),
        outline = Color(0xFF24304E),
        accent = Color(0xFF22D3EE),
        accentAlt = Color(0xFF8B5CF6),
        good = Color(0xFF34D399),
        warn = Color(0xFFFBBF24),
        danger = Color(0xFFF87171),
        textPrimary = Color(0xFFE5EAF5),
        textSecondary = Color(0xFF93A0BC),
        onAccent = Color(0xFF03202B),
    )

    /** Saffron — warm, for readers who find blue cold. Still dark, still readable. */
    val SAFFRON = AppPalette(
        id = "saffron",
        name = "Saffron",
        tamilName = "குங்குமப்பூ",
        bg = Color(0xFF120C06),
        surface = Color(0xFF1D1409),
        surfaceHigh = Color(0xFF2A1E0F),
        outline = Color(0xFF46331B),
        accent = Color(0xFFFB923C),
        accentAlt = Color(0xFFF43F5E),
        good = Color(0xFF4ADE80),
        warn = Color(0xFFFACC15),
        danger = Color(0xFFFB7185),
        textPrimary = Color(0xFFF7EEE3),
        textSecondary = Color(0xFFC4A98C),
        onAccent = Color(0xFF2A1204),
    )

    /** Evergreen — calm green, low glare. */
    val EVERGREEN = AppPalette(
        id = "evergreen",
        name = "Evergreen",
        tamilName = "பசுமை",
        bg = Color(0xFF05120E),
        surface = Color(0xFF0A1E19),
        surfaceHigh = Color(0xFF112B24),
        outline = Color(0xFF1E4438),
        accent = Color(0xFF34D399),
        accentAlt = Color(0xFF22D3EE),
        good = Color(0xFF86EFAC),
        warn = Color(0xFFFBBF24),
        danger = Color(0xFFF87171),
        textPrimary = Color(0xFFE3F5EC),
        textSecondary = Color(0xFF8EB3A3),
        onAccent = Color(0xFF03291C),
    )

    /** Violet — deeper purple, softer than the blue. */
    val VIOLET = AppPalette(
        id = "violet",
        name = "Violet",
        tamilName = "ஊதா",
        bg = Color(0xFF0B0716),
        surface = Color(0xFF15102A),
        surfaceHigh = Color(0xFF221A3D),
        outline = Color(0xFF372B5C),
        accent = Color(0xFFA78BFA),
        accentAlt = Color(0xFF22D3EE),
        good = Color(0xFF4ADE80),
        warn = Color(0xFFFBBF24),
        danger = Color(0xFFFB7185),
        textPrimary = Color(0xFFEDE8FA),
        textSecondary = Color(0xFFA99CC7),
        onAccent = Color(0xFF1B0F3B),
    )

    /** Slate — almost colourless, for a quiet interface. */
    val SLATE = AppPalette(
        id = "slate",
        name = "Slate",
        tamilName = "சாம்பல்",
        bg = Color(0xFF0C0E12),
        surface = Color(0xFF15181E),
        surfaceHigh = Color(0xFF1F242C),
        outline = Color(0xFF333B46),
        accent = Color(0xFFCBD5E1),
        accentAlt = Color(0xFF60A5FA),
        good = Color(0xFF4ADE80),
        warn = Color(0xFFFBBF24),
        danger = Color(0xFFF87171),
        textPrimary = Color(0xFFF1F5F9),
        textSecondary = Color(0xFF94A3B8),
        onAccent = Color(0xFF10151B),
    )

    /** Black — true black panels, for OLED screens and long battery life. */
    val BLACK = AppPalette(
        id = "black",
        name = "Black (OLED)",
        tamilName = "கருப்பு",
        bg = Color(0xFF000000),
        surface = Color(0xFF0A0A0A),
        surfaceHigh = Color(0xFF141414),
        outline = Color(0xFF262626),
        accent = Color(0xFF38BDF8),
        accentAlt = Color(0xFF818CF8),
        good = Color(0xFF34D399),
        warn = Color(0xFFFBBF24),
        danger = Color(0xFFF87171),
        textPrimary = Color(0xFFF5F5F5),
        textSecondary = Color(0xFFA3A3A3),
        onAccent = Color(0xFF00131F),
    )

    /** Rose — the sixth choice, warm pink without losing the warning colours. */
    val ROSE = AppPalette(
        id = "rose",
        name = "Rose",
        tamilName = "ரோஜா",
        bg = Color(0xFF120810),
        surface = Color(0xFF1E0E1A),
        surfaceHigh = Color(0xFF2C1526),
        outline = Color(0xFF4A2439),
        accent = Color(0xFFFB7185),
        accentAlt = Color(0xFFC084FC),
        good = Color(0xFF4ADE80),
        warn = Color(0xFFFBBF24),
        danger = Color(0xFFEF4444),
        textPrimary = Color(0xFFF9EBF1),
        textSecondary = Color(0xFFC39AAE),
        onAccent = Color(0xFF3B0512),
    )

    val all: List<AppPalette> = listOf(MIDNIGHT, SAFFRON, EVERGREEN, VIOLET, SLATE, BLACK, ROSE)

    fun byId(id: String?): AppPalette = all.firstOrNull { it.id == id } ?: MIDNIGHT
}

/**
 * The process-wide current palette.
 *
 * Backed by a Compose snapshot state: the theme composable, every screen colour and the Material
 * scheme all read through [current], so one `apply` repaints all of them. Reading the state
 * outside composition (the widget, notification builders) is allowed and simply gets the value.
 *
 * AppRoot also keys its screen tree on `UiState.themeId`. That is deliberate belt and braces:
 * the key rebuilds screens that read no state at all, which is the same fix the language switch
 * needed, and it costs one flicker-free rebuild per theme change.
 */
object Palette {
    private val state = androidx.compose.runtime.mutableStateOf(Palettes.MIDNIGHT)

    val current: AppPalette get() = state.value

    fun apply(id: String?) { state.value = Palettes.byId(id) }
}

/** Storage for the chosen theme. One key, read before the first composition. */
object ThemeStore {
    private const val PREFS = "liveguard_theme"
    private const val KEY = "theme_id"

    fun currentId(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, Palettes.MIDNIGHT.id)
            ?: Palettes.MIDNIGHT.id

    fun save(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, id).apply()
    }
}
