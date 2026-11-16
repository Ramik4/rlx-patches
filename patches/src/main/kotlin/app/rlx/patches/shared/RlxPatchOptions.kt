/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.patches.shared

/**
 * Morphe app icon styles that are not offered in the patch options of "Custom branding"
 * (GPLv3 section 7e, see NOTICE). Their resources stay in the app, so an icon selected with an
 * earlier YT Morphe build keeps working; the app settings hide them too (RlxBranding).
 */
private val hiddenMorpheIconStyles = setOf("black", "dark", "light", "play", "play_black")

/** The icon styles of [names] that the patch option "App icon" offers. */
internal fun rlxPatchOptionIconStyles(names: Array<String>): List<String> =
    names.filterNot(hiddenMorpheIconStyles::contains)
