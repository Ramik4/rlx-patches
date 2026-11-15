/*
 * RLX Patches
 * https://github.com/RAMiK4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.extension.shared;

import static app.morphe.extension.shared.StringRef.str;

/**
 * Versions of the RLX Patches this app was patched with.
 */
@SuppressWarnings("unused")
public final class RlxInfo {

    public static final String REPOSITORY_URL = "https://github.com/RAMiK4/rlx-patches";

    private RlxInfo() {
    }

    /**
     * @return The RLX Patches version, such as "2.3.7". Replaced while patching.
     */
    public static String rlxVersion() {
        return "";
    }

    /**
     * @return The Morphe Patches version RLX is based on, such as "2.3.0-dev.7". Replaced while patching.
     */
    public static String morpheBaseVersion() {
        return "";
    }

    /**
     * @return "RLX Patches 2.3.7 · Based on Morphe Patches 2.3.0-dev.7", localized.
     */
    public static String aboutSummary() {
        return str("rlx_about_summary", rlxVersion(), morpheBaseVersion());
    }
}
