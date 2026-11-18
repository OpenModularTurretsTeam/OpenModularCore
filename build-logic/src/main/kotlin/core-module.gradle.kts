// Build logic of a Java 17 core module (omlib-core, omt-core, oml-core). The module's gradle.properties names its
// base package (base_package) and its PIT thresholds; everything else is the same for all three.
//
// Besides the normal Java 17 jar, the module builds its Java 8 jar for 1.12.2 (JvmDowngrader: downgradeJar, then
// shadeDowngradedApi, which bundles the stubs of the Java 9+ API the core uses under <base package>/jvmdg/), which
// its -j8 module publishes (core-java8.gradle.kts). `check` runs the unit tests twice: `test` on Java 17 against the
// normal classes, `testJava8` on a Java 8 JVM against the downgraded, shaded classes and the -j8 modules.
import xyz.wagyourtail.jvmdg.gradle.JVMDowngraderExtension
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import xyz.wagyourtail.jvmdg.gradle.task.ShadeJar
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import java.util.zip.ZipFile

plugins {
    `java-library`
    jacoco
    `maven-publish`
    id("xyz.wagyourtail.jvmdowngrader")
}

/** The mod's base package (omtteam.omlib); the core is <base>.core. */
val basePackage = property("base_package") as String
val corePackage = "$basePackage.core"
val basePath = basePackage.replace('.', '/')
val javaVersion = (property("java_version") as String).toInt()
val java8Version = (property("java8_version") as String).toInt()
/** Where the Java 8 jar keeps the API stubs; one prefix per mod, so the three 1.12 mod jars never carry the same class. */
val stubPrefix = "$basePath/jvmdg/"
/** The test classes get their own stubs: they stand for a caller from outside the core (as the 1.12 game layer is). */
val testStubPrefix = "$basePath/jvmdgtest/"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVersion
}

// ---- Test libraries: the same for the Java 17 and the Java 8 run ----
// A module adds its own with "testLibraries"(...) (oml-core: EqualsVerifier).
val testLibraries = configurations.dependencyScope("testLibraries")
configurations.testImplementation { extendsFrom(testLibraries.get()) }
dependencies {
    "testLibraries"(platform("org.junit:junit-bom:${property("junit_version")}"))
    "testLibraries"("org.junit.jupiter:junit-jupiter")
    "testLibraries"("org.junit.platform:junit-platform-launcher")
    // Architecture rules (src/test/.../architecture/CoreArchitectureTest)
    "testLibraries"("com.tngtech.archunit:archunit-junit5:${property("archunit_version")}")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

// ---- Core isolation: no Minecraft, loader or game-layer imports ----
// Ported from the mods' mod-common.gradle.kts. The core repository has no Minecraft on its classpath, so such an import
// would not compile anyway; this names the line. Any omtteam package outside a core counts as game layer.
val verifyCoreIsolation = tasks.register("verifyCoreIsolation") {
    group = "verification"
    description = "Fails if the core imports Minecraft, a loader or game-layer classes"
    val sourceDirs = listOf("main", "test").map { layout.projectDirectory.dir("src/$it/java") }
    val root = rootProject.projectDir
    sourceDirs.forEach { inputs.dir(it) }
    doLast {
        val forbidden = Regex("""^\s*import\s+(static\s+)?(net\.minecraft|net\.minecraftforge|cpw\.mods|net\.fabricmc|net\.neoforged|com\.mojang|team\.reborn|omtteam\.\w+\.(?!core\b))""")
        val violations = sourceDirs.map { it.asFile }.flatMap { dir ->
            dir.walkTopDown().filter { it.extension == "java" }.flatMap { file ->
                file.readLines().withIndex().filter { forbidden.containsMatchIn(it.value) }
                    .map { "${file.relativeTo(root)}:${it.index + 1}: ${it.value.trim()}" }
            }.toList()
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Core logic must not depend on the game:\n" + violations.joinToString("\n"))
        }
    }
}

// ---- Test coverage (JaCoCo): check prints the line coverage per package, from the Java 17 run ----
// Report: build/reports/jacoco/test/html/index.html
jacoco {
    toolVersion = property("jacoco_version") as String
}
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        csv.required = true
        xml.required = true
        html.required = true
    }
    val csv = reports.csv.outputLocation
    val html = reports.html.outputLocation
    val prefix = "$basePackage."
    doLast {
        val rows = csv.get().asFile.readLines().drop(1).map { it.split(',') }
        val byPackage = sortedMapOf<String, IntArray>()
        for (row in rows) {
            val lines = byPackage.getOrPut(row[1].removePrefix(prefix)) { IntArray(2) }
            lines[0] += row[7].toInt() // LINE_MISSED
            lines[1] += row[8].toInt() // LINE_COVERED
        }
        fun percent(l: IntArray) = "%5.1f%%  (%d/%d lines)".format(100.0 * l[1] / maxOf(1, l[0] + l[1]), l[1], l[0] + l[1])
        val total = IntArray(2).also { t -> byPackage.values.forEach { t[0] += it[0]; t[1] += it[1] } }
        println("Test coverage of $corePackage (unit tests, Java $javaVersion):")
        println("  %-24s %s".format("core", percent(total)))
        byPackage.forEach { (name, lines) -> println("    %-22s %s".format(name, percent(lines))) }
        println("  Report: ${html.get().asFile.resolve("index.html").toURI()}")
    }
}

