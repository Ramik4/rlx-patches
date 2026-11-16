/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.patches.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.all.misc.resources.addAppResources
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.morphe.patches.shared.misc.settings.preference.NonInteractivePreference
import app.morphe.util.ResourceGroup
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.copyResources
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.Base64

private const val EXTENSION_PACKAGE = "Lapp/rlx/extension/shared"
private const val INFO_CLASS = "$EXTENSION_PACKAGE/RlxInfo;"
private const val DEFAULTS_CLASS = "$EXTENSION_PACKAGE/RlxDefaults;"
private const val ABOUT_CLASS = "$EXTENSION_PACKAGE/RlxAbout;"
private const val BRANDING_CLASS = "$EXTENSION_PACKAGE/RlxBranding;"

private const val MORPHE_LIST_PREFERENCE_CLASS =
    "Lapp/morphe/extension/shared/settings/preference/CustomDialogListPreference;"

private const val MORPHE_SETTINGS_NAME_CLASS = "Lapp/morphe/extension/shared/patches/SettingsNamePatch;"
private const val MORPHE_SETTINGS_TITLE = "morphe_settings_title"
private const val RLX_SETTINGS_TITLE = "rlx_settings_title"

private const val MORPHE_ABOUT_PREFERENCE_CLASS =
    "Lapp/morphe/extension/shared/settings/preference/about/MorpheAboutPreference;"

internal const val RLX_PREFERENCE_PACKAGE = "app.rlx.extension.shared.settings.preference"

/**
 * The RLX core of an app, applied with the Morphe settings of the app:
 *
 * - The "RLX Extended" settings screen, which the other RLX patches add their settings to.
 * - The RLX default settings of rlx/defaults.json, applied once per setting.
 * - The "Based on Morphe Patches" attribution on the "About" entry of the settings (GPLv3 section 7b).
 *
 * @param appId Section of rlx/defaults.json for the app, "youtube" or "music".
 * @param extendedScreen The "RLX Extended" screen of the app.
 */
internal fun rlxCorePatch(
    appId: String,
    sharedExtensionPatch: Patch,
    mainActivityOnCreateFingerprint: Fingerprint,
    extendedScreen: BasePreferenceScreen.Screen,
) = patch(
    description = "Adds the RLX Extended settings, the RLX default settings and the Morphe Patches attribution.",
) {
    dependsOn(
        sharedExtensionPatch,
        addResourcesPatch,
    )

    execute {
        addAppResources("rlx")

        copyResources(
            "rlx",
            ResourceGroup(
                "drawable",
                "rlx_settings_screen_extended.xml",
                "rlx_settings_screen_extended_bold.xml",
            ),
        )

        // Versions shown in the settings.
        mutableClassDefBy(INFO_CLASS).methods.apply {
            first { it.name == "rlxVersion" }.returnEarly(RlxBuildInfo.rlxVersion)
            first { it.name == "morpheBaseVersion" }.returnEarly(RlxBuildInfo.morpheBaseVersion)
        }

        // Default settings. Base64 keeps quotes, backslashes and non-ASCII text out of the string constant.
        mutableClassDefBy(DEFAULTS_CLASS).methods.apply {
            first { it.name == "defaultsBase64" }
                .returnEarly(Base64.getEncoder().encodeToString(RlxBuildInfo.defaultsJson))
            first { it.name == "appId" }.returnEarly(appId)
        }
        mainActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $DEFAULTS_CLASS->onActivityCreated(Landroid/app/Activity;)V",
        )

        // Settings name: "RLX" in place of "Morphe" (GPLv3 section 7c and 7e, see NOTICE). The entry of the
        // app settings declares its title in the Morphe settings patches (RLX hooks); the toolbar and other
        // places without a name declared while patching read the string here.
        mutableClassDefBy(MORPHE_SETTINGS_NAME_CLASS).methods.first { it.name == "getSettingsName" }.apply {
            val index = instructions.indexOfFirst { instruction ->
                instruction.opcode == Opcode.CONST_STRING &&
                    ((instruction as ReferenceInstruction).reference as StringReference).string == MORPHE_SETTINGS_TITLE
            }
            if (index < 0) throw PatchException("$MORPHE_SETTINGS_TITLE not found in getSettingsName")
            val register = getInstruction<OneRegisterInstruction>(index).registerA
            replaceInstruction(index, "const-string v$register, \"$RLX_SETTINGS_TITLE\"")
        }

        // Morphe app icons, header logo and splash animations are hidden from the lists of the settings.
        mutableClassDefBy(MORPHE_LIST_PREFERENCE_CLASS).methods
            .first { it.name == "onAttachedToHierarchy" }
            .addInstruction(
                0,
                "invoke-static/range { p0 .. p0 }, $BRANDING_CLASS->onListPreferenceAttached(Landroid/preference/ListPreference;)V",
            )

        // "About" entry: RLX version, Morphe Patches base version and the RLX about dialog.
        mutableClassDefBy(MORPHE_ABOUT_PREFERENCE_CLASS).methods
            .filter { it.name == "<init>" }
            .forEach { constructor ->
                constructor.implementation!!.instructions
                    .withIndex()
                    .filter { (_, instruction) -> instruction.opcode == Opcode.RETURN_VOID }
                    .map { (index, _) -> index }
                    .reversed()
                    .forEach { index ->
                        constructor.addInstructionsAtControlFlowLabel(
                            index,
                            "invoke-static/range { p0 .. p0 }, $ABOUT_CLASS->onCreated(Landroid/preference/Preference;)V",
                        )
                    }
            }

        extendedScreen.addPreferences(
            NonInteractivePreference(
                key = "rlx_info",
                summaryKey = null,
                tag = "$RLX_PREFERENCE_PACKAGE.RlxInfoPreference",
                selectable = true,
            ),
            NonInteractivePreference(
                key = "rlx_reset_defaults",
                tag = "$RLX_PREFERENCE_PACKAGE.RlxResetDefaultsPreference",
                selectable = true,
            ),
        )
    }

    // RLX app icons and header logos, after Custom branding and Change header added theirs.
    finalize {
        applyRlxBranding(appId)
    }
}
