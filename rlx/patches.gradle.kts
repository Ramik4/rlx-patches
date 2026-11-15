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
