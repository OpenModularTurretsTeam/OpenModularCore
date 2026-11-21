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
The `-j8` modules are separate modules rather than a classifier, because a composite build (see "Developing with the mods") substitutes whole modules.

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
2.0.0 moved `MemoryCardData` and the trust part of `MemoryCardRules` (`canCopy`, `canPaste`, `captureTrust`, `applyTrust`) from `omt-core` to `omlib-core` (`omtteam.omlib.core.card`).
`MemoryCardRules.capture` and `apply` became `BaseCardRules` in `omt-core` (`omtteam.openmodularturrets.core.card`).
It also brought back `MachineMode.byOrdinal(int, MachineMode)`.
2.1.0 (additive) added switches for single upgrades and addons (`LogicSettings.upgradeEnabled` and `addonEnabled`, `default` methods that return `true`) and computer control of a base (`TurretBaseLogic.isComputerControlled`, `setComputerControlled`, `acceptsComputers`, the new component `computerControlled` of `TurretBaseLogic.State`, whose old constructor stays, and the package `omtteam.openmodularturrets.core.computer`).

The mods copy the core classes into their own jars, so players still install one jar per mod.

## Prerequisites

- Git.
- A JDK 17 or newer to run Gradle.
  Gradle itself comes with the wrapper (Gradle 9.8.1); use `./gradlew`.
- The Java 17 toolchain (compile, `test`, `pitest`) and the Java 8 toolchain (`testJava8`).
  Any vendor works; Gradle downloads them through the foojay resolver if they are not installed.
- Internet access on the first build: the Gradle distribution (services.gradle.org, through the wrapper), the Gradle Plugin Portal (foojay resolver, JvmDowngrader), Maven Central and the toolchain downloads.

When a mod build includes the core (see "Developing with the mods"), the core is built in that mod's Gradle JVM.
The mods' builds run Gradle on JDK 25; see their READMEs.

## Building

```bash
git clone https://github.com/OpenModularTurretsTeam/OpenModularCore.git   # branch main
cd OpenModularCore
./gradlew check                    # both test runs, isolation, architecture rules, coverage, Java 8 jar and API checks
./gradlew :omt-core:pitest         # mutation testing of one module, on demand
./gradlew pitest                   # mutation testing of all three modules
./gradlew publishToMavenLocal      # the six modules into ~/.m2 as 0.0.0-SNAPSHOT
```

`check` runs these tasks in each of `omlib-core`, `omt-core` and `oml-core`:
- `test`: the unit tests on Java 17 against the normal classes, including the ArchUnit rules (`CoreArchitectureTest`).
- `testJava8`: the same tests on a Java 8 JVM against the downgraded classes.
  The test classes are downgraded too (`downgradeTestJar`, `shadeTestJar`), and they run against the module's `-j8` module.
  The architecture tests are left out of this run.
- `verifyCoreIsolation`: fails on an import of Minecraft, a mod loader (`net.minecraft`, `net.minecraftforge`, `cpw.mods`, `net.fabricmc`, `net.neoforged`, `com.mojang`, `team.reborn`) or of a mod package outside a `core` package.
- `jacocoTestReport`: prints the line coverage per package of the Java 17 run; report in `<module>/build/reports/jacoco/test/html/index.html`.
- `verifyJava8Jar`: every class of the downgraded jar is Java 8 bytecode and lies under the module's `core` package or its stub prefix.
- `verifyJava8Api`: no public or protected signature names a JvmDowngrader stub type.

The Java 8 jar of each module is made by `downgradeJar` and `shadeDowngradedApi`, and the `-j8` module copies it to `<module>-j8/build/libs/`.

`pitest` is not part of `check`.
It runs on Java 17 and writes `<module>/build/reports/pitest/index.html`.
It fails when fewer mutations are detected, less is covered or more mutations survive than the thresholds in `<module>/gradle.properties` (`pitest_mutation_threshold`, `pitest_coverage_threshold`, `pitest_max_surviving`).