// ---- Mutation testing (PIT): ./gradlew pitest, on demand, not part of check ----
// Plants small mistakes (flipped conditions, changed boundaries, removed calls) and checks whether a unit test
// fails. Surviving mistakes show behaviour the tests do not pin down. Report: build/reports/pitest/index.html
val pitest = configurations.create("pitest") {
    isCanBeConsumed = false
}
dependencies {
    pitest("org.pitest:pitest-command-line:${property("pitest_version")}")
    pitest("org.pitest:pitest-junit5-plugin:${property("pitest_junit5_version")}")
}
tasks.register<JavaExec>("pitest") {
    group = "verification"
    description = "Mutation testing of the core logic (report in build/reports/pitest)"
    dependsOn(tasks.testClasses)
    classpath = pitest + sourceSets.test.get().runtimeClasspath
    mainClass = "org.pitest.mutationtest.commandline.MutationCoverageReport"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    val reportDir = layout.buildDirectory.dir("reports/pitest")
    val sources = sourceSets.main.get().java.srcDirs
    outputs.dir(reportDir)
    // fail on a degradation: % of mutations detected, % line coverage of the mutated classes, mutations not detected
    // (survived or not reached by any test); the values are in the module's gradle.properties
    val thresholds = listOf(
        "--mutationThreshold", project.property("pitest_mutation_threshold").toString(),
        "--coverageThreshold", project.property("pitest_coverage_threshold").toString(),
        "--maxSurviving", project.property("pitest_max_surviving").toString(),
    )
    inputs.property("thresholds", thresholds)
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            "--reportDir", reportDir.get().asFile.path,
            "--targetClasses", "$corePackage.*",
            "--targetTests", "$corePackage.*",
            // the tests and their helpers (fakes, TestWorld) share the core packages
            "--excludedClasses", "*Test,*Test\$*,*.Fake*,*.TestWorld*",
            "--sourceDirs", sources.joinToString(","),
            "--threads", Runtime.getRuntime().availableProcessors().toString(),
            "--outputFormats", "HTML,XML",
            "--timestampedReports=false",
        ) + thresholds
    })
    doLast {
        println("PIT report: ${reportDir.get().asFile.resolve("index.html").toURI()}")
    }
}

// ---- Java 8 jar (JvmDowngrader): downgradeJar, then shadeDowngradedApi ----
extensions.configure<JVMDowngraderExtension> {
    downgradeTo = JavaVersion.toVersion(java8Version)
}
tasks.named<DowngradeJar>("downgradeJar") {
    downgradeTo = JavaVersion.toVersion(java8Version)
    classpath = sourceSets.main.get().compileClasspath
    archiveClassifier = "downgraded"
}
val shadeDowngradedApi = tasks.named<ShadeJar>("shadeDowngradedApi") {
    downgradeTo = JavaVersion.toVersion(java8Version)
    shadePath = stubPrefix
    archiveClassifier = "downgraded-shaded"
}

// The Java 8 jar for the -j8 module (core-java8.gradle.kts), which publishes it. The usage is private to this build.
configurations.consumable("java8JarElements") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("omtteam-core-java8-jar"))
    }
    outgoing.artifact(shadeDowngradedApi)
}

