/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.patches.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.misc.settings.preference.BasePreference
import app.morphe.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.morphe.patches.shared.misc.settings.preference.PreferenceCategory
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.morphe.patches.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory

/** Key of the "Restore RLX default settings" preference, the last of RLX Customizations. */
internal const val RLX_RESET_DEFAULTS_KEY = "rlx_reset_defaults"

/**
 * The Morphe settings shown on RLX Customizations, in groups, in this order. A setting is found by its key,
 * or by its title for a preference without a key. A setting of a patch that is not applied is left out.
 * The settings of the RLX patches (Save to device, voice over translation) come after the [RLX_GROUPS_BEFORE]
 * groups.
 */
private val customizations = listOf(
    // Branding.
    listOf(
        "morphe_custom_branding_name",
        "morphe_custom_branding_icon",
        "morphe_header_logo",
        "morphe_splash_screen_style",
        "morphe_splash_screen_duration",
        "morphe_custom_branding_notification_icon",
    ),
    // Font.
    listOf(
        "morphe_custom_font_type",
        "morphe_custom_font_file",
    ),
    // Theme.
    listOf(
        "morphe_theme_color_dark",
        "morphe_theme_color_dark_custom",
        "morphe_theme_color_light",
        "morphe_theme_color_light_custom",
        "morphe_theme_color_change_foreground",
    ),
    // Settings. "Restore RLX default settings" is added at the end.
    listOf(
        "morphe_settings_name",
        "morphe_language",
        "morphe_pref_import_export_title",
    ),
)

/** The number of [customizations] groups before the settings of the RLX patches. */
private const val RLX_GROUPS_BEFORE = 3

/**
 * Moves the Morphe settings of [customizations] from [sources] to the RLX Customizations screen [target].
 * Called by the Morphe settings patch of the app when all patches added their settings, right before the
 * settings are written (RLX hook), so no Morphe patch needs to know about the RLX screen.
 */
internal fun moveToRlxCustomizations(
    sources: List<BasePreferenceScreen.Screen>,
    target: BasePreferenceScreen.Screen,
) {
    val groups = customizations.map { ids ->
        ids.mapNotNull { id -> sources.firstNotNullOfOrNull { take(it, id) } }
    }

    // The settings of the RLX patches, which were added to the screen directly.
    val own = target.preferences.toList()
    val (reset, patches) = own.partition { it.key == RLX_RESET_DEFAULTS_KEY }
    target.preferences.clear()

    fun addGroup(preferences: List<BasePreference>) {
        if (preferences.isNotEmpty()) target.addPreferences(noTitleUnsortedPreferenceCategory(preferences.toSet()))
    }

    groups.take(RLX_GROUPS_BEFORE).forEach(::addGroup)
    addGroup(patches)
    groups.drop(RLX_GROUPS_BEFORE).dropLast(1).forEach(::addGroup)
    addGroup(groups.last() + reset)
}

private fun BasePreference.isSetting(id: String) = key == id || (key == null && titleKey == id)

/** Removes the setting [id] from [screen] (also from a category of it) and returns it, or null if not found. */
private fun take(screen: BasePreferenceScreen.Screen, id: String): BasePreference? =
    take(screen.preferences, id) ?: screen.categories.firstNotNullOfOrNull { take(it.preferences, id) }

private fun take(preferences: MutableSet<BasePreference>, id: String): BasePreference? {
    val list = preferences.toList()
    list.forEachIndexed { index, preference ->
        if (preference.isSetting(id)) {
            preferences.remove(preference)
            return preference
        }
        if (preference is PreferenceCategory) {
            val children = preference.preferences.toMutableSet()
            val found = take(children, id) ?: return@forEachIndexed
            // The preferences of a category cannot be changed, so a copy without the setting takes its place,
            // or none if it is empty.
            val replacement = if (children.isEmpty()) null else preference.copyWith(children)
            preferences.clear()
            preferences.addAll(list.subList(0, index))
            replacement?.let(preferences::add)
            preferences.addAll(list.subList(index + 1, list.size))
            return found
        }
    }
    return null
}

private fun PreferenceCategory.copyWith(preferences: Set<BasePreference>): PreferenceCategory {
    if (this::class != PreferenceCategory::class) {
        throw PatchException("Cannot move a setting out of ${this::class.simpleName} $key")
    }
    // The key already has the sort type of the category, which UNSORTED keeps as it is.
    return PreferenceCategory(
        key = key,
        titleKey = titleKey,
        icon = icon,
        iconBold = iconBold,
        layout = layout,
        sorting = Sorting.UNSORTED,
        tag = tag,
        preferences = preferences,
    )
}
