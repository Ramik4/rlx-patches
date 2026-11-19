/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.morphe.extension.shared.settings.preference.about;

import static app.morphe.extension.shared.StringRef.str;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.rlx.extension.shared.RlxInfo;

/**
 * The RLX about page, in the style of the Morphe about page: the RLX Patches icon, version and
 * Morphe Patches base version (GPLv3 section 7b), whether a newer RLX release is out, links and
 * credits. A tap on the version shows the notes of the latest release.
 * <p>
 * The page is HTML written here and shown in a WebView with Morphe's page style
 * ({@link AboutStylesheet}); only the latest release is fetched (GitHub API).
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
    // Dummy url. Tapping the version shows the release notes rather than a page.
    private static final String NOTES_LINK_PLACEHOLDER_URL = "https://rlx.patches/release-notes/";

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
    /**
     * Notes of the latest release (Markdown) and when it was published (yyyy-mm-dd), once fetched.
     */
    @Nullable
    private static volatile String latestNotes;
    @Nullable
    private static volatile String latestDate;
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
                    new RlxDialog(context, html).show();
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
                    JSONObject release = new JSONObject(read(connection.getInputStream()));
                    String tag = release.getString("tag_name");
                    latestNotes = release.optString("body", "").trim();
                    final String published = release.optString("published_at", "");
                    latestDate = published.length() >= 10 ? published.substring(0, 10) : null;
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

        // Icon and name of RLX Patches (the icon of the source in Morphe Manager).
        html.append("<div class=\"dialog-header\"><div class=\"about-header\">")
                .append("<div class=\"app-logo\" style=\"padding:0;background:none;overflow:hidden\">")
                .append("<img alt=\"\" style=\"display:block;width:100%;height:100%\" src=\"data:image/png;base64,")
                .append(RlxInfo.iconBase64()).append("\"/></div>")
                .append("<div class=\"app-name\">RLX Patches</div></div>");

        // Version, the Morphe base, and whether a newer release is out (nothing while that is not known).
        // A tap shows the notes of the latest release, if there are any.
        final String latest = latestVersion;
        final String notes = latestNotes;
        final boolean hasNotes = latest != null && notes != null && !notes.isEmpty();
        html.append("<div class=\"version-card\"");
        if (hasNotes) {
            html.append(" style=\"cursor:pointer\" onclick=\"location.href='")
                    .append(NOTES_LINK_PLACEHOLDER_URL).append("'\"");
        }
        html.append("><div class=\"version-label-row\"><span class=\"version-label\">")
                .append(str("morphe_settings_about_version")).append("</span>");
        if (testBuild) {
            html.append("<span class=\"badge\">").append(str("rlx_about_test_build")).append("</span>");
        } else if (hasNotes) {
            html.append(AboutDialogStyle.chevron());
        }
        html.append("</div><div class=\"version\" style=\"font-size:20px;line-height:26px;margin-top:4px\">")
                .append(nonBreaking(AboutDialogStyle.isolateLtr(version))).append("</div>")
                .append("<div class=\"version-hint\" style=\"margin-top:2px\">")
                .append(nonBreaking(str("rlx_about_component",
                        AboutDialogStyle.isolateLtr(RlxInfo.morpheBaseVersion()))))
                .append("</div>");

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
                        // The card opens the notes; the button only opens Morphe Manager.
                        .append("\" onclick=\"event.stopPropagation()\" class=\"update-button\">")
                        .append(str("morphe_patches_update_notice_dialog_update"))
                        .append("</a></div>");
            }
        }
        html.append("</div></div>"); // End of .version-card and .dialog-header.

        // Links.
        html.append("<div class=\"section\"><div class=\"group-label\">")
                .append(str("rlx_about_links")).append("</div><div class=\"tiles\">");
        appendTile(html, RlxInfo.REPOSITORY_URL, "RLX Patches", AboutDialogStyle.linkIcon(RlxInfo.REPOSITORY_URL), false);
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

    /**
     * @return The page of the notes of the latest release.
     */
    private static String createNotesHtml(Context context) {
        StringBuilder html = new StringBuilder(AboutDialogStyle.documentStart(context))
                .append(AboutDialogStyle.pageStart(false))
                .append("<div class=\"dialog-header\"><div class=\"dialog-title\">RLX Patches ")
                .append(nonBreaking(AboutDialogStyle.isolateLtr(String.valueOf(latestVersion))))
                .append("</div>");
        final String date = latestDate;
        if (date != null) {
            html.append("<div class=\"dialog-subtitle\">").append(AboutDialogStyle.isolateLtr(date)).append("</div>");
        }
        html.append("</div><div class=\"section license-body\" style=\"padding-top:0;line-height:1.5\">")
                .append(markdownToHtml(String.valueOf(latestNotes)))
                .append("</div>")
                .append(AboutDialogStyle.pageEnd(str("morphe_settings_about_close"), null))
                .append(AboutDialogStyle.DOCUMENT_END);
        return html.toString();
    }

    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]+)\\]\\((https?://[^)\\s]+)\\)");
    private static final Pattern BARE_URL = Pattern.compile("(?<![\"'>=])(https?://[^\\s<]+)");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern CODE = Pattern.compile("`([^`]+)`");

    /**
     * The Markdown of release notes as HTML: headings, lists, paragraphs, links, bold and code.
     */
    static String markdownToHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        boolean inList = false;
        StringBuilder paragraph = new StringBuilder();
        for (String raw : markdown.replace("\r", "").split("\n")) {
            final String line = raw.trim();
            final boolean item = line.startsWith("- ") || line.startsWith("* ");
            // An indented line goes on with the list item before it.
            if (inList && !item && !line.isEmpty() && Character.isWhitespace(raw.charAt(0))) {
                html.insert(html.length() - "</li>".length(), " " + inline(line));
                continue;
            }
            if (line.isEmpty() || item || line.startsWith("#")) {
                if (paragraph.length() > 0) {
                    html.append("<p>").append(paragraph).append("</p>");
                    paragraph.setLength(0);
                }
                if (!item && inList) {
                    html.append("</ul>");
                    inList = false;
                }
            }
            if (line.isEmpty()) continue;
            if (line.startsWith("#")) {
                html.append("<h2>").append(inline(line.replaceFirst("^#+\\s*", ""))).append("</h2>");
            } else if (item) {
                if (!inList) {
                    html.append("<ul style=\"padding-inline-start:20px;margin:8px 0\">");
                    inList = true;
                }
                html.append("<li style=\"margin:4px 0\">").append(inline(line.substring(2))).append("</li>");
            } else {
                if (paragraph.length() > 0) paragraph.append(' ');
                paragraph.append(inline(line));
            }
        }
        if (paragraph.length() > 0) html.append("<p>").append(paragraph).append("</p>");
        if (inList) html.append("</ul>");
        return html.toString();
    }

    private static String inline(String text) {
        String html = AboutDialogStyle.escapeHtml(text);
        html = CODE.matcher(html).replaceAll("<code>$1</code>");
        html = BOLD.matcher(html).replaceAll("<b>$1</b>");
        Matcher link = MARKDOWN_LINK.matcher(html);
        StringBuffer out = new StringBuffer();
        while (link.find()) {
            link.appendReplacement(out, Matcher.quoteReplacement(
                    "<a href=\"" + link.group(2) + "\">" + link.group(1) + "</a>"));
        }
        link.appendTail(out);
        return BARE_URL.matcher(out.toString()).replaceAll("<a href=\"$1\">$1</a>");
    }

    /**
     * The about page, which also opens the release notes over itself.
     */
    private static final class RlxDialog extends AboutWebViewDialog {
        RlxDialog(Context context, String html) {
            super(context, html, false);
        }

        @Override
        protected WebViewClient createWebViewClient() {
            final Dialog dialog = this;
            return new AboutLinksWebClient(getContext(), dialog) {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    if (NOTES_LINK_PLACEHOLDER_URL.equals(request.getUrl().toString())) {
                        try {
                            new AboutWebViewDialog(dialog.getContext(), createNotesHtml(dialog.getContext()), false).show();
                        } catch (Exception ex) {
                            Logger.printException(() -> "Could not show the release notes", ex);
                        }
                        return true;
                    }
                    return super.shouldOverrideUrlLoading(view, request);
                }
            };
        }
    }
}