// Every class of the Java 8 jar is Java 8 bytecode and lies in the core or under this mod's stub prefix. The prefixes
// differ per mod, so no two Java 8 jars can carry the same class.
val verifyJava8Jar = tasks.register("verifyJava8Jar") {
    group = "verification"
    description = "Fails if the Java 8 jar has a class newer than Java 8 or outside the core and its stub prefix"
    val jar = shadeDowngradedApi.flatMap { it.archiveFile }
    inputs.file(jar)
    val corePath = "$basePath/core/"
    val maxMajor = 44 + java8Version
    doLast {
        val problems = mutableListOf<String>()
        var classes = 0
        var stubs = 0
        ZipFile(jar.get().asFile).use { zip ->
            for (entry in zip.entries()) {
                val name = entry.name
                if (!name.endsWith(".class")) continue
                classes++
                when {
                    name.startsWith(corePath) -> {}
                    name.startsWith(stubPrefix) -> stubs++
                    else -> problems += "$name: outside $corePath and $stubPrefix"
                }
                val header = zip.getInputStream(entry).use { it.readNBytes(8) }
                val major = ((header[6].toInt() and 0xff) shl 8) or (header[7].toInt() and 0xff)
                if (major > maxMajor) problems += "$name: class file version $major (Java 8 is $maxMajor)"
            }
        }
        if (problems.isNotEmpty()) {
            throw GradleException("Java 8 jar ${jar.get().asFile.name}:\n" + problems.joinToString("\n"))
        }
        println("Java 8 jar ${jar.get().asFile.name}: $classes classes, all Java 8 bytecode, $stubs of them API stubs under $stubPrefix")
    }
}

// The public API of the Java 8 jar names only Java 8 types and the core's own types. A Java 9+ type in a public
// signature becomes a JvmDowngrader stub type there (omtteam/<mod>/jvmdg/..., or xyz/wagyourtail/... before shading),
// and 1.12 code cannot pass its own objects to it (java.util.random.RandomGenerator was such a case). Checked: every
// class the game layer can reach (public, nested ones only inside a reachable class) with its superclass, interfaces
// and generic signature, and its public and protected fields, methods (with their throws clauses) and record
// components. The one stub allowed is J_L_Record as the superclass of a downgraded record: no Java 8 code names
// java.lang.Record. Annotations are not checked (the downgraded classes keep a few of JvmDowngrader's, see
// ../CLAUDE.md). ASM comes with JvmDowngrader.
val verifyJava8Api = tasks.register("verifyJava8Api") {
    group = "verification"
    description = "Fails if a public signature of the Java 8 jar names a JvmDowngrader stub type"
    val jar = shadeDowngradedApi.flatMap { it.archiveFile }
    inputs.file(jar)
    val corePath = "$basePath/core/"
    doLast {
        val stubType = Regex("""(omtteam/\w+/jvmdg\w*/|xyz/wagyourtail/)[^;<>:]*""")
        val recordBase = Regex("""omtteam/\w+/jvmdg/xyz/wagyourtail/jvmdg/j16/stub/java_base/J_L_Record""")
        val classes = sortedMapOf<String, ClassNode>()
        ZipFile(jar.get().asFile).use { zip ->
            for (entry in zip.entries()) {
                if (!entry.name.endsWith(".class") || !entry.name.startsWith(corePath)) continue
                val node = ClassNode()
                zip.getInputStream(entry).use { ClassReader(it.readBytes()).accept(node, ClassReader.SKIP_CODE) }
                classes[node.name] = node
            }
        }
        fun visible(access: Int) = access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED) != 0
        val reachable = mutableMapOf<String, Boolean>()
        fun isReachable(name: String): Boolean = reachable.getOrPut(name) {
            val node = classes[name] ?: return@getOrPut false
            val own = node.innerClasses.firstOrNull { it.name == name }
            when {
                own == null -> node.access and Opcodes.ACC_PUBLIC != 0
                own.outerName == null -> false // local or anonymous class
                else -> visible(own.access) && isReachable(own.outerName)
            }
        }
        val problems = mutableListOf<String>()
        var checked = 0
        fun check(where: String, vararg texts: String?) {
            checked++
            texts.filterNotNull().flatMap { text -> stubType.findAll(text).map { it.value } }.distinct()
                .forEach { problems += "$where: ${it.replace('/', '.')}" }
        }
        fun recordComponents(annotations: List<AnnotationNode>?): List<Pair<String, String>> =
            annotations.orEmpty().filter { it.desc.endsWith("/RecordComponents;") }.flatMap { components ->
                ((components.values?.getOrNull(1) as? List<*>).orEmpty()).filterIsInstance<AnnotationNode>().map { value ->
                    val fields = value.values.orEmpty().chunked(2).associate { it[0] as String to it[1] }
                    (fields["name"] as? String ?: "?") to ((fields["type"] as? Type)?.descriptor ?: "")
                }
            }
        for ((name, node) in classes) {
            if (!isReachable(name)) continue
            val type = name.replace('/', '.')
            val superName = node.superName?.takeUnless { recordBase.matches(it) }
            check(type, superName, node.signature?.replace(recordBase, "java/lang/Record"), *node.interfaces.toTypedArray())
            node.fields.filter { visible(it.access) }.forEach { check("$type.${it.name}", it.desc, it.signature) }
            node.methods.filter { visible(it.access) }
                .forEach { check("$type.${it.name}${it.desc}", it.desc, it.signature, *it.exceptions.toTypedArray()) }
            node.recordComponents.orEmpty().forEach { check("$type.${it.name} (record component)", it.descriptor, it.signature) }
            (recordComponents(node.visibleAnnotations) + recordComponents(node.invisibleAnnotations))
                .forEach { (component, descriptor) -> check("$type.$component (record component)", descriptor) }
        }
        if (problems.isNotEmpty()) {
            throw GradleException(
                "Public signatures of the Java 8 jar ${jar.get().asFile.name} name JvmDowngrader stub types; use Java 8 " +
                    "types or the core's own types there (../CLAUDE.md, \"Java version\"):\n" + problems.joinToString("\n")
            )
        }
        println("Java 8 API of ${jar.get().asFile.name}: ${reachable.count { it.value }} public classes, $checked signatures, no stub types")
    }
}

