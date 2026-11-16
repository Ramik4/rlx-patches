/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.extension.shared.settings.preference;

import static app.morphe.extension.shared.StringRef.str;

import android.app.Activity;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.ui.CustomDialog;
import app.rlx.extension.shared.RlxDefaults;

/**
 * Applies all RLX default settings again, after a confirmation.
 */
@SuppressWarnings({"unused", "deprecation"})
public class RlxResetDefaultsPreference extends Preference {

    {
        setOnPreferenceClickListener(pref -> {
            confirm(pref.getContext());
            return true;
        });
    }

    public RlxResetDefaultsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public RlxResetDefaultsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RlxResetDefaultsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RlxResetDefaultsPreference(Context context) {
        super(context);
    }

    private static void confirm(Context context) {
        CustomDialog.create(
                context,
                str("rlx_reset_defaults_title"),          // Title.
                str("rlx_reset_defaults_confirm"),        // Message.
                null,                                     // No EditText.
                null,                                     // OK button text (default).
                () -> resetDefaults(context),             // OK button action.
                () -> {},                                 // Cancel button action.
                null,                                     // No Neutral button text.
                null,                                     // No Neutral button action.
                true                                      // Dismiss dialog when onNeutralClick.
        ).first.show();
    }

    private static void resetDefaults(Context context) {
        try {
            final RlxDefaults.Changes changes = RlxDefaults.applyAll(context);
            Utils.showToastShort(str("rlx_reset_defaults_done", changes.changed));
            // Restart so every screen shows the new values.
            if (changes.changed > 0 && context instanceof Activity activity) {
                RlxDefaults.showRestartDialog(activity, str("rlx_defaults_restart_message"));
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Could not reset the RLX default settings", ex);
        }
    }
}
