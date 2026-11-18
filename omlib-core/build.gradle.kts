// OMLib's core: trust lists, access levels and profiles, energy buffer, camo light and permissions, world (faces,
// positions, flood fill)
plugins {
    id("core-module")
}

dependencies {
    // equals/hashCode contracts of the core value records (EqualsContractTest)
    "testLibraries"("nl.jqno.equalsverifier:equalsverifier:${property("equalsverifier_version")}")
}