// ---- Java 8 test run: the test classes downgraded too, on a Java 8 JVM, against the -j8 modules ----
val testJar = tasks.register<Jar>("testJar") {
    archiveClassifier = "tests"
    from(sourceSets.test.map { it.output })
}
val downgradeTestJar = tasks.register<DowngradeJar>("downgradeTestJar") {
    downgradeTo = JavaVersion.toVersion(java8Version)
    inputFile = testJar.flatMap { it.archiveFile }
    classpath = sourceSets.test.get().runtimeClasspath
    archiveClassifier = "tests-downgraded"
}
val shadeTestJar = tasks.register<ShadeJar>("shadeTestJar") {
    downgradeTo = JavaVersion.toVersion(java8Version)
    inputFile = downgradeTestJar.flatMap { it.archiveFile }
    shadePath = testStubPrefix
    archiveClassifier = "tests-downgraded-shaded"
}
val java8TestClasses = tasks.register<Sync>("java8TestClasses") {
    from(shadeTestJar.map { zipTree(it.archiveFile) })
    into(layout.buildDirectory.dir("classes/java8/test"))
}

// The module's own -j8 module (and through it the -j8 modules it depends on), as a 1.12 mod gets them, plus the
// test libraries, all resolved for a Java 8 consumer
val java8TestRuntime = configurations.dependencyScope("java8TestRuntime") {
    extendsFrom(testLibraries.get())
}
dependencies {
    "java8TestRuntime"(project(":${project.name}-j8"))
}
val java8TestRuntimeClasspath = configurations.resolvable("java8TestRuntimeClasspath") {
    extendsFrom(java8TestRuntime.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment.STANDARD_JVM))
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, java8Version)
    }
}

val testJava8 = tasks.register<Test>("testJava8") {
    group = "verification"
    description = "Runs the unit tests on Java 8 against the downgraded, shaded classes"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(java8Version) }
    testClassesDirs = files(java8TestClasses)
    classpath = files(java8TestClasses) + java8TestRuntimeClasspath.get()
    useJUnitPlatform()
    // The architecture rules check the design on the Java 17 classes; the downgraded classes also depend on their
    // stubs, which the rules do not allow. verifyJava8Jar checks the Java 8 jar instead.
    filter {
        excludeTestsMatching("$basePackage.architecture.*")
    }
    extensions.configure<JacocoTaskExtension> { isEnabled = false }
}

tasks.check {
    dependsOn(verifyCoreIsolation, tasks.jacocoTestReport, verifyJava8Jar, verifyJava8Api, testJava8)
}
