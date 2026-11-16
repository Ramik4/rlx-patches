/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.patches.music.settings

import app.morphe.patches.music.misc.extension.sharedExtensionPatch
import app.morphe.patches.music.misc.settings.PreferenceScreen
import app.morphe.patches.music.shared.MusicActivityOnCreateFingerprint
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.rlx.patches.shared.moveToRlxCustomizations
import app.rlx.patches.shared.rlxCorePatch

/**
 * The "RLX Customizations" screen of the YouTube Music settings. RLX patches add their settings here, and the Morphe
 * branding, theme and settings options are moved here (RlxCustomizations.kt).
 */
val RLX_CUSTOMIZATIONS_SCREEN = PreferenceScreen.Screen(
    // The settings root is sorted by key: right below "About", above the Morphe screens.
    key = "morphe_settings_music_screen_00_rlx_customizations",
    titleKey = "rlx_customizations_screen_title",
    summaryKey = null,
    icon = "@drawable/rlx_settings_screen_customizations",
    iconBold = "@drawable/rlx_settings_screen_customizations_bold",
    layout = "@layout/morphe_preference_with_icon",
    sorting = Sorting.UNSORTED,
)

/**
 * Applied by the Morphe settings patch of YouTube Music, see rlx/README.md.
 */
val rlxSettingsPatch = rlxCorePatch(
    appId = "music",
    sharedExtensionPatch = sharedExtensionPatch,
    mainActivityOnCreateFingerprint = MusicActivityOnCreateFingerprint,
    customizationsScreen = RLX_CUSTOMIZATIONS_SCREEN,
)

/**
 * Moves the Morphe settings of RLX Customizations there. Called by the Morphe settings patch of YouTube Music right
 * before the settings are written (RLX hook), see rlx/README.md.
 */
fun moveRlxCustomizations() = moveToRlxCustomizations(
    sources = listOf(PreferenceScreen.GENERAL, PreferenceScreen.MISC),
    target = RLX_CUSTOMIZATIONS_SCREEN,
)