`publishToMavenLocal` is what JitPack runs (with `-x test -Pversion=<tag>`).
A local build has the version `0.0.0-SNAPSHOT` unless `-Pversion=` is given.
You do not need it to work on the mods.
No mod resolves the core's modules from `~/.m2`: they include the local checkout instead.
(On the `1.12` branch, OMLib's core also reaches OMT, OMPD and OML inside OMLib's dev jar in `~/.m2`; see "Changing the core".)

## Developing with the mods

### Where to clone the core

Every mod build looks for a local core before it uses JitPack.
Its settings file starts at its own folder and walks up one parent folder at a time to the filesystem root.
The first folder that contains `OpenModularCore/settings.gradle.kts` is included as a composite build (`includeBuild`).
So clone this repository as `OpenModularCore` (the exact name) into a folder above the mod clones; one checkout then serves every mod on every Minecraft version.
A mod clone that sits next to `OpenModularCore/` in the same folder finds it too.

The mods are one repository each, with a branch per Minecraft line: `1.12` for 1.12.2, `1.20.1+` for 1.20.1, 1.21.1 and 1.21.11.
To work on both lines, put each line in its own folder:

```
dev/
├── OpenModularCore/          # this repository, branch main
├── mc1.12/
│   ├── OMLib/                # branch 1.12
│   ├── OpenModularTurrets/
│   ├── OpenModularPassiveDefense/
│   └── OpenModularLighting/
└── mc1.20.1/
    ├── OMLib/                # branch 1.20.1+
    ├── OpenModularTurrets/   # with its omlib/ submodule
    └── OpenModularLighting/  # with its omlib/ submodule
```

```bash
cd dev
git clone https://github.com/OpenModularTurretsTeam/OpenModularCore.git
for repo in OMLib OpenModularTurrets OpenModularPassiveDefense OpenModularLighting; do
    git clone -b 1.12 https://github.com/OpenModularTurretsTeam/$repo.git mc1.12/$repo
done
git clone -b 1.20.1+ https://github.com/OpenModularTurretsTeam/OMLib.git mc1.20.1/OMLib
for repo in OpenModularTurrets OpenModularLighting; do
    git clone --recurse-submodules -b 1.20.1+ https://github.com/OpenModularTurretsTeam/$repo.git mc1.20.1/$repo   # with omlib/
done
```

On the `1.20.1+` branch, OMT and OML carry OMLib as the submodule `omlib/`, which they build before anything else (`omlib/tools/bootstrap.sh`).
That script looks for `OpenModularCore/` the same way, starting at the `omlib/` folder.
A folder that builds several mods together (one settings file for all of them) walks up the same way from its own folder.

### Local source instead of JitPack

The core's `group` is the JitPack group, and its project names are the module names.
Gradle therefore substitutes `com.github.OpenModularTurretsTeam.OpenModularCore:<module>:<core_version>` with the project of the same name in the local checkout.
`core_version` has no effect then: the mods build against whatever is checked out in `OpenModularCore/`, with nothing published in between.

To see which one a build uses, look for this line in the Gradle output (it is not shown with `-q`):

```
OpenModularCore: local source at <path> (OMM_NO_LOCAL_CORE=1 for the JitPack artifact)
```

Without that line, the core comes from JitPack.
On the `1.20.1+` branch, the `omlib/` build prints `OMLib: building the library for all nodes into omlib/build/maven (core: <path>)`, or `(core: jitpack)`, whenever it rebuilds.

To build against the JitPack artifact of `core_version` instead, as the CI of the `1.20.1+` branch does (`.github/workflows/check.yml`, without `OpenModularCore/`; the `1.12` branch has no CI):

```bash
OMM_NO_LOCAL_CORE=1 ./gradlew build      # any value other than empty or 0 turns the local core off
./gradlew build -Pomm.noLocalCore         # the same for this build (any value other than false)
```

Prefer the environment variable: every settings file and the `omlib/` bootstrap read it, also in builds that include other builds.

Don't run two builds at the same time that include the same core checkout (for example 1.12 and 1.20.1): they share its `build/` folders.
Set `OMM_NO_LOCAL_CORE=1` for one of them.

### Changing the core

