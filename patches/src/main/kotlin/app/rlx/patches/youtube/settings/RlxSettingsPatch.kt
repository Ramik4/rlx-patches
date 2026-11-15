/*
 * RLX Patches
 * https://github.com/RAMiK4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.patches.youtube.settings

import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.misc.settings.PreferenceScreen
import app.morphe.patches.youtube.shared.YouTubeActivityOnCreateFingerprint
import app.rlx.patches.shared.rlxCorePatch

/**
 * The "RLX Extended" screen of the YouTube settings. RLX patches add their settings here.
 */
val RLX_EXTENDED_SCREEN = PreferenceScreen.Screen(
    key = "rlx_extended_screen",
    titleKey = "rlx_extended_screen_title",
    summaryKey = null,
    icon = "@drawable/rlx_settings_screen_extended",
    iconBold = "@drawable/rlx_settings_screen_extended_bold",
    layout = "@layout/preference_with_icon",
    sorting = Sorting.UNSORTED,
)

/**
 * Applied by the Morphe settings patch of YouTube, see rlx/README.md.
 */
val rlxSettingsPatch = rlxCorePatch(
    appId = "youtube",
    sharedExtensionPatch = sharedExtensionPatch,
    mainActivityOnCreateFingerprint = YouTubeActivityOnCreateFingerprint,
    extendedScreen = RLX_EXTENDED_SCREEN,
)
