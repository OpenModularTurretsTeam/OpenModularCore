// OpenModularCore: the plain-Java game rules of OMLib, OMT and OML, shared by every Minecraft version.
// Java 17 modules (omlib-core, omt-core, oml-core) for the modern versions, plus their downgraded Java 8 modules
// (omlib-core-j8, omt-core-j8, oml-core-j8) for 1.12.2. See CLAUDE.md and ../CLAUDE.md ("Shared core logic").
pluginManagement {
    // Convention plugins of the core modules (core-module, core-java8)
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Auto-provisions the Java toolchains (17 for the build, 8 for the Java 8 test run) if they are not installed
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "OpenModularCore"

// Project names are the published artifact ids: a workspace that includes this build (includeBuild) substitutes
// com.github.OpenModularTurretsTeam.OpenModularCore:<name> with the project of that name.
include("omlib-core", "omt-core", "oml-core")
include("omlib-core-j8", "omt-core-j8", "oml-core-j8")
