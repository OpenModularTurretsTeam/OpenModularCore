// Convention plugins of the core repository: core-module (the Java 17 modules) and core-java8 (their Java 8 modules)
plugins {
    `kotlin-dsl`
}

dependencies {
    // JvmDowngrader (approved for the core, see ../../CLAUDE.md): downgrades the jars to Java 8 and shades the API stubs
    implementation("xyz.wagyourtail.jvmdowngrader:xyz.wagyourtail.jvmdowngrader.gradle.plugin:2.0.1")
}
