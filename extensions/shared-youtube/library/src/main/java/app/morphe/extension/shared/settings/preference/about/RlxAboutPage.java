/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.morphe.extension.shared.settings.preference.about;

import static app.morphe.extension.shared.StringRef.str;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.rlx.extension.shared.RlxInfo;

/**
 * The RLX about page, in the style of the Morphe about page: the RLX Patches icon, version and
 * Morphe Patches base version (GPLv3 section 7b), whether a newer RLX release is out, links and
 * credits.
 * <p>
 * RLX code in the package of the Morphe about page, so it can use its page style, which is not public.
 */
@SuppressWarnings("unused")
public final class RlxAboutPage {

    private static final String LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/Ramik4/rlx-patches/releases/latest";
    private static final int CONNECTION_TIMEOUT_MILLIS = 5000;

    private static final String YTINDIR_URL = "https://github.com/Ramik4/ytindir";
    private static final String MORPHE_PATCHES_URL = "https://github.com/MorpheApp/morphe-patches";
    private static final String MORPHE_WEBSITE_URL = "https://morphe.software";

    /**
     * Name, role (string key, or the text itself if it is not a key) and page of each credit.
     */
    private static final String[][] CREDITS = {
            {"Morphe", "Morphe Patches", "https://github.com/MorpheApp"},
            {"ReVanced, ReVanced Extended", "rlx_about_credit_revanced", "https://github.com/ReVanced"},
            {"MicroG RE", "rlx_about_credit_microg", "https://github.com/MorpheApp/MicroG-RE"},
            {"inotia00", "rlx_about_credit_inotia00", "https://github.com/inotia00"},
            {"MarcaDian", "rlx_about_credit_marcadian", "https://github.com/MarcaDian"},
            {"sashade8", "rlx_about_credit_sashade8", "https://github.com/sashade8-ship-it"},
            {"lavinhoque33", "rlx_about_credit_lavinhoque33", "https://github.com/lavinhoque33"},
            {"Ramik4", "rlx_about_credit_ramik4", "https://github.com/Ramik4"},
    };

    /**
     * Latest RLX release, such as "2.4.5", once fetched. Fetched once per app start.
     */
    @Nullable
    private static volatile String latestVersion;
    private static volatile boolean latestVersionFetched;

    private RlxAboutPage() {
    }

    /**
     * Shows the page, after looking up the latest release (only the first time, and only for a few seconds).
     */
    public static void show(Context context) {
        Utils.runOnBackgroundThread(() -> {
            fetchLatestVersion();
            final String html = createHtml(context);
            Utils.runOnMainThreadNowOrLater(() -> {
                if (context instanceof Activity activity
                        && (activity.isFinishing() || activity.isDestroyed())) {
                    return;
                }
                try {
                    new AboutWebViewDialog(context, html, false).show();
                } catch (Exception ex) {
                    Logger.printException(() -> "Could not show the RLX about page", ex);
                }
            });
        });
    }

