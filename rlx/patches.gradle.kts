// RLX Patches build hook for the patches project, applied from patches/build.gradle.kts.
//
// RLX ships YouTube and YouTube Music only. The patches of other apps stay in the tree
// unchanged, so merges from Morphe never conflict, and are only left out of the build here.

/** Apps whose patches, strings and extension are left out of the bundle. */
val excludedApps = listOf("reddit")

the<SourceSetContainer>().named("main") {
    val kotlinSources = extensions.getByName("kotlin") as SourceDirectorySet
    excludedApps.forEach { app ->
        kotlinSources.exclude("app/morphe/patches/$app/**")
        resources.exclude(
            "addresources/*/$app/**",
            "addresources-json/*/$app.json",
            "extensions/$app.mpe",
        )
    }
}

// The extensions are added to the resources by the Morphe Gradle plugin; the patches file is
// the jar (.mpp), so filter it there too.
tasks.withType<Jar>().configureEach {
    excludedApps.forEach { app -> exclude("extensions/$app.mpe", "$app.mpe") }
}

// The bundle file is named rlx-patches-<version>.mpp. Test builds of the Build workflow set RLX_BUILD to the number
// of the run and are named rlx-patches-<version>-build<number>.mpp; the About entry shows the same version.
// Releases are built without it.
val rlxBuild = providers.environmentVariable("RLX_BUILD").orNull?.trim()?.takeIf { it.isNotEmpty() }
val rlxBuildSuffix = rlxBuild?.let { "build$it" }

tasks.withType<Jar>().configureEach {
    archiveBaseName.set("rlx-patches")
}
if (rlxBuildSuffix != null) {
    tasks.named<Jar>("jar") {
        archiveClassifier.set(rlxBuildSuffix)
    }
}

// Build information the RLX patches read while patching: the RLX version, the Morphe base
// version, and rlx/defaults.json. Bundled as resources under app/rlx/.
val rlxBuildInfo = tasks.register("rlxBuildInfo") {
    description = "Bundles the RLX build information and default settings"

    val rlxVersion = project.version.toString() + (rlxBuildSuffix?.let { "-$it" } ?: "")
    val morpheBaseVersion = project.extra["morpheBaseVersion"].toString()
    val defaults = rootProject.file("rlx/defaults.json")
    val output = layout.buildDirectory.dir("generated/rlx")

    inputs.property("rlxVersion", rlxVersion)
    inputs.property("morpheBaseVersion", morpheBaseVersion)
    inputs.file(defaults)
    outputs.dir(output)

    doLast {
        val dir = output.get().asFile.resolve("app/rlx")
        dir.mkdirs()
        dir.resolve("build.properties").writeText(
            "rlxVersion=$rlxVersion\nmorpheBaseVersion=$morpheBaseVersion\n"
        )
        defaults.copyTo(dir.resolve("defaults.json"), overwrite = true)
    }
}

the<SourceSetContainer>().named("main") {
    resources.srcDir(rlxBuildInfo)
}
