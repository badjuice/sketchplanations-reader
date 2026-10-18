pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Sketchplanations"
include(":app")

// Content client, built from source: github.com/badjuice/SketchplanationsExplode (git submodule).
includeBuild("explode")
