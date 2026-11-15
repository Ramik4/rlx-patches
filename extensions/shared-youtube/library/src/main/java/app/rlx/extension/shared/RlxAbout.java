/*
 * RLX Patches
 * https://github.com/RAMiK4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.extension.shared;

import static app.morphe.extension.shared.StringRef.str;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.preference.Preference;
import android.util.Pair;
import android.widget.LinearLayout;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.ui.CustomDialog;

/**
 * The "About" entry of the settings: shows the RLX version and the Morphe Patches
 * version RLX is based on (GPLv3 section 7b attribution), and opens the RLX about dialog.
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

            Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                    context,
                    str("rlx_about_title"),                                   // Title.
                    str("rlx_about_message",
                            RlxInfo.rlxVersion(), RlxInfo.morpheBaseVersion()), // Message.
                    null,                                                     // No EditText.
                    null,                                                     // OK button text (default).
                    () -> {},                                                 // OK button action.
                    null,                                                     // No Cancel button.
                    str("rlx_about_github"),                                  // Neutral button text.
                    () -> Utils.openLink(RlxInfo.REPOSITORY_URL),             // Neutral button action.
                    true                                                      // Dismiss on neutral click.
            );
            dialogPair.first.show();
        } catch (Exception ex) {
            Logger.printException(() -> "Could not show the about dialog", ex);
        }
    }
}
