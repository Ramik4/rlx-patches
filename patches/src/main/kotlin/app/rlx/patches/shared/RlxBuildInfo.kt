/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.patches.shared

import java.util.Properties

/**
 * Build information bundled by rlx/patches.gradle.kts under app/rlx/.
 */
internal object RlxBuildInfo {
    private const val DIRECTORY = "app/rlx"

    private fun resource(name: String): ByteArray =
        RlxBuildInfo::class.java.classLoader.getResourceAsStream("$DIRECTORY/$name")
            ?.use { it.readBytes() }
            ?: throw IllegalStateException("Missing bundled resource: $DIRECTORY/$name")

    private val properties by lazy {
        Properties().apply { resource("build.properties").inputStream().use { load(it) } }
    }

    /** RLX Patches version, such as "2.3.7". */
    val rlxVersion: String get() = properties.getProperty("rlxVersion")

    /** Morphe Patches version RLX is based on, such as "2.3.0-dev.7". */
    val morpheBaseVersion: String get() = properties.getProperty("morpheBaseVersion")

    /** Content of rlx/defaults.json. */
    val defaultsJson: ByteArray get() = resource("defaults.json")
}
