/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.extension.shared;

import static app.morphe.extension.shared.StringRef.str;

import android.preference.ListPreference;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.ResourceType;
import app.morphe.extension.shared.ResourceUtils;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;

/**
 * The RLX branding in the lists of the settings (GPLv3 section 7e, see NOTICE):
 * <ul>
 * <li>The Morphe app icons, notification icons, header logo and splash animations are hidden.
 * The original YouTube options stay.</li>
 * <li>The icon styles that show the RLX icons (see RlxBranding.kt of the patches) get RLX names,
 * and come first after the original icons.</li>
 * <li>The RLX header logos are added after the original logos.</li>
 * </ul>
 * A hidden value that is still selected stays in the list, so the selection is shown.
 */
@SuppressWarnings({"unused", "deprecation"})
public final class RlxBranding {

    private static final String ICON_KEY = "morphe_custom_branding_icon";
    private static final String NOTIFICATION_ICON_KEY = "morphe_custom_branding_notification_icon";
    private static final String HEADER_KEY = "morphe_header_logo";

    private static final Map<String, Set<String>> HIDDEN_VALUES = Map.of(
            ICON_KEY, Set.of("BLACK", "DARK"),
            NOTIFICATION_ICON_KEY, Set.of("BLACK", "DARK"),
            HEADER_KEY, Set.of("MORPHE"),
            "morphe_splash_screen_style",
            Set.of("MORPHE_COLOR", "MORPHE_MONOCHROME", "RIVE_GLOW_MORPHE", "RIVE_GLOW_MORPHE_2")
    );

    /** Icon style value to the string of its RLX name, in the order of the list. */
    private static final String[][] ICON_NAMES = {
            {"PLAY", "rlx_branding_red"},
            {"PLAY_BLACK", "rlx_branding_green"},
            {"LIGHT", "rlx_branding_layered"},
    };

    /** Header logo value, string of its name, and a drawable that only exists in the app that has it. */
    private static final String[][] HEADER_LOGOS = {
            {"RLX_RED", "rlx_branding_red", "rlx_header_red_youtube_light"},
            {"RLX_GREEN", "rlx_branding_green", "rlx_header_green_youtube_light"},
            {"RLX_RED_PREMIUM", "rlx_branding_red_premium", "rlx_header_red_premium_light"},
            {"RLX_GREEN_PREMIUM", "rlx_branding_green_premium", "rlx_header_green_premium_light"},
            {"RLX_RED", "rlx_branding_red", "rlx_header_music_red_dark"},
            {"RLX_GREEN", "rlx_branding_green", "rlx_header_music_green_dark"},
    };

    /**
     * The app name when no custom name was given while patching: name 2 of Custom branding, which RLX
     * names "YouTube RLX" and "YT Music RLX". Must match RLX_DEFAULT_APP_NAME_INDEX of the patches.
     */
    public static final int DEFAULT_APP_NAME_INDEX = 2;

    private RlxBranding() {
    }

    /**
     * Injection point (CustomBrandingPatch.setBranding, an RLX hook), before the launch alias is selected.
     * <p>
     * The last app name is the custom name given while patching. Without one, its launch alias only has the
     * placeholder name "Custom", yet Morphe keeps it when it is still selected from an earlier install or
     * an imported settings file. RLX selects the default name instead.
     *
     * @param userProvidedCustomName If a custom name was given while patching.
     * @param numberOfPresetAppNames The number of app names, the custom name included.
     */
    public static void checkAppName(boolean userProvidedCustomName, int numberOfPresetAppNames) {
        try {
            if (userProvidedCustomName) return;
            final int selected = SharedYouTubeSettings.CUSTOM_BRANDING_NAME.get();
            if (selected >= numberOfPresetAppNames || selected < 1) {
                Logger.printInfo(() -> "No custom app name, selecting the default name instead of " + selected);
                SharedYouTubeSettings.CUSTOM_BRANDING_NAME.resetToDefault();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "checkAppName failure", ex);
        }
    }

    /**
     * Injection point. Called when a list preference of the settings is attached, before it reads its value.
     */
    public static void onListPreferenceAttached(ListPreference preference) {
        try {
            final String key = preference.getKey();
            if (key == null) return;
            Set<String> hidden = HIDDEN_VALUES.get(key);
            if (hidden == null) return;

            CharSequence[] entries = preference.getEntries();
            CharSequence[] values = preference.getEntryValues();
            if (entries == null || values == null || entries.length != values.length) return;

            final String selected = selectedValue(key);
            List<CharSequence> keptEntries = new ArrayList<>(entries.length + HEADER_LOGOS.length);
            List<CharSequence> keptValues = new ArrayList<>(values.length + HEADER_LOGOS.length);
            for (int i = 0; i < values.length; i++) {
                String value = values[i].toString();
                if (hidden.contains(value) && !value.equalsIgnoreCase(selected)) continue;
                keptEntries.add(entries[i]);
                keptValues.add(values[i]);
            }

            if (key.equals(ICON_KEY) || key.equals(NOTIFICATION_ICON_KEY)) {
                nameIcons(keptEntries, keptValues);
            } else if (key.equals(HEADER_KEY)) {
                addHeaderLogos(keptEntries, keptValues);
            }

            preference.setEntries(keptEntries.toArray(new CharSequence[0]));
            preference.setEntryValues(keptValues.toArray(new CharSequence[0]));
        } catch (Exception ex) {
            Logger.printException(() -> "Could not set the RLX branding options", ex);
        }
    }

    /**
     * Gives the icon styles of the RLX icons their RLX names, and moves them, in the order of
     * {@link #ICON_NAMES}, to the place of the first of them.
     */
    private static void nameIcons(List<CharSequence> entries, List<CharSequence> values) {
        int insertAt = -1;
        List<CharSequence> rlxEntries = new ArrayList<>();
        List<CharSequence> rlxValues = new ArrayList<>();
        for (String[] icon : ICON_NAMES) {
            int index = indexOf(values, icon[0]);
            if (index < 0) continue;
            insertAt = insertAt < 0 ? index : Math.min(insertAt, index);
            rlxEntries.add(str(icon[1]));
            rlxValues.add(icon[0]);
        }
        if (insertAt < 0) return;

        for (int i = values.size() - 1; i >= 0; i--) {
            if (indexOf(rlxValues, values.get(i).toString()) >= 0) {
                if (i < insertAt) insertAt--;
                values.remove(i);
                entries.remove(i);
            }
        }
        values.addAll(insertAt, rlxValues);
        entries.addAll(insertAt, rlxEntries);
    }

    /**
     * Adds the RLX header logos the app has, after the original logos and before the custom logo.
     */
    private static void addHeaderLogos(List<CharSequence> entries, List<CharSequence> values) {
        int insertAt = indexOf(values, "CUSTOM");
        if (insertAt < 0) insertAt = values.size();
        for (String[] logo : HEADER_LOGOS) {
            if (indexOf(values, logo[0]) >= 0) continue;
            if (ResourceUtils.getIdentifier(ResourceType.DRAWABLE, logo[2]) == 0) continue;
            entries.add(insertAt, str(logo[1]));
            values.add(insertAt, logo[0]);
            insertAt++;
        }
    }

    private static int indexOf(List<CharSequence> list, String value) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).toString().equals(value)) return i;
        }
        return -1;
    }

    @Nullable
    private static String selectedValue(String key) {
        Setting<?> setting = Setting.getSettingFromPath(key);
        return setting != null ? String.valueOf(setting.get()) : null;
    }
}
