plugins {
    java
    application
    jacoco
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.app"
version = "0.1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

javafx {
    version = "21.0.4"
    modules("javafx.controls", "javafx.fxml", "javafx.graphics", "javafx.base")
}

val atlantaFxVersion = "2.0.1"
val jacksonVersion = "2.17.2"
val jwtVersion = "4.4.0"
val poiVersion = "5.2.5"

dependencies {
    implementation("io.github.mkpaz:atlantafx-base:$atlantaFxVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.auth0:java-jwt:$jwtVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")
    implementation("org.slf4j:slf4j-simple:2.0.13")

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

application {
    mainClass.set("com.app.MainApp")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestReport)
}

// jpackage manual (requiere ./gradlew build primero).
// macOS: ./gradlew jpackageDmg | Windows: ./gradlew jpackageExe
fun jpackageTask(name: String, type: String): TaskProvider<Exec> =
    tasks.register<Exec>(name) {
        group = "distribution"
        description = "Genera instalador nativo $type con jpackage (requiere build previo)"
        dependsOn(tasks.build)

        val appVersion = project.version.toString().split("-")[0]
            .split(".").mapIndexed { i, p -> if (i == 0) p.toIntOrNull()?.coerceAtLeast(1)?.toString() ?: "1" else p }
            .joinToString(".").ifBlank { "1.0.0" }
        val jarTask = tasks.named<Jar>("jar")
        doFirst {
            logger.lifecycle("jpackage $type — app $appVersion (mock MVP, sin backend real)")
        }
        commandLine(
            "jpackage",
            "--name", "JavappDesktop",
            "--app-version", appVersion,
            "--input", jarTask.get().destinationDirectory.get().asFile.absolutePath,
            "--main-jar", jarTask.get().archiveFileName.get(),
            "--main-class", "com.app.MainApp",
            "--type", type,
            "--dest", layout.buildDirectory.dir("jpackage").get().asFile.absolutePath,
            "--java-options", "-Xmx512m"
        )
    }

jpackageTask("jpackageDmg", "dmg")
jpackageTask("jpackageExe", "exe-msi")
