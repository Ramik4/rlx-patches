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

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.ResourceType;
import app.morphe.extension.shared.ResourceUtils;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;

/**
 * The RLX branding in the lists of the settings (GPLv3 section 7e, see NOTICE).
 * <p>
 * Each list of {@link #specFor} is rebuilt in the RLX order and with the RLX names: the RLX app icons,
 * notification icons, header logos, startup animation, app names and settings names. The Morphe
 * options that are not in a list (the Morphe icons, header logo and animations, and some Morphe names)
 * are hidden. A hidden value that is still selected stays at the end of the list, so the selection is shown.
 */
@SuppressWarnings({"unused", "deprecation"})
public final class RlxBranding {

    /**
     * The app name when no custom name was given while patching: name 2 of Custom branding, "YouTube RLX"
     * or "YT Music RLX". Must match RLX_DEFAULT_APP_NAME_INDEX of the patches.
     */
    public static final int DEFAULT_APP_NAME_INDEX = 2;

    /**
     * The number of app names, the custom name (the last) included. Must match RLX_APP_NAME_COUNT of the patches.
     */
    public static final int APP_NAME_COUNT = 9;

    private static final String ICON_KEY = "morphe_custom_branding_icon";
    private static final String NOTIFICATION_ICON_KEY = "morphe_custom_branding_notification_icon";
    private static final String HEADER_KEY = "morphe_header_logo";
    private static final String SPLASH_KEY = "morphe_splash_screen_style";
    private static final String APP_NAME_KEY = "morphe_custom_branding_name";
    private static final String SETTINGS_NAME_KEY = "morphe_settings_name";

    /** The value of the custom app name in Morphe's own list, which has 5 names. */
    private static final String MORPHE_CUSTOM_APP_NAME_VALUE = "5";

    /** When an option of a list is shown. */
    private enum Show {
        /** If the original list has it. */
        ORIGINAL,
        /** Always. */
        ALWAYS,
        /** If the app has the drawable of the option. */
        DRAWABLE,
        /** If a custom app name was given while patching. */
        CUSTOM_APP_NAME,
    }

    /** An option of a list. */
    private static final class Option {
        final String value;
        /** "str:" a string of the patches, "res:" a string resource of the app, or null: the original name. */
        @Nullable
        final String label;
        final Show show;
        @Nullable
        final String drawable;

        Option(String value, @Nullable String label, Show show, @Nullable String drawable) {
            this.value = value;
            this.label = label;
            this.show = show;
            this.drawable = drawable;
        }
    }

    private static Option option(String value, @Nullable String label) {
        return new Option(value, label, Show.ORIGINAL, null);
    }

    private static Option added(String value, @Nullable String label) {
        return new Option(value, label, Show.ALWAYS, null);
    }

    private static Option header(String value, String label, String drawable) {
        return new Option(value, label, Show.DRAWABLE, drawable);
    }

    private static Option appName(int index) {
        return added(String.valueOf(index), "res:morphe_custom_branding_name_entry_" + index);
    }

    private static Option customAppName() {
        return new Option(String.valueOf(APP_NAME_COUNT), "str:morphe_custom_branding_icon_entry_7",
                Show.CUSTOM_APP_NAME, null);
    }

    private static final Option[] YOUTUBE_ICONS = {
            option("ORIGINAL", "str:rlx_branding_original_youtube"),
            option("YOUTUBE_ICON1", "str:rlx_branding_original_analog"),
            option("YOUTUBE_ICON2", "str:rlx_branding_original_cake"),
            option("YOUTUBE_ICON3", "str:rlx_branding_original_metal"),
            option("PLAY", "str:rlx_branding_red"),
            option("PLAY_BLACK", "str:rlx_branding_green"),
            option("LIGHT", "str:rlx_branding_layered"),
            option("CUSTOM", null),
    };

    private static final Option[] MUSIC_ICONS = {
            option("ORIGINAL", "str:rlx_branding_original_music"),
            option("PLAY", "str:rlx_branding_red_music"),
            option("PLAY_BLACK", "str:rlx_branding_green_music"),
            option("LIGHT", "str:rlx_branding_layered_music"),
            option("CUSTOM", null),
    };

    /** The red and green RLX icons have the same notification icon. */
    private static final Option[] YOUTUBE_NOTIFICATION_ICONS = {
            option("FOLLOW", null),
            option("ORIGINAL", "str:rlx_branding_notification_original_youtube"),
            option("PLAY", "str:rlx_branding_rlx"),
            added("LIGHT", "str:rlx_branding_layered"),
            option("CUSTOM", null),
    };

    private static final Option[] MUSIC_NOTIFICATION_ICONS = {
            option("FOLLOW", null),
            option("ORIGINAL", "str:rlx_branding_original_music"),
            option("PLAY", "str:rlx_branding_rlx_music"),
            option("CUSTOM", null),
    };

    private static final Option[] YOUTUBE_HEADERS = {
            option("DEFAULT", null),
            option("REGULAR", "str:rlx_branding_original_youtube"),
            option("PREMIUM", "str:rlx_branding_original_premium"),
            header("RLX_RED", "str:rlx_branding_red_youtube", "rlx_header_red_youtube_light"),
            header("RLX_GREEN", "str:rlx_branding_green_youtube", "rlx_header_green_youtube_light"),
            header("RLX_RED_PREMIUM", "str:rlx_branding_red_premium", "rlx_header_red_premium_light"),
            header("RLX_GREEN_PREMIUM", "str:rlx_branding_green_premium", "rlx_header_green_premium_light"),
            option("CUSTOM", null),
    };

