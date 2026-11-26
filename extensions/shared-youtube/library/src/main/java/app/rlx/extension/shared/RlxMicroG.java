/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.extension.shared;

import android.os.Build;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * RLX: the update button of Morphe's "MicroG is outdated" dialog downloads the MicroG-RE file
 * without launcher icon for the ABI of the device ({@code microg-<version>-noicon-arm64-v8a.apk}),
 * instead of opening the download page, which gives the universal file with icon.
 * Hooked in {@code GmsCoreSupportPatch}; see rlx/README.md.
 */
@SuppressWarnings("unused")
public final class RlxMicroG {
    /** Download link of the file for this device, from the latest release, or null. */
    @Nullable
    private static volatile String updateDownloadUrl;

    private RlxMicroG() {
    }

    /**
     * Called with the latest MicroG-RE release (GitHub API) that Morphe fetched for its update check.
     */
    public static void setLatestRelease(@Nullable JSONObject release) {
        updateDownloadUrl = release == null ? null : findDownload(release.optJSONArray("assets"));
    }

    /**
     * @return The download link of the update for this device, or Morphe's link if the release has no such file.
     */
    public static String updateDownload(String morpheDownload) {
        String url = updateDownloadUrl;
        return url != null ? url : morpheDownload;
    }

    @Nullable
    private static String findDownload(@Nullable JSONArray assets) {
        if (assets == null) return null;
        // The files without icon: one per ABI, then the universal one.
        String[] abis = Build.SUPPORTED_ABIS;
        for (String suffix : abis == null ? new String[0] : abis) {
            String url = findAsset(assets, "-noicon-" + suffix + ".apk");
            if (url != null) return url;
        }
        return findAsset(assets, "-noicon.apk");
    }

    @Nullable
    private static String findAsset(JSONArray assets, String nameSuffix) {
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.optJSONObject(i);
            if (asset == null) continue;
            String name = asset.optString("name");
            String url = asset.optString("browser_download_url");
            if (name.endsWith(nameSuffix) && url.startsWith("https://github.com/")) {
                return url;
            }
        }
        return null;
    }
}
