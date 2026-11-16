/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.patches.shared

/**
 * The values the patch option "App icon" of "Custom branding" offers between "Original" and
 * "Custom": the RLX icons, as label to icon style. The Morphe icons are not offered
 * (GPLv3 section 7e, see NOTICE); their launch aliases stay, so an icon selected with an earlier
 * YT Morphe build keeps working. The app settings hide them too (RlxBranding).
 *
 * @param names The icon styles of Morphe; an RLX icon whose style Morphe removed is left out.
 */
internal fun rlxPatchOptionIconStyles(names: Array<String>): Map<String, String> =
    rlxIconStyles.filterValues { it in names }

/** The icon style of the app when the patch option "App icon" is not set: RLX red. */
internal const val RLX_DEFAULT_ICON_STYLE = "play"

/**
 * The app name of the app when no custom name is given: name 2 of Custom branding, which RLX names
 * "YouTube RLX" and "YT Music RLX" (addresources values/rlx-youtube and values/rlx-music).
 * Must match RlxBranding.DEFAULT_APP_NAME_INDEX of the extension.
 */
internal const val RLX_DEFAULT_APP_NAME_INDEX = 2
