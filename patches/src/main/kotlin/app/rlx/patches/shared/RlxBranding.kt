/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.patches.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourceFile
import app.morphe.patcher.patch.resourceXml

/**
 * The RLX app icons take the place of Morphe icon styles that RLX hides (GPLv3 section 7e, see
 * NOTICE), so no launch alias, setting value or patch option of Morphe changes:
 * label in the patch option "App icon" to Morphe icon style.
 */
internal val rlxIconStyles = linkedMapOf(
    "RLX red" to "play",
    "RLX green" to "play_black",
    "RLX layered" to "light",
)

private val iconFilePrefixes = listOf(
    "morphe_adaptive_background_",
    "morphe_adaptive_foreground_",
    "morphe_adaptive_monochrome_",
    "morphe_notification_icon_",
)

/**
 * RLX header logos of YouTube. Each is an attribute and a drawable with the suffix _light and _dark,
 * like the Morphe header logo; the values of the header logo setting are in the extension
 * (ChangeHeaderPatch.HeaderLogo, an RLX hook).
 */
private val youTubeHeaders = listOf(
    "rlx_header_red_youtube",
    "rlx_header_green_youtube",
    "rlx_header_red_premium",
    "rlx_header_green_premium",
)

/** RLX header logos of YouTube Music (dark theme only, like the Morphe header logo). */
private val musicHeaders = listOf(
    "rlx_header_music_red_dark",
    "rlx_header_music_green_dark",
)

private fun bundled(path: String): ByteArray =
    RlxBuildInfo::class.java.classLoader.getResourceAsStream(path)?.use { it.readBytes() }
        ?: throw PatchException("Missing bundled resource: $path")

private fun writeResource(path: String, bundledPath: String) {
    val file = resourceFile(path, false)
    file.parentFile.mkdirs()
    file.writeBytes(bundled(bundledPath))
}

private fun hasResource(path: String) = resourceFile(path, false).exists()

/**
 * Puts the RLX icons and header logos in the app. Called when all patches have run, so Custom
 * branding and Change header have added their resources; does nothing for a patch that is excluded.
 *
 * @param appId "youtube" (square icons) or "music" (round icons).
 */
internal fun applyRlxBranding(appId: String) {
    if (hasResource("res/drawable/morphe_adaptive_foreground_play.xml")) {
        rlxIconStyles.values.forEach { style ->
            iconFilePrefixes.forEach { prefix ->
                val name = "$prefix$style.xml"
                writeResource("res/drawable/$name", "rlx/branding/$appId/drawable/$name")
            }
        }
    }

    when (appId) {
        "youtube" -> if (hasResource("res/drawable/morphe_header_light.xml")) addYouTubeHeaders()
        "music" -> if (hasResource("res/drawable/morphe_header_dark.xml")) {
            musicHeaders.forEach { writeResource("res/drawable/$it.xml", "rlx/drawable/$it.xml") }
        }
    }
}

private fun addYouTubeHeaders() {
    youTubeHeaders.forEach { logo ->
        listOf("light", "dark").forEach { theme ->
            writeResource("res/drawable/${logo}_$theme.xml", "rlx/drawable/${logo}_$theme.xml")
        }
    }

    // The header logo is an attribute of the theme, as Change header does for the Morphe logo.
    resourceXml("res/values/attrs.xml", copy = false).use { attrs ->
        youTubeHeaders.forEach { logo ->
            attrs.root.appendChild("attr") {
                setAttribute("format", "reference")
                setAttribute("name", logo)
            }
        }
    }

    resourceXml("res/values/styles.xml").use { styles ->
        arrayOf(
            "Base.Theme.YouTube.Light" to "light",
            "Base.Theme.YouTube.Dark" to "dark",
            "CairoLightThemeRingo2Updates" to "light",
            "CairoDarkThemeRingo2Updates" to "dark",
        ).forEach { (style, theme) ->
            val styleElement = styles.resource(style)
            youTubeHeaders.forEach { logo ->
                styleElement.appendChild("item") {
                    setAttribute("name", logo)
                    text = "@drawable/${logo}_$theme"
                }
            }
        }
    }
}
