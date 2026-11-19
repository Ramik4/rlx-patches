/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.patches.shared

/**
 * The RLX default theme colors (rlx/defaults.json: theme_color_dark_custom, theme_color_light_custom).
 *
 * With the theme colors left to the app (the default of the patch options), the system draws the
 * splash screen with these before the app starts, so it matches the colors the app then shows.
 */
internal const val RLX_THEME_COLOR_DARK = "#0F0F0F"
internal const val RLX_THEME_COLOR_LIGHT = "#F0F0F0"
