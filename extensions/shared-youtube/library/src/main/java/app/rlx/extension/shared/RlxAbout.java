/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.extension.shared;

import android.app.Activity;
import android.content.Context;
import android.preference.Preference;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.settings.preference.about.RlxAboutPage;

/**
 * The "About" entry of the settings: shows the RLX version and the Morphe Patches
 * version RLX is based on (GPLv3 section 7b attribution), and opens the RLX about page.
 */
@SuppressWarnings({"unused", "deprecation"})
public final class RlxAbout {

    private RlxAbout() {
    }

    /**
     * Injection point. Called at the end of each constructor of the Morphe about preference.
     */
    public static void onCreated(Preference preference) {
        try {
            preference.setSummary(RlxInfo.aboutSummary());
            preference.setOnPreferenceClickListener(pref -> {
                showDialog(pref.getContext());
                return true;
            });
        } catch (Exception ex) {
            Logger.printException(() -> "Could not set up the about preference", ex);
        }
    }

    public static void showDialog(Context context) {
        try {
            if (context instanceof Activity activity
                    && (activity.isFinishing() || activity.isDestroyed())) {
                return;
            }
            RlxAboutPage.show(context);
        } catch (Exception ex) {
            Logger.printException(() -> "Could not show the about page", ex);
        }
    }
}
