/*
 * RLX Patches
 * https://github.com/RAMiK4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.extension.shared.settings.preference;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import app.rlx.extension.shared.RlxAbout;
import app.rlx.extension.shared.RlxInfo;

/**
 * Shows the RLX version and the Morphe Patches version RLX is based on, and opens the RLX about dialog.
 */
@SuppressWarnings({"unused", "deprecation"})
public class RlxInfoPreference extends Preference {

    {
        setSummary(RlxInfo.aboutSummary());
        setOnPreferenceClickListener(pref -> {
            RlxAbout.showDialog(pref.getContext());
            return true;
        });
    }

    public RlxInfoPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public RlxInfoPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RlxInfoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RlxInfoPreference(Context context) {
        super(context);
    }
}
