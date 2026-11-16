/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.extension.shared;

import android.app.Activity;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.settings.Setting;

/**
 * Keeps the channel whitelists (ads, playback speed, SponsorBlock) when settings are imported.
 * <p>
 * A Morphe settings import resets every setting the imported text does not contain. Preset
 * settings files do not contain the whitelists, so importing one would clear the channels the
 * user whitelisted. The import dialog exports the current settings right before every import,
 * so the whitelists are remembered on export and restored after an import that left them empty.
 * Whitelists the imported text does contain are imported as they are.
 */
final class RlxWhitelistKeeper implements Setting.ImportExportCallback {

    /**
     * Whitelists of YouTube. Settings that are not part of the app are skipped.
     */
    private static final String[] WHITELIST_KEYS = {
            "morphe_ads_channel_whitelist",
            "morphe_playback_speed_channel_whitelist",
            "morphe_sb_channel_whitelist",
    };

    private static boolean registered;

    private final Map<String, String> saved = new HashMap<>();

    private RlxWhitelistKeeper() {
    }

    /**
     * Called once the settings classes are loaded, so the whitelists can be found by key.
     */
    static synchronized void register() {
        if (registered) return;
        registered = true;

        RlxWhitelistKeeper keeper = new RlxWhitelistKeeper();
        keeper.remember();
        Setting.addImportExportCallback(keeper);
    }

    @Nullable
    private static Setting<?> whitelist(String key) {
        Setting<?> setting = Setting.getSettingFromPath(key);
        return setting != null && setting.defaultValue instanceof String ? setting : null;
    }

    private synchronized void remember() {
        saved.clear();
        for (String key : WHITELIST_KEYS) {
            Setting<?> setting = whitelist(key);
            if (setting == null) continue;
            String value = (String) setting.get();
            if (!value.isEmpty()) saved.put(key, value);
        }
    }

    @Override
    public void settingsExported(@Nullable Activity context) {
        remember();
    }

    @Override
    public synchronized void settingsImported(@Nullable Activity context) {
        for (Map.Entry<String, String> entry : saved.entrySet()) {
            Setting<?> setting = whitelist(entry.getKey());
            if (setting == null || !((String) setting.get()).isEmpty()) continue;

            //noinspection unchecked
            ((Setting<String>) setting).save(entry.getValue());
            Logger.printDebug(() -> "Kept channel whitelist on import: " + entry.getKey());
        }
        remember();
    }
}