    private static void fetchLatestVersion() {
        if (latestVersionFetched || !Utils.isNetworkConnected()) return;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE_API_URL).openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
            connection.setReadTimeout(CONNECTION_TIMEOUT_MILLIS);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            try {
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String tag = new JSONObject(read(connection.getInputStream())).getString("tag_name");
                    latestVersion = tag.startsWith("v") ? tag.substring(1) : tag;
                }
                // Any answer counts, so a missing release is not asked for again on every tap.
                latestVersionFetched = true;
            } finally {
                connection.disconnect();
            }
        } catch (Exception ex) {
            Logger.printInfo(() -> "Could not fetch the latest RLX release", ex);
        }
    }

    private static String read(InputStream stream) throws Exception {
        try (stream) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int length;
            while ((length = stream.read(buffer)) != -1) out.write(buffer, 0, length);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    /**
     * @return The release a version belongs to: "2.4.5" for the test build "2.4.5-build12".
     */
    private static String releaseOf(String version) {
        final int build = version.indexOf("-build");
        return build < 0 ? version : version.substring(0, build);
    }

    /**
     * @return Negative, zero or positive as version a is older than, the same as or newer than b.
     */
    static int compareVersions(String a, String b) {
        String[] partsA = a.split("[.-]");
        String[] partsB = b.split("[.-]");
        for (int i = 0, length = Math.max(partsA.length, partsB.length); i < length; i++) {
            final int numberA = i < partsA.length ? parseNumber(partsA[i]) : 0;
            final int numberB = i < partsB.length ? parseNumber(partsB[i]) : 0;
            if (numberA != numberB) return Integer.compare(numberA, numberB);
        }
        return 0;
    }

    private static int parseNumber(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String nonBreaking(String text) {
        return text.replace("-", "&#8209;");
    }

    private static void appendTile(StringBuilder html, String url, String title, String icon, boolean wide) {
        html.append("<a href=\"").append(AboutDialogStyle.escapeHtml(url))
                .append(wide ? "\" class=\"tile wide\">" : "\" class=\"tile\">")
                .append(icon)
                .append("<span class=\"tile-title\">").append(AboutDialogStyle.escapeHtml(title)).append("</span>")
                .append("</a>");
    }

    private static String createHtml(Context context) {
        final String version = RlxInfo.rlxVersion();
        final String release = releaseOf(version);
        final boolean testBuild = !release.equals(version);

        StringBuilder html = new StringBuilder(AboutDialogStyle.documentStart(context))
                .append(AboutDialogStyle.pageStart(true));

        // Icon of RLX Patches (the icon of the source in Morphe Manager), name, and the Morphe base.
        html.append("<div class=\"dialog-header\"><div class=\"about-header\">")
                .append("<div class=\"app-logo\" style=\"padding:0;background:none;overflow:hidden\">")
                .append("<img alt=\"\" style=\"display:block;width:100%;height:100%\" src=\"data:image/png;base64,")
                .append(RlxInfo.iconBase64()).append("\"/></div>")
                .append("<div><div class=\"app-name\">RLX Patches</div><div class=\"app-component\">")
                .append(nonBreaking(str("rlx_about_component",
                        AboutDialogStyle.isolateLtr(RlxInfo.morpheBaseVersion()))))
                .append("</div></div></div>");

        // Version, and whether a newer release is out (nothing while that is not known).
        html.append("<div class=\"version-card\"><div class=\"version-label-row\"><span class=\"version-label\">")
                .append(str("morphe_settings_about_version")).append("</span>");
        if (testBuild) {
            html.append("<span class=\"badge\">").append(str("rlx_about_test_build")).append("</span>");
        }
        html.append("</div><div class=\"version\">")
                .append(nonBreaking(AboutDialogStyle.isolateLtr(version))).append("</div>");

        final String latest = latestVersion;
        if (latest != null) {
            final int comparison = compareVersions(latest, release);
            if (comparison == 0) {
                html.append("<div class=\"version-status\">")
                        .append(str("morphe_settings_about_links_dev_header_up_to_date"))
                        .append("</div>");
            } else if (comparison > 0) {
                html.append("<div class=\"version-status update\"><div class=\"version-status-text\">")
                        .append(nonBreaking(str("morphe_settings_about_update_available",
                                AboutDialogStyle.isolateLtr(latest))))
                        .append("<div class=\"version-hint\">")
                        .append(str("morphe_settings_about_update_hint"))
                        .append("</div></div><a href=\"").append(MorpheAboutPreference.MANAGER_LINK_PLACEHOLDER_URL)
                        .append("\" class=\"update-button\">")
                        .append(str("morphe_patches_update_notice_dialog_update"))
                        .append("</a></div>");
            }
        }
        html.append("</div></div>"); // End of .version-card and .dialog-header.

        // Links.
        html.append("<div class=\"section\"><div class=\"group-label\">")
                .append(str("rlx_about_links")).append("</div><div class=\"tiles\">");
        appendTile(html, RlxInfo.REPOSITORY_URL, "GitHub", AboutDialogStyle.linkIcon(RlxInfo.REPOSITORY_URL), false);
        appendTile(html, YTINDIR_URL, "YTindir", AboutDialogStyle.linkIcon(YTINDIR_URL), false);
        appendTile(html, MORPHE_PATCHES_URL, "Morphe Patches", AboutDialogStyle.linkIcon(MORPHE_PATCHES_URL), false);
        appendTile(html, MORPHE_WEBSITE_URL, "Morphe", AboutDialogStyle.linkIcon(MORPHE_WEBSITE_URL), false);
        appendTile(html, MorpheAboutPreference.CREDITS_LINK_PLACEHOLDER_URL, str("rlx_about_morphe_contributors"),
                AboutDialogStyle.linkIcon(MorpheAboutPreference.CREDITS_LINK_PLACEHOLDER_URL), true);
        html.append("</div></div>");

        // Credits.
        html.append("<div class=\"section\">")
                .append(AboutDialogStyle.sectionTitle(str("rlx_about_credits")))
                .append("<div class=\"settings-group\">");
        for (String[] credit : CREDITS) {
            final String role = credit[1].startsWith("rlx_") ? str(credit[1]) : credit[1];
            html.append("<a href=\"").append(AboutDialogStyle.escapeHtml(credit[2])).append("\" class=\"settings-item\">")
                    .append("<div class=\"item-text\"><div class=\"item-title\">")
                    .append(AboutDialogStyle.escapeHtml(credit[0]))
                    .append("</div><div class=\"item-subtitle\">").append(AboutDialogStyle.escapeHtml(role))
                    .append("</div></div>").append(AboutDialogStyle.chevron()).append("</a>");
        }
        html.append("</div></div>");

        // Not affiliated, and the license.
        html.append("<div class=\"section\"><div class=\"item-subtitle\" style=\"padding:0 4px;line-height:1.5\">")
                .append(AboutDialogStyle.escapeHtml(str("rlx_about_notice")))
                .append("</div></div>");

        html.append(AboutDialogStyle.pageEnd(str("morphe_settings_about_close"), null))
                .append(AboutDialogStyle.DOCUMENT_END);
        return html.toString();
    }
}
