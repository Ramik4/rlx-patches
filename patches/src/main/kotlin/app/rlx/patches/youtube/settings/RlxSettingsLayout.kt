/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.patches.youtube.settings

import app.morphe.patcher.patch.resourceXml
import app.morphe.patcher.xml.XmlElement
import app.morphe.patches.shared.misc.settings.preference.BasePreference
import app.morphe.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.morphe.patches.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import app.morphe.patches.youtube.misc.settings.PreferenceScreen
import app.rlx.patches.shared.rlxLogger
import app.rlx.patches.shared.takeSetting

/**
 * "Downloads" of the RLX settings: the download settings of Morphe (moved here from *Player* and *Feed*), then
 * Save to device, which its patch adds here.
 */
val RLX_DOWNLOADS_SCREEN = PreferenceScreen.Screen(
    key = "morphe_settings_screen_rlx_downloads",
    titleKey = "rlx_downloads_screen_title",
    summaryKey = null,
    icon = "@drawable/rlx_settings_screen_downloads",
    iconBold = "@drawable/rlx_settings_screen_downloads_bold",
    layout = "@layout/preference_with_icon",
    sorting = Sorting.UNSORTED,
)

/** "Swipe and seekbar": the Morphe screens *Swipe controls* and *Seekbar*, each as it is. */
private val SWIPE_AND_SEEKBAR_SCREEN = PreferenceScreen.Screen(
    key = "morphe_settings_screen_rlx_swipe_seekbar",
    titleKey = "rlx_swipe_seekbar_screen_title",
    summaryKey = "rlx_swipe_seekbar_screen_summary",
    icon = "@drawable/rlx_settings_screen_swipe_seekbar",
    iconBold = "@drawable/rlx_settings_screen_swipe_seekbar_bold",
    layout = "@layout/preference_with_icon",
    sorting = Sorting.UNSORTED,
)

/** "Community services": Return YouTube Dislike, SponsorBlock and DeArrow, each as it is. */
private val COMMUNITY_SCREEN = PreferenceScreen.Screen(
    key = "morphe_settings_screen_rlx_community",
    titleKey = "rlx_community_screen_title",
    summaryKey = "rlx_community_screen_summary",
    icon = "@drawable/rlx_settings_screen_community",
    iconBold = "@drawable/rlx_settings_screen_community_bold",
    layout = "@layout/preference_with_icon",
    sorting = Sorting.UNSORTED,
)

/** Key of the screen in *Video* that holds both voice over translations. */
private const val VOICE_OVER_TRANSLATION_KEY = "morphe_vot_screen_rlx"

/**
 * Groups and orders the screens of the RLX settings (YouTube):
 *
 * About, RLX Customizations, General, Feed, Video (with *Voice over translation*: Yandex and Morphe), Player,
 * Swipe and seekbar, Shorts, Downloads, Ads, Community services (RYD, SponsorBlock, DeArrow), Miscellaneous.
 *
 * Keys and settings do not change: a Morphe screen is moved as it is, so it is still found by its key (search,
 * links into the settings). Called with the RLX Customizations right before the settings are written.
 */
internal fun applyRlxSettingsLayout() {
    rootOrder = null
    groupDownloads()
    groupVoiceOverTranslation()

    // Nesting and ordering the screens of the settings root needs the list of root screens, which Morphe keeps
    // private. Without it, the new screens are still there, sorted by key after the Morphe screens.
    val root = rootScreens() ?: return

    nest(root, SWIPE_AND_SEEKBAR_SCREEN, PreferenceScreen.SWIPE_CONTROLS, PreferenceScreen.SEEKBAR)
    nest(
        root,
        COMMUNITY_SCREEN,
        PreferenceScreen.RETURN_YOUTUBE_DISLIKE,
        PreferenceScreen.SPONSORBLOCK,
        PreferenceScreen.DEARROW,
    )

    val order = listOf(
        RLX_CUSTOMIZATIONS_SCREEN,
        PreferenceScreen.GENERAL,
        PreferenceScreen.FEED,
        PreferenceScreen.VIDEO,
        PreferenceScreen.PLAYER,
        SWIPE_AND_SEEKBAR_SCREEN,
        PreferenceScreen.SHORTS,
        RLX_DOWNLOADS_SCREEN,
        PreferenceScreen.ADS,
        COMMUNITY_SCREEN,
    )
    // Screens Morphe adds later go before Miscellaneous, which stays the last.
    val ordered = order.filter { it in root } +
        root.filter { it !in order && it != PreferenceScreen.MISC } +
        listOf(PreferenceScreen.MISC).filter { it in root }
    root.clear()
    root.addAll(ordered)

    // Morphe writes each preference of the settings root at the top of morphe_prefs.xml, so the file has them in
    // reverse; the root is sorted by key at runtime, which hides that. The RLX core puts them in this order once
    // they are written (orderRlxSettingsRoot).
    rootOrder = listOf(ABOUT_KEY) + ordered.mapNotNull { it.key }
}

