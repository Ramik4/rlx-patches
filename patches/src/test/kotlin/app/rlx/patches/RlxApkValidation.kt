/*
 * Copyright 2026 RLX Patches.
 * https://github.com/Ramik4/rlx-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.rlx.patches

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.apk.ApkUtils.applyTo
import app.morphe.patcher.dex.SdkDexVerifier
import app.morphe.patcher.patch.PatchLoader
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Patches a stock APK with the patches of the RLX bundle that Morphe Manager selects by default (as the yt-morphe
 * builds do), with fingerprint validation (as `--validate` of the Morphe CLI) and SDK DEX verification.
 *
 * Usage: RlxApkValidationKt <patches.mpp> <stock-apk> [extra patch names, comma separated] [unsigned-output]
 *
 * With the system property `rlxOnly=true`, only the extra patches (and their dependencies) are applied.
 */
fun main(arguments: Array<String>) {
    require(arguments.size in 2..4) {
        "Usage: RlxApkValidationKt <patches.mpp> <stock-apk> [extra patch names] [unsigned-output]"
    }
    val bundle = File(arguments[0]).canonicalFile
    val input = File(arguments[1]).canonicalFile
    require(bundle.isFile) { "Patches file does not exist: $bundle" }
    require(input.isFile) { "Stock APK does not exist: $input" }
    val extra = arguments.getOrNull(2).orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val sdk = System.getenv("ANDROID_HOME") ?: error("ANDROID_HOME is required for SDK verification")

    val packageName = "com.google.android.youtube"
    val only = System.getProperty("rlxOnly", "false").toBooleanStrict()
    val patches = PatchLoader.Jar(setOf(bundle))
    val selected = patches.filter { patch ->
        // Universal patches have no compatibility, as in the patches list Morphe Manager reads.
        val compatibility = patch.compatibility
        val compatible = compatibility.isNullOrEmpty() || compatibility.any { it.packageName == packageName }
        compatible && (if (only) patch.name in extra else patch.default || patch.name in extra)
    }.toSet()
    val missing = extra - selected.mapNotNull { it.name }.toSet()
    require(missing.isEmpty()) { "Patches not found: $missing" }
    println("Selected ${selected.size} patches")

    val workspace = createTempDirectory("rlx-validation").toFile()
    var failed = 0
    Patcher(PatcherConfig(input, workspace, verifier = SdkDexVerifier(File(sdk)), validate = true)).use { patcher ->
        patcher += selected
        runBlocking {
            patcher().collect { result ->
                val exception = result.exception
                if (exception != null) {
                    failed++
                    println("FAILED: ${result.patch.name}: ${exception.message}")
                    exception.printStackTrace()
                } else {
                    println("Applied: ${result.patch.name}")
                }
            }
        }
        if (failed == 0) {
            val result = patcher.get()
            arguments.getOrNull(3)?.let { result.applyTo(File(it)) }
        }
    }
    check(failed == 0) { "$failed patches failed" }
    println("All patches applied with fingerprint validation and SDK DEX verification: $workspace")
}
