/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.extension.shared;

import static app.morphe.extension.shared.StringRef.str;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;
import app.morphe.extension.shared.ui.CustomDialog;

/**
 * Applies the RLX default settings (rlx/defaults.json) once per setting.
 * <p>
 * A default is applied the first time a build that has it starts, and only if the setting
 * still has the Morphe default value. A setting the user has changed is never touched, and
 * whatever the user selects afterwards is kept, also across updates. Defaults added to a later
 * build are applied once on the update that adds them.
 * <p>
 * When RLX changes a default, the earlier RLX defaults of the setting are listed in the
 * "replaced" section. A setting that still has one of them gets the new default once.
 */
@SuppressWarnings("unused")
public final class RlxDefaults {

    private static final String PREFERENCES_NAME = "rlx_defaults";
    private static final String APPLIED_KEYS = "applied_keys";
    /**
     * Entries "key=value" of the defaults that were applied or checked against the
     * replaced defaults of the setting, so each new default replaces an earlier one only once.
     */
    private static final String APPLIED_VALUES = "applied_values";

    /**
     * Preferences of earlier builds that applied the same defaults (the YT Morphe and
     * YT Music Morphe builds). Their applied settings are not applied again.
     */
    private static final String[] LEGACY_PREFERENCES_NAMES = {
            "morphe_extras_defaults",
    };

    /**
     * Settings classes that must be loaded, so their settings can be found by key.
     * Classes that are not part of the app are skipped.
     */
    private static final String[] SETTINGS_CLASSES = {
            "app.morphe.extension.shared.settings.BaseSettings",
            "app.morphe.extension.shared.settings.SharedSettings",
            "app.morphe.extension.shared.settings.SharedYouTubeSettings",
            "app.morphe.extension.youtube.settings.Settings",
            "app.morphe.extension.music.settings.Settings",
            // RLX patches.
            "app.rlx.extension.youtube.yandexvot.YandexVotSettings",
            "app.rlx.extension.youtube.savetodevice.SaveToDeviceSettings",
    };

    /**
     * Prefix the Morphe settings export leaves out of the keys.
     */
    private static final String MORPHE_KEY_PREFIX = "morphe_";

    private static final long FIRST_RUN_RESTART_DELAY_MS = 10_000;

    private static boolean done;

    private RlxDefaults() {
    }

    /**
     * @return rlx/defaults.json, Base64 encoded. Replaced while patching.
     */
    private static String defaultsBase64() {
        return "";
    }

    /**
     * @return The section of rlx/defaults.json for this app, "youtube" or "music". Replaced while patching.
     */
    private static String appId() {
        return "";
    }