private const val ABOUT_KEY = "morphe_settings_screen_00_about"

/** Keys of the preferences of the settings root in their RLX order, or null if the layout did not run. */
private var rootOrder: List<String>? = null

/**
 * Puts the preferences of the settings root in the RLX order (About, then the screens of [applyRlxSettingsLayout])
 * and makes the root keep that order instead of sorting by key. Called by the RLX core when the Morphe settings are
 * written; if they are not written yet, Morphe's order (by key) is kept. Preferences that are not in the order, such
 * as those of add-on bundles, stay at the end.
 */
internal fun orderRlxSettingsRoot() {
    val order = rootOrder ?: return
    listOf("morphe_prefs.xml", "morphe_prefs_icons.xml", "morphe_prefs_icons_bold.xml").forEach { file ->
        resourceXml("res/xml/$file").use { xml ->
            val root = xml.elements("PreferenceScreen").first()
            val children = root.children.toList()
            if (children.none { it.attribute("android:key") == ABOUT_KEY }) {
                rlxLogger.warning("RLX settings layout: the settings root of $file is not written yet, Morphe order kept")
                return@use
            }

            fun rank(element: XmlElement): Int {
                val key = element.attribute("android:key").orEmpty()
                val index = order.indexOfFirst { key == it || key.startsWith("${it}_sort_by_") }
                return if (index < 0) order.size else index
            }
            children.sortedBy(::rank).forEach { root.appendCopy(it) }
            children.forEach { it.remove() }
            root.setAttribute("android:key", ROOT_KEY_UNSORTED)
        }
    }
}

private const val ROOT_KEY_UNSORTED = "morphe_settings_root_screen_sort_by_unsorted"

/**
 * Downloads: the settings of Morphe's *Downloads* screen (from *Player*) and *Download post images* (from *Feed*),
 * then what RLX patches added (Save to device), directly on the screen so it keeps its toolbar.
 */
private fun groupDownloads() {
    val morphe = takeSetting(PreferenceScreen.PLAYER, "morphe_external_downloader_screen") as? PreferenceScreenPreference
    val postImages = takeSetting(PreferenceScreen.FEED, "morphe_post_image_download")
    val own = RLX_DOWNLOADS_SCREEN.preferences.toList()

    val preferences = buildList<BasePreference> {
        morphe?.preferences?.takeIf { it.isNotEmpty() }?.let { add(noTitleUnsortedPreferenceCategory(it)) }
        postImages?.let { add(noTitleUnsortedPreferenceCategory(it)) }
        addAll(own)
    }
    if (preferences.isEmpty()) return
    RLX_DOWNLOADS_SCREEN.preferences.clear()
    RLX_DOWNLOADS_SCREEN.addPreferences(*preferences.toTypedArray())
}

/**
 * Video → Voice over translation: Yandex (RLX) and Morphe's translation of the captions, which keeps its key and
 * settings and gets a title that tells the two apart.
 */
private fun groupVoiceOverTranslation() {
    val video = PreferenceScreen.VIDEO
    val yandex = takeSetting(video, "rlx_yandex_vot_screen")
    val morphe = (takeSetting(video, "morphe_vot_screen") as? PreferenceScreenPreference)?.let {
        PreferenceScreenPreference(
            key = it.key,
            titleKey = "rlx_vot_morphe_title",
            summaryKey = it.summaryKey,
            // The key already has the sort type of the screen, which UNSORTED keeps as it is.
            sorting = Sorting.UNSORTED,
            preferences = it.preferences,
        )
    }
    val screens = listOfNotNull(yandex, morphe)
    if (screens.isEmpty()) return

    video.addPreferences(
        PreferenceScreenPreference(
            key = VOICE_OVER_TRANSLATION_KEY,
            titleKey = "rlx_vot_screen_title",
            summaryKey = null,
            sorting = Sorting.UNSORTED,
            preferences = screens.toSet(),
        ),
    )
}

/** Moves the [screens] that have settings from the settings root into [target], in this order. */
private fun nest(
    root: MutableSet<BasePreferenceScreen.Screen>,
    target: BasePreferenceScreen.Screen,
    vararg screens: BasePreferenceScreen.Screen,
) {
    val present = screens.filter { it in root }
    if (present.isEmpty()) return
    root.removeAll(present.toSet())
    target.addPreferences(*present.map { it.transform() }.toTypedArray())
}

/** The screens of the settings root, in the order they are written; null if Morphe changed how it keeps them. */
@Suppress("UNCHECKED_CAST")
private fun rootScreens(): MutableSet<BasePreferenceScreen.Screen>? = try {
    BasePreferenceScreen::class.java.getDeclaredField("root")
        .apply { isAccessible = true }
        .get(PreferenceScreen) as MutableSet<BasePreferenceScreen.Screen>
} catch (e: Exception) {
    rlxLogger.warning("RLX settings layout: the screens of the settings root are not found, Morphe order kept ($e)")
    null
}
