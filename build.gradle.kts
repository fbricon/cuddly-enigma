import com.adarshr.gradle.testlogger.theme.ThemeType
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.or
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.VerificationReportsFormats.*
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.*


fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)
fun prop(name: String) = properties(name).get()

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.testLogger)
    jacoco
}

group = prop("pluginGroup")
version = prop("pluginVersion")

val quarkusVersion = prop("quarkusVersion")
val lsp4mpVersion = prop("lsp4mpVersion")
val quarkusLsVersion = prop("lsp4mpVersion")
val quteLsVersion = prop("quteLsVersion")
//val ideaVersion = prop("ideaVersion").or(prop("platformType")+"-"+prop("platformVersion"))
// Configure project's dependencies
repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repository.jboss.org/nexus/content/repositories/snapshots") }
    maven { url = uri("https://repository.jboss.org/nexus/content/groups/public") }
    maven { url = uri("https://repo.eclipse.org/content/repositories/lsp4mp-snapshots") }
    maven { url = uri("https://repo.eclipse.org/content/repositories/lsp4mp-releases") }
    maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
}

val lsp: Configuration by configurations.creating

sourceSets {
    named("test") {
        java.srcDir("src/test/java")
        java.srcDir("intellij-community/java/testFramework/src")
        java.srcDir("intellij-community/platform/external-system-api/testFramework/src")
        java.srcDir("intellij-community/platform/external-system-impl/testSrc")
        java.srcDir("intellij-community/platform/lang-impl/testSources")
        java.srcDir("intellij-community/platform/testFramework/extensions/src")
        java.srcDir("intellij-community/plugins/gradle/src")
        java.srcDir("intellij-community/plugins/gradle/testSources")
        java.srcDir("intellij-community/plugins/gradle/tooling-extension-impl/testSources")
        java.srcDir("intellij-community/plugins/maven/src/test/java")
        java.srcDir("intellij-community/plugins/maven/testFramework/src")
        resources.srcDir("src/test/resources")
    }

    create("integrationTest") {
        java.srcDir("src/it/java")
        resources.srcDir("src/it/resources")
        compileClasspath += sourceSets.main.get().output
        compileClasspath += configurations.testImplementation.get()
        runtimeClasspath += sourceSets.main.get().output + compileClasspath
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation("org.zeroturnaround:zt-zip:1.14")
    implementation("com.kotcrab.remark:remark:1.2.0")
    implementation("org.jsoup:jsoup:1.14.2")
    implementation("io.quarkus:quarkus-core:$quarkusVersion") {
        isTransitive = false
    }
    implementation("io.quarkus:quarkus-core-deployment:$quarkusVersion") {
        exclude(group = "org.aesh")
        exclude(group = "org.apache.commons")
        exclude(group = "org.wildfly.common")
        exclude(group = "io.quarkus.gizmo")
        exclude(group = "org.ow2.asm")
        exclude(group = "io.quarkus")
        exclude(group = "org.eclipse.sisu")
        exclude(group = "org.graalvm.sdk")
        exclude(group = "org.junit.platform")
        exclude(group = "org.junit.jupiter")
    }
    implementation("io.quarkus:quarkus-arc:$quarkusVersion") {
        isTransitive = false
    }
    implementation("org.eclipse.lsp4mp:org.eclipse.lsp4mp.ls:$lsp4mpVersion")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.15.0")
    // Required by lsp4j as the version from IJ is incompatible
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.vladsch.flexmark:flexmark:0.62.2")
    lsp("org.eclipse.lsp4mp:org.eclipse.lsp4mp.ls:$lsp4mpVersion:uber") {
        isTransitive = false
    }
    lsp("com.redhat.microprofile:com.redhat.quarkus.ls:$quarkusLsVersion") {
        isTransitive = false
    }
    implementation("com.redhat.microprofile:com.redhat.qute.ls:$quteLsVersion")
    lsp("com.redhat.microprofile:com.redhat.qute.ls:$quteLsVersion:uber") {
        isTransitive = false
    }
    implementation(files(layout.buildDirectory.dir("server")) {
        builtBy("copyDeps")
    })

    testImplementation("com.redhat.devtools.intellij:intellij-common-ui-test-library:0.2.0")
    testImplementation("org.assertj:assertj-core:3.19.0")

}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.JETBRAINS
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    //val ideaBits = ideaVersion.split("-")
    pluginName = properties("pluginName")
    type = prop("platformType")  //ideaBits.get(0)
    version = prop("platformVersion")//ideaBits.get(1)
    updateSinceUntilBuild = false
    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
}

configurations {
    runtimeClasspath {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    testImplementation {
        isCanBeResolved = true
    }
}

testlogger {
    theme = ThemeType.STANDARD
    showExceptions = true
    showStackTraces = true
    showFullStackTraces = false
    showCauses = true
    slowThreshold = 2000
    showSummary = true
    showSimpleNames = false
    showPassed = true
    showSkipped = true
    showFailed = true
    showOnlySlow = false
    showStandardStreams = false
    showPassedStandardStreams = true
    showSkippedStandardStreams = true
    showFailedStandardStreams = true
    logLevel = LogLevel.LIFECYCLE
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
//changelog {
//    groups.empty()
//    repositoryUrl = properties("pluginRepositoryUrl")
//}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<Test> {
    environment("GRADLE_RELEASE_REPOSITORY","https://services.gradle.org/distributions")
    systemProperty("idea.log.leaked.projects.in.tests", "false")
    systemProperty( "idea.maven.test.mirror", "https://repo1.maven.org/maven2")
    systemProperty( "com.redhat.devtools.intellij.telemetry.mode", "disabled")
}


tasks.register<Copy>("copyDeps") {
    val serverDir = layout.buildDirectory.dir("server/server")
    from(lsp)
    into(serverDir)
    rename("^(.*)(-[0-9]+.[0-9]+.[0-9]+(-SNAPSHOT)?)(.*)$", "$1$4")
    doLast {
        val destinationDir = serverDir.get().asFile
        val numFilesCopied = destinationDir.listFiles()?.size ?: 0
        logger.quiet("Copied $numFilesCopied JARs from lsp configuration to ${destinationDir}.")
    }
}

tasks {
    wrapper {
        gradleVersion = prop("gradleVersion")
    }

    runPluginVerifier {
        //ideVersions = listOf(ideaVersion)
        failureLevel = listOf(INVALID_PLUGIN, COMPATIBILITY_PROBLEMS, MISSING_DEPENDENCIES )
        verificationReportsFormats = listOf(MARKDOWN, HTML)
    }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

//        val changelog = project.changelog // local variable for configuration cache compatibility
//        // Get the latest available change notes from the changelog file
//        changeNotes = version.map { pluginVersion ->
//            with(changelog) {
//                renderItem(
//                    (getOrNull(pluginVersion) ?: getUnreleased()).withHeader(false).withEmptySections(false),
//                    Changelog.OutputType.HTML,
//                )
//            }
//        }
    }

    runIde {
        systemProperties["com.redhat.devtools.intellij.telemetry.mode"] = "disabled"
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    prepareSandbox {
        dependsOn("copyDeps")
    }

    jacocoTestReport {
        reports {
            xml.required = true
            html.required = true
        }
    }

    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        //dependsOn("patchChangelog")
        token = environment("PUBLISH_TOKEN")
        channels = properties("channel").map { listOf(it) }
    }

    check {
        dependsOn(jacocoTestReport)
    }
}