1. Edit the core here and run `./gradlew check` (both test runs).
2. Build every mod on every Minecraft line against it.
   A core change is done only when all of them build.

On the `1.20.1+` branch, run in each of `OMLib/`, `OpenModularTurrets/` and `OpenModularLighting/`:

```bash
./gradlew chiseledBuild                  # every node: 1.20.1 Fabric and Forge, 1.21.1 and 1.21.11 Fabric and NeoForge
```

`chiseledBuild` runs `build` on every node, so it also runs each node's unit tests and, in OMT and OML, its game tests, against the local core.
The dev runs use the local core too:

```bash
./gradlew :1.21.1-fabric:runClient       # a dev client with the local core (any node; :runServer for a dedicated server)
```

- Edits to `omt-core` and `oml-core` reach OMT and OML directly.
- Edits to `omlib-core` reach OMT and OML through their `omlib/` submodule: the bootstrap rebuilds it when a file under `omlib-core` or `build-logic` changes.
  With `OMLIB_SKIP_BUILD` set (any value), it is not rebuilt and the change does not arrive.
- Switching `OMM_NO_LOCAL_CORE` rebuilds `omlib/` too.

In standalone clones on the `1.12` branch, OMPD and OML get OMLib's core only through OMLib's dev jar in `~/.m2`, which carries its own copy of `omlib-core-j8`.
OMT also gets `omlib-core-j8` from the local core through `omt-core-j8`, but the copy in OMLib's dev jar is on its classpath too.
So publish OMLib first, after every core change:

```bash
cd OMLib && ./gradlew publishToMavenLocal                     # runs build first; the dev jar carries omlib-core-j8
cd ../OpenModularTurrets && ./gradlew build -Pomlib_local     # OMLib from ~/.m2 instead of CurseForge
cd ../OpenModularPassiveDefense && ./gradlew build -Pomlib_local -Pomlib_version=4.0.1   # its omlib_version is still 4.0.0
cd ../OpenModularLighting && ./gradlew build -Pomlib_version=4.0.1                     # always takes OMLib from ~/.m2; its omlib_version is still 4.0.0
```

The mods ask `~/.m2` for the OMLib version in their `omlib_version`.
If that differs from the version OMLib publishes (its `mod_version`, with `-alpha.<release_number>` or `-beta.<release_number>` added when `release_type` is alpha or beta), add `-Pomlib_version=<that version>`.
A folder that builds the 1.12 mods together with OMLib (one settings file for all of them) needs no `~/.m2`: OMLib is a project of that build, and the mods use it directly.

On the `1.12` branch, `runClient` and `runServer` of a standalone OMT or OMPD do not load OMLib, because it is only on their compile classpath: copy OMLib's `-forge.jar` into the mod's `run/mods/`, as that mod's `CONTRIBUTING.md` describes.
OML has OMLib's dev jar on its run classpath, so its runs load it.

Finally, repeat the builds with `OMM_NO_LOCAL_CORE=1` once the core is released, to check what a build without the local core (and CI on `1.20.1+`) sees.

### Releasing

Maintainers create and push the tags; a tag is a plain version (`2.0.0`, not `v2.0.0`).
1. Run `./gradlew check` on the commit to tag; JitPack does not run the tests.
2. Tag and push.
   JitPack builds a tag the first time someone requests it, which can take a few minutes.
3. Raise `core_version` in `gradle.properties` of every mod on every branch (OMLib, OMT and OML on `1.20.1+`; OMLib, OMT, OMPD and OML on `1.12`), so all of them name the same version.
4. Build the mods with `OMM_NO_LOCAL_CORE=1`, so they take the new tag from JitPack.

Before tagging, a commit hash works as `core_version` for a trial CI build of a mod (the commit must be pushed).
Never release a mod with a hash.

## License

The code comes from OMLib, Open Modular Turrets and Open Modular Lighting, whose code is MIT-licensed (see their `LICENSE`, `LICENSE-DUAL` and `CLA.md`).
This repository is MIT-licensed as well, with OpenModularTurretsTeam as the copyright holder (see `LICENSE`).
