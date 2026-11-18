// Build logic of a Java 8 core module (omlib-core-j8, omt-core-j8, oml-core-j8) for 1.12.2: it publishes the Java 8
// jar of the core module of the same name without "-j8" (built there by downgradeJar and shadeDowngradedApi, see
// core-module.gradle.kts), plus that module's sources.
//
// It is a module of its own, not a classifier of the Java 17 module, because a workspace that includes this build
// (includeBuild) substitutes whole modules by group and project name. Its variants carry Java 8 as their target JVM,
// so a Java 8 consumer (1.12 with RetroFuturaGradle) can resolve them, and its dependencies are the other -j8
// modules (omt-core-j8 -> omlib-core-j8, never omlib-core).
plugins {
    base
    `maven-publish`
}

val coreProject = ":" + project.name.removeSuffix("-j8")
val java8Version = (property("java8_version") as String).toInt()

/** Gives the script the factory for a component of its own (the java component would publish Java 17 variants). */
abstract class ComponentFactoryHolder @Inject constructor(val factory: SoftwareComponentFactory)
val java8Component = objects.newInstance<ComponentFactoryHolder>().factory.adhoc("java8")
components.add(java8Component)

// ---- What the Java 17 core module provides: its Java 8 jar and its sources ----
val core = configurations.dependencyScope("core")
dependencies {
    "core"(project(coreProject))
}
val coreJava8Jar = configurations.resolvable("coreJava8Jar") {
    extendsFrom(core.get())
    isTransitive = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("omtteam-core-java8-jar"))
    }
}
val coreSources = configurations.resolvable("coreSources") {
    extendsFrom(core.get())
    isTransitive = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
}

val jarName = "${project.name}-${project.version}.jar"
val sourcesJarName = "${project.name}-${project.version}-sources.jar"
val java8Jar = tasks.register<Copy>("java8Jar") {
    description = "Copies the Java 8 jar of $coreProject"
    from(coreJava8Jar)
    into(layout.buildDirectory.dir("libs"))
    rename { jarName }
}
val java8SourcesJar = tasks.register<Copy>("java8SourcesJar") {
    description = "Copies the sources jar of $coreProject"
    from(coreSources)
    into(layout.buildDirectory.dir("libs/sources"))
    rename { sourcesJarName }
}
tasks.assemble {
    dependsOn(java8Jar, java8SourcesJar)
}

// ---- The module: Java 8 api and runtime variants, and the sources ----
// A -j8 module declares its dependencies with api(project(":<other>-j8")).
val api = configurations.dependencyScope("api")

fun AttributeContainer.java8Library(usage: String) {
    attribute(Usage.USAGE_ATTRIBUTE, objects.named(usage))
    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment.STANDARD_JVM))
    attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, java8Version)
}

val apiElements = configurations.consumable("apiElements") {
    extendsFrom(api.get())
    attributes { java8Library(Usage.JAVA_API) }
    outgoing.artifact(java8Jar.map { it.destinationDir.resolve(jarName) }) { builtBy(java8Jar) }
}
val runtimeElements = configurations.consumable("runtimeElements") {
    extendsFrom(api.get())
    attributes { java8Library(Usage.JAVA_RUNTIME) }
    outgoing.artifact(java8Jar.map { it.destinationDir.resolve(jarName) }) { builtBy(java8Jar) }
}
val sourcesElements = configurations.consumable("sourcesElements") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
    outgoing.artifact(java8SourcesJar.map { it.destinationDir.resolve(sourcesJarName) }) {
        builtBy(java8SourcesJar)
        classifier = "sources"
    }
}

java8Component.addVariantsFromConfiguration(apiElements.get()) { mapToMavenScope("compile") }
java8Component.addVariantsFromConfiguration(runtimeElements.get()) { mapToMavenScope("runtime") }
java8Component.addVariantsFromConfiguration(sourcesElements.get()) {
    mapToMavenScope("runtime")
    mapToOptional()
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(java8Component)
        }
    }
}
