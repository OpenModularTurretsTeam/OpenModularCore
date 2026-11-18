// OML's core: beams, clusters, formulas, light casting, math, light state. It does not use OMLib's core.
plugins {
    id("core-module")
}

dependencies {
    // equals/hashCode contracts of the core value records (EqualsContractTest)
    "testLibraries"("nl.jqno.equalsverifier:equalsverifier:${property("equalsverifier_version")}")
}