    private static final Option[] MUSIC_HEADERS = {
            option("DEFAULT", "str:rlx_branding_header_default_original"),
            header("RLX_RED", "str:rlx_branding_red_music", "rlx_header_music_red_dark"),
            header("RLX_GREEN", "str:rlx_branding_green_music", "rlx_header_music_green_dark"),
            option("CUSTOM", null),
    };

    /** MORPHE_COLOR plays the RLX startup animation (RlxBranding.kt of the patches). */
    private static final Option[] SPLASH_STYLES = {
            option("DISABLED", null),
            option("FPS_60_ONE_SECOND", null),
            option("FPS_60_BLACK_AND_WHITE", null),
            option("RIVE_GLOW", null),
            option("MORPHE_COLOR", "str:rlx_branding_rlx"),
    };

    /** 1 is the original name; 4 (Morphe's "YT") is hidden. */
    private static final Option[] YOUTUBE_APP_NAMES = {
            option("1", null), appName(5), appName(2), appName(6), appName(7), appName(8), appName(3),
            customAppName(),
    };

    /** 1 is the original name; 4 and 8 (Morphe's "Music" and "Music Morphe") are hidden. */
    private static final Option[] MUSIC_APP_NAMES = {
            appName(5), option("1", null), appName(2), appName(6), appName(7), appName(3),
            customAppName(),
    };

    /** The names of the values are in the arrays of values/rlx-shared. */
    private static final Option[] SETTINGS_NAMES = {
            option("DEFAULT", null),
            option("RLX_SETTINGS", null),
            option("MORPHE_SETTINGS", null),
            option("EXTENDED", null),
            option("RLX", null),
            option("CUSTOM", null),
    };

    private RlxBranding() {
    }

    @Nullable
    private static Option[] specFor(String key) {
        final boolean music = "music".equals(RlxDefaults.appId());
        return switch (key) {
            case ICON_KEY -> music ? MUSIC_ICONS : YOUTUBE_ICONS;
            case NOTIFICATION_ICON_KEY -> music ? MUSIC_NOTIFICATION_ICONS : YOUTUBE_NOTIFICATION_ICONS;
            case HEADER_KEY -> music ? MUSIC_HEADERS : YOUTUBE_HEADERS;
            case SPLASH_KEY -> SPLASH_STYLES;
            case APP_NAME_KEY -> music ? MUSIC_APP_NAMES : YOUTUBE_APP_NAMES;
            case SETTINGS_NAME_KEY -> SETTINGS_NAMES;
            default -> null;
        };
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
            Option[] spec = specFor(key);
            if (spec == null) return;

            CharSequence[] entries = preference.getEntries();
            CharSequence[] values = preference.getEntryValues();
            if (entries == null || values == null || entries.length != values.length) return;

            List<CharSequence> newEntries = new ArrayList<>(spec.length + 1);
            List<CharSequence> newValues = new ArrayList<>(spec.length + 1);
            for (Option option : spec) {
                final int originalIndex = indexOf(values, option.value);
                if (!isShown(option, originalIndex, values)) continue;

                CharSequence label = label(option.label, originalIndex >= 0 ? entries[originalIndex] : null);
                if (label == null || contains(newEntries, label)) continue;
                newEntries.add(label);
                newValues.add(option.value);
            }

            // A hidden option that is selected stays, so the selection is shown.
            final String selected = selectedValue(key);
            if (selected != null && indexOf(newValues, selected) < 0) {
                final int originalIndex = indexOf(values, selected);
                if (originalIndex >= 0) {
                    newEntries.add(entries[originalIndex]);
                    newValues.add(values[originalIndex]);
                } else if (key.equals(APP_NAME_KEY)) {
                    CharSequence label = label("res:morphe_custom_branding_name_entry_" + selected, null);
                    if (label != null) {
                        newEntries.add(label);
                        newValues.add(selected);
                    }
                }
            }

            preference.setEntries(newEntries.toArray(new CharSequence[0]));
            preference.setEntryValues(newValues.toArray(new CharSequence[0]));
        } catch (Exception ex) {
            Logger.printException(() -> "Could not set the RLX branding options", ex);
        }
    }

    private static boolean isShown(Option option, int originalIndex, CharSequence[] originalValues) {
        return switch (option.show) {
            case ORIGINAL -> originalIndex >= 0;
            case ALWAYS -> true;
            case DRAWABLE -> ResourceUtils.getIdentifier(ResourceType.DRAWABLE, option.drawable) != 0;
            // Morphe's list has its custom name (5) only if one was given.
            case CUSTOM_APP_NAME -> indexOf(originalValues, MORPHE_CUSTOM_APP_NAME_VALUE) >= 0;
        };
    }

    @Nullable
    private static CharSequence label(@Nullable String label, @Nullable CharSequence original) {
        if (label == null) return original;
        if (label.startsWith("str:")) return str(label.substring(4));
        if (label.startsWith("res:")) {
            // The app names are the names of the launch aliases, so they are read as the launcher reads
            // them. The translations of the patches may hold another string of the same name.
            final int id = ResourceUtils.getStringIdentifier(label.substring(4));
            return id == 0 ? null : Utils.getContext().getString(id);
        }
        return label;
    }

    private static int indexOf(CharSequence[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].toString().equals(value)) return i;
        }
        return -1;
    }

    private static int indexOf(List<CharSequence> list, String value) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).toString().equals(value)) return i;
        }
        return -1;
    }

    private static boolean contains(List<CharSequence> list, CharSequence value) {
        return indexOf(list, value.toString()) >= 0;
    }

    @Nullable
    private static String selectedValue(String key) {
        Setting<?> setting = Setting.getSettingFromPath(key);
        return setting != null ? String.valueOf(setting.get()) : null;
    }
}
