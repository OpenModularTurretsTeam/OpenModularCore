// omt-core downgraded to Java 8, for 1.12.2 (built from :omt-core, see core-java8)
plugins {
    id("core-java8")
}

dependencies {
    // The Java 8 module of OMLib's core, never the Java 17 one
    api(project(":omlib-core-j8"))
}
