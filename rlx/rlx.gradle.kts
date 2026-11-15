// RLX Patches build hook, applied from the root build.gradle.kts.
//
// gradle.properties keeps Morphe's own `version` untouched, so merges from Morphe never
// conflict there. That value is kept as the Morphe base version, and every project gets the
// RLX version from rlx/rlx.properties instead.

import java.util.Properties

val rlxProperties = Properties().apply {
    rootProject.file("rlx/rlx.properties").reader().use { load(it) }
}
val rlxVersion: String = rlxProperties.getProperty("version")
    ?: throw GradleException("rlx/rlx.properties has no version")
val morpheBaseVersion: String = rootProject.version.toString()

allprojects {
    extra["morpheBaseVersion"] = morpheBaseVersion
    version = rlxVersion
}

tasks.register("printVersions") {
    description = "Prints the RLX version and the Morphe version it is based on"
    group = "help"
    doLast {
        println("rlxVersion=$rlxVersion")
        println("morpheBaseVersion=$morpheBaseVersion")
    }
}
