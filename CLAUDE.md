# CLAUDE.md: OpenModularCore

The shared core of OMLib, OMT and OML: plain Java game rules, no Minecraft or loader imports.
The rules for this repository are in `../CLAUDE.md` (the OMM root), sections "Shared core logic" and "Working across versions"; read them before working here.
`README.md` lists the modules and the commands.

## Layout

- `omlib-core/`, `omt-core/`, `oml-core/`: the Java 17 modules, one per mod, with unchanged package names (`omtteam.<mod>.core`).
  Each has its main and test sources (with the fakes the tests use) and a `gradle.properties` with its `base_package` and PIT thresholds.
- `omlib-core-j8/`, `omt-core-j8/`, `oml-core-j8/`: the Java 8 modules for 1.12.2, without sources of their own.
  They publish the Java 8 jar that the Java 17 module of the same name builds (`downgradeJar`, then `shadeDowngradedApi`).
- `build-logic/`: the convention plugins `core-module` (Java 17 modules) and `core-java8` (Java 8 modules).
- `jitpack.yml`: how JitPack builds a tag.

## Rules

- The core source comes from the modern mods (branch `1.20.1+`) and is moved here, not rewritten: same packages, same content.
- Nothing a Minecraft version or loader decides goes into the core; behaviour that differs between versions comes in through the core's interfaces or settings, never a version switch.
- Public signatures that a game layer calls use only Java 8 types or the core's own types (a Java 9+ type becomes a JvmDowngrader stub type on 1.12); `verifyJava8Api` enforces it.
- Randomness comes in as a `java.util.function.LongSupplier` (decided by the user, 2026-11-18), never `java.util.random.RandomGenerator` or `java.util.Random`.
  The game layer passes its random source's `nextLong` (1.12 `world.rand::nextLong`, modern `level.random::nextLong`); the core derives bounded draws and doubles from it (`BoundedDraw` in `oml-core`).
- Classes stay in the module of the mod they came from, with two exceptions (decided by the user, 2026-11-18):
  - `CamoPermissions` is in `omlib-core` (`omtteam.omlib.core.camo`, next to `CamoLight`), so that OMPD on 1.12 can use it without `omt-core-j8`.
  - `MachineMode` and `TargetingSettings` are in `omlib-core` (`omtteam.omlib.core.machine`), because OMLib's 1.12 game layer uses them and OMLib cannot depend on `omt-core`.
- No Java 9+ call that JvmDowngrader has no stub for; `testJava8` catches them as `MissingStubError`.
- Tests use JUnit 5 and EqualsVerifier 3 (they also run on Java 8); no new dependency without asking the user.
- Changing a public signature is API design: present options to the user first.
- A core change is done only when every workspace builds with it (see "Changing the core" in `../CLAUDE.md`).
- Commit only when the user asks; never push or tag (the user creates and pushes the tags that JitPack builds).
- Markdown: one sentence per line.

## Checks

- `./gradlew check`: per Java 17 module `test` (Java 17), `testJava8` (Java 8, downgraded classes and tests), `verifyCoreIsolation`, `CoreArchitectureTest` (ArchUnit, Java 17 run only), the JaCoCo report, `verifyJava8Jar` and `verifyJava8Api`.
- `verifyJava8Jar`: every class of the Java 8 jar is Java 8 bytecode and lies in the core or under the mod's stub prefix (`omtteam/<mod>/jvmdg/`).
- `verifyJava8Api`: no public signature of the Java 8 jar names a JvmDowngrader stub type (`omtteam/<mod>/jvmdg/…` or `xyz/wagyourtail/…`).
  It reads every class a game layer can reach (public, nested ones only inside a reachable class): superclass, interfaces, generic signature, and the descriptors, generic signatures and throws clauses of its public and protected fields, methods and record components.
  The one stub it allows is `J_L_Record` as the superclass of a downgraded record; annotations are not checked.
- The Java 8 run downgrades the tests too and gives them their own stub prefix (`omtteam/<mod>/jvmdgtest/`), so they call the core like a 1.12 game layer: a public signature with a Java 9+ type fails there.
- `./gradlew :<module>:pitest`: mutation testing, on demand; thresholds in the module's `gradle.properties`.
