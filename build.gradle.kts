plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
    id("org.jetbrains.intellij") version "1.13.3"
    jacoco
}

group = "com.github.nizienko"
version = "0.9.4"

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")

}

sourceSets["main"].java.srcDirs("src/main/gen")


// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.1")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("241.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
val robotVersion = "0.11.22"
dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.intellij.remoterobot:remote-robot:$robotVersion")
    testImplementation("com.intellij.remoterobot:remote-fixtures:$robotVersion")
    testImplementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Video Recording
    testImplementation("com.automation-remarks:video-recorder-junit5:2.0")
}

tasks.downloadRobotServerPlugin {
    version.set(robotVersion)
}

jacoco {
    toolVersion = "0.8.10"
    applyTo(tasks.runIdeForUiTests.get())
}

val uiTestsCoverageReport = tasks.register<JacocoReport>("uiTestsCoverageReport") {
    executionData(tasks.runIdeForUiTests.get())
    sourceSets(sourceSets.main.get())

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.runIdeForUiTests {
    systemProperty("robot-server.port", "8082")
    systemProperty("ide.mac.message.dialogs.as.sheets", "false")
    systemProperty("jb.privacy.policy.text", "<!--999.999-->")
    systemProperty("jb.consents.confirmation.enabled", "false")
    systemProperty("ide.mac.file.chooser.native", "false")
    systemProperty("jbScreenMenuBar.enabled", "false")
    systemProperty("apple.laf.useScreenMenuBar", "false")
    systemProperty("idea.trust.all.projects", "true")
    systemProperty("ide.show.tips.on.startup.default.value", "false")

    configure<JacocoTaskExtension> {

        // 221+ uses a custom classloader and jacoco fails to find classes
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
    finalizedBy(uiTestsCoverageReport)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }

    // we need the coverage from Idea process, not from test task
    configure<JacocoTaskExtension> {
        isEnabled = false
    }
}