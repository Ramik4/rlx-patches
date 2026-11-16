/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.extension.shared;

import android.preference.ListPreference;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.settings.Setting;

/**
 * Hides the Morphe branding from the lists of the settings: the Morphe app icons and
 * notification icons, the Morphe header logo and the Morphe splash animations
 * (GPLv3 section 7e, see NOTICE). The original YouTube options stay.
 * <p>
 * A hidden value that is still selected stays in the list, so the selection is shown.
 */
@SuppressWarnings({"unused", "deprecation"})
public final class RlxBranding {

    private static final Map<String, Set<String>> HIDDEN_VALUES = Map.of(
            "morphe_custom_branding_icon",
            Set.of("BLACK", "DARK", "LIGHT", "PLAY", "PLAY_BLACK"),
            "morphe_custom_branding_notification_icon",
            Set.of("BLACK", "PLAY"),
            "morphe_header_logo",
            Set.of("MORPHE"),
            "morphe_splash_screen_style",
            Set.of("MORPHE_COLOR", "MORPHE_MONOCHROME", "RIVE_GLOW_MORPHE", "RIVE_GLOW_MORPHE_2")
    );

    private RlxBranding() {
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
            List<CharSequence> keptEntries = new ArrayList<>(entries.length);
            List<CharSequence> keptValues = new ArrayList<>(values.length);
            for (int i = 0; i < values.length; i++) {
                String value = values[i].toString();
                if (hidden.contains(value) && !value.equalsIgnoreCase(selected)) continue;
                keptEntries.add(entries[i]);
                keptValues.add(values[i]);
            }
            if (keptValues.size() == values.length) return;

            preference.setEntries(keptEntries.toArray(new CharSequence[0]));
            preference.setEntryValues(keptValues.toArray(new CharSequence[0]));
        } catch (Exception ex) {
            Logger.printException(() -> "Could not hide the Morphe branding options", ex);
        }
    }

    @Nullable
    private static String selectedValue(String key) {
        Setting<?> setting = Setting.getSettingFromPath(key);
        return setting != null ? String.valueOf(setting.get()) : null;
    }
}
