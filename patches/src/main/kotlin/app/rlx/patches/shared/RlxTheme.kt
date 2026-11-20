/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.patches.shared

import app.morphe.patcher.patch.stringOption

/**
 * The RLX default theme colors (rlx/defaults.json: theme_color_dark_custom, theme_color_light_custom).
 *
 * With the theme colors left to the app (the default of the patch options), the system draws the
 * splash screen with these before the app starts, so it matches the colors the app then shows.
 * Not used when the app starts with pure black ([rlxInAppDefaultColorsOption]).
 */
internal const val RLX_THEME_COLOR_DARK = "#0F0F0F"
internal const val RLX_THEME_COLOR_LIGHT = "#F0F0F0"

/** [rlxInAppDefaultColorsOption]: the RLX colors, [RLX_THEME_COLOR_DARK] and [RLX_THEME_COLOR_LIGHT]. */
internal const val RLX_IN_APP_DEFAULT_COLORS_RLX = "rlx"

/** [rlxInAppDefaultColorsOption]: pure black and white, the Morphe defaults of the theme settings. */
internal const val RLX_IN_APP_DEFAULT_COLORS_PURE_BLACK = "pure_black"

/**
 * Option of the Theme patch: the theme colors the app starts with while the colors are left to the app
 * (the default of the color options). They can still be changed in the app settings.
 *
 * With pure black, the RLX default settings leave the theme settings at their Morphe defaults (pure black,
 * white, foreground unchanged) and the splash screen is black or white.
 */
internal val rlxInAppDefaultColorsOption = stringOption(
    key = "inAppDefaultColors",
    default = RLX_IN_APP_DEFAULT_COLORS_RLX,
    values = mapOf(
        "RLX (#0F0F0F, #F0F0F0)" to RLX_IN_APP_DEFAULT_COLORS_RLX,
        "Pure black (#000000, #FFFFFF)" to RLX_IN_APP_DEFAULT_COLORS_PURE_BLACK,
    ),
    title = "Default colors in the app",
    description = "The theme colors the app starts with when the colors are changed in the app. " +
            "They can be changed in the app settings.",
    required = true,
) {
    it == RLX_IN_APP_DEFAULT_COLORS_RLX || it == RLX_IN_APP_DEFAULT_COLORS_PURE_BLACK
}

/** If the app starts with pure black and white ([rlxInAppDefaultColorsOption]). */
internal val rlxPureBlackByDefault: Boolean
    get() = rlxInAppDefaultColorsOption.value == RLX_IN_APP_DEFAULT_COLORS_PURE_BLACK
