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

dependencies {
    implementation("io.github.mkpaz:atlantafx-base:$atlantaFxVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.auth0:java-jwt:$jwtVersion")
    implementation("org.slf4j:slf4j-simple:2.0.13")
    // Ikonli Core + Feather Icons (o FontAwesome / Material)
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-feather-pack:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-material2-pack:12.3.1")

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

// jpackage manual (requiere JDK 21 en la plataforma de destino).
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
        val stagingDir = layout.buildDirectory.dir("jpackage-input").get().asFile
        val macIcon = file("src/main/resources/images/macos/icon-1024x1024px.icns")
        val windowsIcon = file("src/main/resources/images/windows/icon-256x256px.ico")
        val runtimeModules = listOf(
            "java.se", "jdk.crypto.ec", "jdk.jfr", "javafx.base", "javafx.graphics",
            "javafx.controls", "javafx.fxml"
        )
        val javafxModulePath = configurations.runtimeClasspath.get().files
            .filter { it.name.startsWith("javafx-") && it.extension == "jar" }
            .joinToString(File.pathSeparator) { File(stagingDir, it.name).absolutePath }

        doFirst {
            stagingDir.deleteRecursively()
            stagingDir.mkdirs()
            copy {
                from(jarTask.get().archiveFile)
                from(configurations.runtimeClasspath)
                into(stagingDir)
            }
        }
        doFirst {
            logger.lifecycle("jpackage $type — app $appVersion (mock MVP, sin backend real)")
        }
        val jpackageArgs = mutableListOf(
            "--name", "JavappDesktop",
            "--app-version", appVersion,
            "--vendor", "Javapp",
            "--input", stagingDir.absolutePath,
            "--main-jar", jarTask.get().archiveFileName.get(),
            "--main-class", "com.app.MainApp",
            "--module-path", javafxModulePath,
            "--add-modules", runtimeModules.joinToString(","),
            "--type", type,
            "--dest", layout.buildDirectory.dir("jpackage").get().asFile.absolutePath,
            "--java-options", "-Xmx512m"
        )
        if (type == "dmg") {
            jpackageArgs.addAll(listOf(
                "--icon", macIcon.absolutePath,
                "--mac-package-identifier", "com.app.javappdesktop"
            ))
        } else if (type == "exe" || type == "msi") {
            jpackageArgs.addAll(listOf(
                "--icon", windowsIcon.absolutePath,
                "--win-menu",
                "--win-menu-group", "Javapp",
                "--win-shortcut",
                "--win-per-user-install"
            ))
        }
        commandLine("jpackage", *jpackageArgs.toTypedArray())
    }

jpackageTask("jpackageDmg", "dmg")
jpackageTask("jpackageExe", "exe")
jpackageTask("jpackageMsi", "msi")
