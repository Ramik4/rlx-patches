/*
 * Copyright 2026 RLX Patches.
 * https://github.com/Ramik4/rlx-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.rlx.patches.youtube.layout.glassmenus

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.misc.settings.preference.NonInteractivePreference
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.patches.youtube.layout.flyout.FeedPopupWindowFlyoutFingerprint
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.misc.settings.PreferenceScreen
import app.morphe.patches.youtube.misc.settings.settingsPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.shared.YouTubeActivityOnCreateFingerprint
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/rlx/extension/youtube/glassmenus/GlassMenus;"
private const val EXTENSION_SETTINGS_CLASS = "Lapp/rlx/extension/youtube/glassmenus/GlassMenusSettings;"
private const val SEEKBAR_PREFERENCE = "app.morphe.extension.shared.settings.preference.SeekBarPreference"

@Suppress("unused")
val glassMenusPatch = patch(
    name = "Glass menus",
    description = "Adds an option to show the menus, such as the menu of a video, the player settings, " +
        "the share sheet, the video quality and the sheets of Morphe, as frosted glass: the content behind the menu " +
        "is blurred. Needs Android 12 or later.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        PreferenceScreen.GENERAL.addPreferences(
            PreferenceScreenPreference(
                key = "morphe_glass_menus_screen",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    SwitchPreference("morphe_glass_menus", summary = true),
                    NonInteractivePreference(key = "morphe_glass_menus_opacity", tag = SEEKBAR_PREFERENCE),
                    NonInteractivePreference(key = "morphe_glass_menus_blur", tag = SEEKBAR_PREFERENCE),
                    NonInteractivePreference(key = "morphe_glass_menus_dim", tag = SEEKBAR_PREFERENCE),
                ),
            ),
        )

        // Loads the settings on app start, so they are known to the settings screen, the search and
        // the import and export of the settings.
        YouTubeActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { }, $EXTENSION_SETTINGS_CLASS->load()V",
        )

        // Bottom sheets: every bottom sheet dialog of the app, when it is attached to its window.
        BottomSheetDialogOnAttachedToWindowFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION_CLASS->onBottomSheetDialog(Landroid/app/Dialog;)V",
        )

        // The sheet to save a video to a playlist, which is a dialog of its own.
        AddToPlaylistCreateViewFingerprint.method.apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                addInstruction(
                    index,
                    "invoke-static { v$register }, $EXTENSION_CLASS->onAddToPlaylistView(Landroid/view/View;)V",
                )
            }
        }

        // Dialogs of dialog fragments, right after they are shown: the panels at the bottom of the screen,
        // such as the video quality and the download quality.
        DialogFragmentOnStartFingerprint.let {
            it.method.apply {
                val showIndex = it.instructionMatches.first().index
                val dialogRegister = getInstruction<FiveRegisterInstruction>(showIndex).registerC
                addInstruction(
                    showIndex + 1,
                    "invoke-static { v$dialogRegister }, $EXTENSION_CLASS->onDialogShown(Landroid/app/Dialog;)V",
                )
            }
        }

        // The sheets of Morphe and RLX.
        SlideDialogCreateFingerprint.method.apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                addInstruction(
                    index,
                    "invoke-static { v$register }, $EXTENSION_CLASS->onSlideDialog(Landroid/app/Dialog;)V",
                )
            }
        }

        // Popup menus of large screens. The index is searched again, because other patches add code to these methods.
        FeedPopupWindowFlyoutFingerprint.matchAll(2..4).forEach { match ->
            match.method.apply {
                val index = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL && getReference<MethodReference>()?.let {
                        it.definingClass == "Landroid/widget/PopupWindow;" && it.name == "setOnDismissListener"
                    } == true
                }
                val register = getInstruction<FiveRegisterInstruction>(index).registerC
                addInstruction(
                    index,
                    "invoke-static { v$register }, $EXTENSION_CLASS->onPopupWindow(Landroid/widget/PopupWindow;)V",
                )
            }
        }
    }
}
