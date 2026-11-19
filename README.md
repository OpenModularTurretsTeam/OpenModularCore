# OpenModularCore

The game rules of OMLib, Open Modular Turrets and Open Modular Lighting as plain Java, without Minecraft or a mod loader.
Every Minecraft version of the mods builds against the same released version of this repository, so the rules are the same on all of them.

## Modules

| Module | Package | Java | Used by |
|---|---|---|---|
| `omlib-core` | `omtteam.omlib.core` | 17 | OMLib on 1.20.1, 1.21.1, 1.21.11 |
| `omt-core` | `omtteam.openmodularturrets.core` | 17 | OMT on 1.20.1, 1.21.1, 1.21.11 |
| `oml-core` | `omtteam.openmodularlighting.core` | 17 | OML on 1.20.1, 1.21.1, 1.21.11 |
| `omlib-core-j8` | `omtteam.omlib.core` | 8 | OMLib on 1.12.2 |
| `omt-core-j8` | `omtteam.openmodularturrets.core` | 8 | OMT on 1.12.2 |
| `oml-core-j8` | `omtteam.openmodularlighting.core` | 8 | OML on 1.12.2 |

`omt-core` depends on `omlib-core`, and `omt-core-j8` on `omlib-core-j8`.
`CamoPermissions` is in `omlib-core` (`omtteam.omlib.core.camo`), so that OMPD on 1.12.2 can use it without `omt-core-j8`.
`MachineMode` and `TargetingSettings` are in `omlib-core` (`omtteam.omlib.core.machine`), because OMLib's 1.12.2 game layer uses them.
The memory card data and its trust rules (`MemoryCardData`, `MemoryCardRules`) are in `omlib-core` (`omtteam.omlib.core.card`), because the released OMT 4.0.1 for 1.12.2 calls them in OMLib; the base part of a card is `BaseCardRules` in `omt-core` (`omtteam.openmodularturrets.core.card`).
`oml-core` uses no other core.

The source is written for Java 17.
Public signatures use only Java 8 types and the core's own types; randomness comes in as a `java.util.function.LongSupplier` (the caller's `nextLong`).
The `-j8` modules are the same classes, downgraded to Java 8 bytecode with [JvmDowngrader](https://github.com/unimined/JvmDowngrader).
Each one bundles the stubs of the Java 9+ API it uses under its own prefix (`omtteam/omlib/jvmdg/`, `omtteam/openmodularturrets/jvmdg/`, `omtteam/openmodularlighting/jvmdg/`), so the three 1.12 mod jars never carry the same class.

## Using it

The modules are served by [JitPack](https://jitpack.io/#OpenModularTurretsTeam/OpenModularCore) from the git tags:

```kotlin
repositories {
    maven("https://jitpack.io") {
        content { includeGroup("com.github.OpenModularTurretsTeam.OpenModularCore") }
    }
}

dependencies {
    implementation("com.github.OpenModularTurretsTeam.OpenModularCore:omt-core:${property("core_version")}")
}
```

`core_version` is the tag, e.g. `2.0.0`.
Tags follow semantic versioning: a public class leaving a module or a changed public signature raises the major version.
1.0.0 was the first release.
2.0.0 (next) moves `MemoryCardData` and the trust part of `MemoryCardRules` (`canCopy`, `canPaste`, `captureTrust`, `applyTrust`) from `omt-core` to `omlib-core` (`omtteam.omlib.core.card`).
`MemoryCardRules.capture` and `apply` become `BaseCardRules` in `omt-core` (`omtteam.openmodularturrets.core.card`).
It also brings back `MachineMode.byOrdinal(int, MachineMode)`.

A workspace that has this repository checked out next to it (`includeBuild("../OpenModularCore")`) builds against the local source instead.

## Building

```
./gradlew check                    # both test runs, isolation, architecture rules, coverage, Java 8 jar and API checks
./gradlew publishToMavenLocal      # the six modules into ~/.m2
./gradlew :omt-core:pitest         # mutation testing of one module, on demand
```

`check` runs the unit tests twice: `test` on Java 17 against the normal classes, and `testJava8` on a Java 8 JVM against the downgraded classes (the tests are downgraded too).
It also checks the Java 8 jars: Java 8 bytecode only, the stubs only under the mod's prefix (`verifyJava8Jar`), and no stub type in a public signature (`verifyJava8Api`).
Gradle provisions the Java 17 and Java 8 toolchains itself if they are not installed.

## License

The code comes from OMLib, Open Modular Turrets and Open Modular Lighting, whose code is MIT-licensed (see their `LICENSE`, `LICENSE-DUAL` and `CLA.md`).
This repository is MIT-licensed as well, with OpenModularTurretsTeam as the copyright holder (see `LICENSE`).
