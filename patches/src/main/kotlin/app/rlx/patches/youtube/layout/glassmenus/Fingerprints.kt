/*
 * Copyright 2026 RLX Patches.
 * https://github.com/Ramik4/rlx-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.rlx.patches.youtube.layout.glassmenus

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral

/**
 * The bottom sheet dialog of the Material components, which every bottom sheet of the app uses.
 */
internal object BottomSheetDialogClassFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "design_bottom_sheet_dialog"),
    )
)

internal object BottomSheetDialogOnAttachedToWindowFingerprint : Fingerprint(
    classFingerprint = BottomSheetDialogClassFingerprint,
    name = "onAttachedToWindow",
    returnType = "V",
    parameters = listOf(),
)

/**
 * onCreateView of the sheet to save a video to a playlist, a dialog of its own.
 */
internal object AddToPlaylistCreateViewFingerprint : Fingerprint(
    returnType = "Landroid/view/View;",
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "playlist_add_to_fragment"),
    )
)
