import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

compose.desktop {
    application {
        mainClass = "com.termx.desktop.MainKt"
        nativeDistributions {
            packageName = "TERMYX"
            packageVersion = "0.1.0"
            description = "TERMYX — Beyond the Terminal"
            copyright = "TERMYX contributors (MIT)"
            targetFormats(TargetFormat.Exe)
            windows {
                menu = true
                perUserInstall = true
            }
        }
    }
}
