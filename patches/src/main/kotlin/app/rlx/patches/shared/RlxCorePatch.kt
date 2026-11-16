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
import app.morphe.patcher.patch.mutableClassDefByOrNull
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

private const val MORPHE_EXPORTER_CLASS = "Lapp/morphe/extension/shared/patches/OfflineVideoExporter;"

/** Folders inside Download of the videos exported from the downloads of the app: Morphe's and RLX's. */
private val exportFolders = mapOf(
    "Morphe videos" to "YouTube",
    "Morphe music" to "YouTube Music",
)

private const val MORPHE_BRANDING_THEME_CLASS =
    "Lapp/morphe/extension/shared/patches/CustomBrandingPatch\$BrandingTheme;"

private const val MORPHE_ABOUT_PREFERENCE_CLASS =
    "Lapp/morphe/extension/shared/settings/preference/about/MorpheAboutPreference;"

internal const val RLX_PREFERENCE_PACKAGE = "app.rlx.extension.shared.settings.preference"

/**
 * The RLX core of an app, applied with the Morphe settings of the app:
 *
 * - The "RLX Customizations" settings screen, which the other RLX patches add their settings to, and the Morphe
 *   branding, theme and settings options are moved to (RlxCustomizations.kt).
 * - The RLX default settings of rlx/defaults.json, applied once per setting.
 * - The "Based on Morphe Patches" attribution on the "About" entry of the settings (GPLv3 section 7b).
 *
 * @param appId Section of rlx/defaults.json for the app, "youtube" or "music".
 * @param customizationsScreen The "RLX Customizations" screen of the app.
 */
internal fun rlxCorePatch(
    appId: String,
    sharedExtensionPatch: Patch,
    mainActivityOnCreateFingerprint: Fingerprint,
    customizationsScreen: BasePreferenceScreen.Screen,
) = patch(
    description = "Adds the RLX Customizations settings, the RLX default settings and the Morphe Patches attribution.",
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
                *listOf("customizations", "downloads", "swipe_seekbar", "community").flatMap {
                    listOf("rlx_settings_screen_$it.xml", "rlx_settings_screen_${it}_bold.xml")
                }.toTypedArray(),
            ),
        )

        // Videos exported from the downloads of the app go to Download/YouTube (YouTube Music: Download/YouTube
        // Music), next to the files of Save to device, instead of the Morphe folders.
        mutableClassDefByOrNull(MORPHE_EXPORTER_CLASS)?.methods?.forEach { method ->
            val implementation = method.implementation ?: return@forEach
            implementation.instructions.withIndex().toList().forEach { (index, instruction) ->
                if (instruction.opcode != Opcode.CONST_STRING) return@forEach
                val folder = exportFolders[((instruction as ReferenceInstruction).reference as StringReference).string]
                    ?: return@forEach
                val register = method.getInstruction<OneRegisterInstruction>(index).registerA
                method.replaceInstruction(index, "const-string v$register, \"$folder\"")
            }
        } ?: rlxLogger.warning("RLX: $MORPHE_EXPORTER_CLASS not found, exported videos keep the Morphe folder")

        // Startup animation of RLX Green: green, where Morphe gives RLX Red and RLX Green (the play icons) the same
        // animation (RlxBranding.kt).
        mutableClassDefByOrNull(MORPHE_BRANDING_THEME_CLASS)?.methods
            ?.firstOrNull { it.name == "startupAnimationResourceName" }
            ?.apply {
                implementation!!.instructions
                    .withIndex()
                    .filter { (_, instruction) -> instruction.opcode == Opcode.RETURN_OBJECT }
                    .map { (index, instruction) -> index to (instruction as OneRegisterInstruction).registerA }
                    .reversed()
                    .forEach { (index, register) ->
                        addInstructionsAtControlFlowLabel(
                            index,
                            """
                                invoke-static { p0, v$register }, $BRANDING_CLASS->startupAnimation(Ljava/lang/Enum;Ljava/lang/String;)Ljava/lang/String;
                                move-result-object v$register
                            """,
                        )
                    }
            }
            ?: rlxLogger.warning("RLX: startupAnimationResourceName not found, RLX Green has the red startup animation")

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

        // The last setting of RLX Customizations (RlxCustomizations.kt).
        customizationsScreen.addPreferences(
            NonInteractivePreference(
                key = RLX_RESET_DEFAULTS_KEY,
                tag = "$RLX_PREFERENCE_PACKAGE.RlxResetDefaultsPreference",
                selectable = true,
            ),
        )
    }

    // RLX app icons and header logos, after Custom branding and Change header added theirs.
    finalize {
        // Morphe strings and arrays that RLX replaces (the app names of Custom branding, the settings names).
        // Added here, after the resources of the app, so they replace the Morphe ones (addResourcesPatch
        // keeps the last resource of a name and runs after this).
        addAppResources("rlx-shared")
        addAppResources("rlx-$appId")
        applyRlxBranding(appId)
    }
}
