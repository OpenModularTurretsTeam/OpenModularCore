// OMT's core: turret base, turrets, targeting, security, items, settings, storage, generator, memory card, world
plugins {
    id("core-module")
}

dependencies {
    // OMT's core builds on OMLib's (security, world, camo); OMT compiles against it through OMLib
    api(project(":omlib-core"))
    // equals/hashCode contracts of the core value records and of AmmoType (EqualsContractTest)
    "testLibraries"("nl.jqno.equalsverifier:equalsverifier:${property("equalsverifier_version")}")
}