    /**
     * Injection point. Called when the main activity is created.
     */
    public static void onActivityCreated(Activity activity) {
        if (done) return;
        done = true;

        try {
            loadSettingsClasses();
            RlxWhitelistKeeper.register();
        } catch (Exception ex) {
            Logger.printException(() -> "Could not keep the channel whitelists on import", ex);
        }

        try {
            final boolean firstRun = !SharedYouTubeSettings.SETTINGS_INITIALIZED.get();
            final boolean rebootNeeded = apply(activity, false).rebootNeeded;
            if (!rebootNeeded) return;

            if (firstRun) {
                // Morphe restarts the app once on the first run. Restart anyway if that does not happen.
                Utils.runOnMainThreadDelayed(() -> {
                    if (SharedYouTubeSettings.SETTINGS_INITIALIZED.get()) return;
                    SharedYouTubeSettings.SETTINGS_INITIALIZED.save(true);
                    showRestartDialog(currentActivity(activity), str("morphe_restart_first_run"));
                }, FIRST_RUN_RESTART_DELAY_MS);
            } else {
                Utils.runOnMainThreadDelayed(() -> showRestartDialog(
                        currentActivity(activity), str("rlx_defaults_restart_message")), 1000);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Could not apply the RLX default settings", ex);
        }
    }

    public static final class Changes {
        /**
         * Number of settings whose value changed.
         */
        public int changed;
        /**
         * If a changed setting needs an app restart.
         */
        public boolean rebootNeeded;
    }

    /**
     * Applies every RLX default again, also to settings the user has changed.
     */
    public static Changes applyAll(Context context) throws Exception {
        return apply(context, true);
    }

    /**
     * @param all Apply every default again, also to settings the user has changed.
     */
    private static Changes apply(Context context, boolean all) throws Exception {
        Changes result = new Changes();

        JSONObject json = defaultsJson();
        JSONObject defaults = sectionOfThisApp(json, null);
        if (defaults.length() == 0) return result;
        JSONObject replaced = sectionOfThisApp(json, "replaced");

        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        Set<String> applied = new HashSet<>(preferences.getStringSet(APPLIED_KEYS, new HashSet<>()));
        Set<String> appliedValues = new HashSet<>(preferences.getStringSet(APPLIED_VALUES, new HashSet<>()));
        if (!all) {
            for (String legacyName : LEGACY_PREFERENCES_NAMES) {
                applied.addAll(context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
                        .getStringSet(APPLIED_KEYS, new HashSet<>()));
            }
        }

        loadSettingsClasses();

        boolean appliedChanged = false;
        for (Iterator<String> keys = defaults.keys(); keys.hasNext(); ) {
            String key = keys.next();
            final String value = String.valueOf(defaults.get(key));
            final String appliedValue = key + "=" + value;
            final boolean replacesEarlier = !all && applied.contains(key)
                    && !appliedValues.contains(appliedValue) && replaced.has(key);
            if (!all && applied.contains(key) && !replacesEarlier) continue;

            Setting<?> setting = findSetting(key);
            if (setting == null) {
                // Not part of this app (yet). Applied once a build has it.
                continue;
            }

            final boolean apply = all
                    || (replacesEarlier ? hasValue(setting, replaced.optJSONArray(key)) : setting.isSetToDefault());
            if (apply) {
                try {
                    final Object before = setting.get();
                    setting.saveFromString(value);
                    if (!before.equals(setting.get())) {
                        result.changed++;
                        result.rebootNeeded |= setting.rebootApp;
                    }
                } catch (Exception ex) {
                    Logger.printException(() -> "Invalid RLX default: " + key + " = " + value, ex);
                }
            }

            appliedChanged |= applied.add(key);
            appliedChanged |= appliedValues.add(appliedValue);
        }

        if (appliedChanged) {
            preferences.edit()
                    .putStringSet(APPLIED_KEYS, applied)
                    .putStringSet(APPLIED_VALUES, appliedValues)
                    .apply();
        }

        final int changed = result.changed;
        Logger.printInfo(() -> "Applied " + changed + " RLX default settings");
        return result;
    }

    private static JSONObject defaultsJson() throws Exception {
        final String encoded = defaultsBase64();
        if (encoded.isEmpty()) return new JSONObject();

        return new JSONObject(new String(Base64.decode(encoded, Base64.DEFAULT), StandardCharsets.UTF_8));
    }

    /**
     * @param parent Name of the section that holds the sections of the apps, or null for the defaults.
     */
    private static JSONObject sectionOfThisApp(JSONObject json, @Nullable String parent) {
        JSONObject apps = parent == null ? json : json.optJSONObject(parent);
        JSONObject section = apps != null ? apps.optJSONObject(appId()) : null;
        return section != null ? section : new JSONObject();
    }

    /**
     * @return If the setting has one of the values, compared as in the settings export (enum names in any case).
     */
    private static boolean hasValue(Setting<?> setting, @Nullable JSONArray values) {
        if (values == null) return false;
        final String current = String.valueOf(setting.get());
        for (int i = 0, length = values.length(); i < length; i++) {
            if (current.equalsIgnoreCase(String.valueOf(values.opt(i)))) return true;
        }
        return false;
    }

    private static void loadSettingsClasses() {
        for (String className : SETTINGS_CLASSES) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException ignored) {
                // Not part of this app.
            }
        }
    }

    /**
     * @param key Key of the Morphe settings export, with or without the "morphe_" prefix.
     */
    @Nullable
    private static Setting<?> findSetting(String key) {
        Setting<?> setting = key.startsWith(MORPHE_KEY_PREFIX)
                ? null
                : Setting.getSettingFromPath(MORPHE_KEY_PREFIX + key);
        return setting != null ? setting : Setting.getSettingFromPath(key);
    }

    private static Activity currentActivity(Activity fallback) {
        Activity current = Utils.getActivity();
        return current != null ? current : fallback;
    }

    public static void showRestartDialog(@Nullable Activity activity, String message) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        var dialogPair = CustomDialog.create(
                activity,
                str("morphe_settings_restart_title"),   // Title.
                message,                                 // Message.
                null,                                    // No EditText.
                str("morphe_settings_restart"),          // OK button text.
                () -> Utils.restartApp(activity),        // OK button action.
                null,                                    // No Cancel button.
                null,                                    // No Neutral button text.
                null,                                    // No Neutral button action.
                true                                     // Dismiss dialog when onNeutralClick.
        );
        dialogPair.first.setCancelable(false);
        Utils.showDialog(activity, dialogPair.first);
    }
}
