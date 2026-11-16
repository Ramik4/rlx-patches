/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - Jav1x (https://github.com/Jav1x)
 *
 * Ported to morphe-patches: https://github.com/MorpheApp/morphe-patches
 * Modified by: Jav1x (https://github.com/Jav1x)
 * Original YaVoT add-on: MarcaDian (https://github.com/MarcaDian)
 * Timing and coordination with the Morphe voice over translation: Dual VoT maintainers
 * Substantially modified by: YaVoT maintainers (sashade8-ship-it),
 * https://github.com/sashade8-ship-it/morphe-patches-yavot
 *
 * Modified by RLX Patches (https://github.com/Ramik4/rlx-patches), 2026:
 * built into the RLX bundle instead of loaded as an add-on bundle, settings in RLX Customizations.
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Attribution Notice
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Attribution (Section 7(b)): This specific copyright notice and the
 *    list of original authors above must be preserved in any copy or
 *    derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin (Section 7(c)): Modified versions must be clearly marked as
 *    such (e.g., by adding a "Modified by" line or a new copyright notice).
 *    They must not be misrepresented as the original work.
 */

package app.rlx.patches.youtube.video.yandexvot

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.patches.shared.misc.settings.preference.InputType
import app.morphe.patches.shared.misc.settings.preference.ListPreference
import app.morphe.patches.shared.misc.settings.preference.NonInteractivePreference
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.patches.shared.misc.settings.preference.TextPreference
import app.morphe.patches.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import app.morphe.patches.youtube.layout.player.buttons.playerOverlayButtonsHookPatch
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.misc.playercontrols.legacyPlayerControlsPatch
import app.morphe.patches.youtube.misc.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.misc.settings.settingsPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.shared.YouTubeActivityOnCreateFingerprint
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.videoid.videoIdPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.rlx.patches.youtube.settings.RLX_CUSTOMIZATIONS_SCREEN
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_PACKAGE = "Lapp/rlx/extension/youtube/yandexvot"
private const val EXTENSION_REGISTER_METHOD = "$EXTENSION_PACKAGE/YandexVot;->register()V"
private const val EXTENSION_ORIGINAL_VOLUME_CLASS = "$EXTENSION_PACKAGE/YandexVotOriginalVolumePatch;"
private const val EXTENSION_PREFERENCE_PACKAGE = "app.rlx.extension.youtube.yandexvot"

private const val AUDIO_TRACK_CLASS = "Landroid/media/AudioTrack;"

private fun MethodReference.isAudioTrackSetVolume() =
    definingClass == AUDIO_TRACK_CLASS &&
            name == "setVolume" &&
            parameterTypes.toList() == listOf("F") &&
            returnType == "I"

private fun getVolumeRegister(instruction: Instruction): Int? = when (instruction) {
    is FiveRegisterInstruction -> if (instruction.registerCount >= 2) instruction.registerD else null
    is TwoRegisterInstruction -> instruction.registerB
    is RegisterRangeInstruction ->
        if (instruction.registerCount >= 2) instruction.startRegister + instruction.registerCount - 1 else null
    else -> null
}

private fun getAudioTrackRegister(instruction: Instruction): Int? = when (instruction) {
    is FiveRegisterInstruction -> if (instruction.registerCount >= 1) instruction.registerC else null
    is TwoRegisterInstruction -> instruction.registerA
    is RegisterRangeInstruction -> if (instruction.registerCount >= 1) instruction.startRegister else null
    else -> null
}

/**
 * Method of the player that sets the volume of its AudioTrack.
 */
internal object AudioTrackSetVolumeMethodFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = AUDIO_TRACK_CLASS,
            name = "setVolume",
            parameters = listOf("F"),
            returnType = "I"
        )
    )
)

@Suppress("unused")
val yandexVoiceOverTranslationPatch = patch(
    name = "Voice over translation (Yandex)",
    description = "Adds a player button that plays a Yandex voice over translation of the video. " +
            "Settings are in RLX Customizations.",
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        // Patches that call the player hooks of AddOnApi, which the translation subscribes to.
        playerOverlayButtonsHookPatch,
        legacyPlayerControlsPatch,
        videoInformationPatch,
        videoIdPatch,
        playerTypeHookPatch,
    )

    execute {
        copyResources(
            "rlx",
            ResourceGroup("drawable", "morphe_yt_yandex_vot.xml", "morphe_yt_yandex_vot_bold.xml"),
        )

        RLX_CUSTOMIZATIONS_SCREEN.addPreferences(
            PreferenceScreenPreference(
                key = "rlx_yandex_vot_screen",
                titleKey = "morphe_yandex_vot_screen_title",
                summaryKey = null,
                sorting = PreferenceScreenPreference.Sorting.UNSORTED,
                preferences = setOf(
                    noTitleUnsortedPreferenceCategory(
                        SwitchPreference("morphe_yandex_vot_enabled", summary = true),
                        ListPreference("morphe_yandex_vot_source_language"),
                        ListPreference("morphe_yandex_vot_target_language"),
                        SwitchPreference("morphe_yandex_vot_use_live_voices", summary = true),
                        NonInteractivePreference(
                            "morphe_yandex_vot_translation_volume",
                            tag = "app.morphe.extension.shared.settings.preference.SeekBarPreference",
                            selectable = true,
                        ),
                        NonInteractivePreference(
                            "morphe_yandex_vot_original_audio_volume",
                            tag = "app.morphe.extension.shared.settings.preference.SeekBarPreference",
                            selectable = true,
                        ),
                        NonInteractivePreference(
                            "morphe_yandex_vot_oauth_token",
                            tag = "$EXTENSION_PREFERENCE_PACKAGE.YandexVotOAuthPreference",
                            selectable = true,
                        ),
                    ),
                    noTitleUnsortedPreferenceCategory(
                        ListPreference("morphe_yandex_vot_timer_position"),
                        SwitchPreference("morphe_yandex_vot_progress_ring_enabled", summary = true),
                        TextPreference(
                            "morphe_yandex_vot_progress_ring_color",
                            tag = "app.morphe.extension.shared.settings.preference.ColorPickerPreference",
                            inputType = InputType.TEXT_CAP_CHARACTERS,
                        ),
                        NonInteractivePreference(
                            "morphe_yandex_vot_progress_ring_thickness",
                            tag = "app.morphe.extension.shared.settings.preference.SeekBarPreference",
                            selectable = true,
                        ),
                    ),
                    noTitleUnsortedPreferenceCategory(
                        SwitchPreference(
                            "morphe_yandex_vot_audio_proxy_enabled",
                            titleKey = "morphe_yandex_vot_audio_proxy_title",
                            summary = true,
                        ),
                        TextPreference("morphe_yandex_vot_proxy_url"),
                    ),
                    noTitleUnsortedPreferenceCategory(
                        // Attribution of the authors of the translation (GPLv3 section 7b).
                        NonInteractivePreference(
                            "rlx_yandex_vot_credits",
                            titleKey = null,
                            summaryKey = "morphe_yandex_vot_rlx_attribution",
                        ),
                    ),
                ),
            ),
        )

        // Subscribes to the player hooks on app start, before any video, so the settings are
        // also registered for the settings screen and the settings import and export.
        YouTubeActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { }, $EXTENSION_REGISTER_METHOD",
        )

        // Lowers the original audio while the translation plays:
        // every AudioTrack.setVolume of the player goes through the extension.
        AudioTrackSetVolumeMethodFingerprint.method.apply {
            val index = indexOfFirstInstructionOrThrow {
                (opcode == Opcode.INVOKE_VIRTUAL || opcode == Opcode.INVOKE_VIRTUAL_RANGE) &&
                        getReference<MethodReference>()?.isAudioTrackSetVolume() == true
            }
            val instruction = implementation!!.instructions.elementAt(index)
            val audioTrackRegister = getAudioTrackRegister(instruction)
                ?: throw PatchException("Cannot find the AudioTrack register")
            val volumeRegister = getVolumeRegister(instruction)
                ?: throw PatchException("Cannot find the volume register")

            addInstructions(
                index,
                """
                    invoke-static { v$audioTrackRegister, v$volumeRegister }, $EXTENSION_ORIGINAL_VOLUME_CLASS->applyVolumeMultiplier(Landroid/media/AudioTrack;F)F
                    move-result v$volumeRegister
                """,
            )
        }
    }
}
