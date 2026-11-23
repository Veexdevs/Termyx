plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.compose") version "1.6.11"
}

kotlin {
    jvmToolchain(17)
    jvm {
        application {
            mainClass = "com.termx.desktop.MainKt"
        }
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(project(":core"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.termx.desktop.MainKt"
        nativeDistributions {
            packageName = "TERMYX"
            packageVersion = "0.1.0"
            description = "TERMYX — Beyond the Terminal"
            copyright = "TERMYX contributors (MIT)"
            targetFormats(NativeDistributionType.Exe)
            windows {
                menu = true
                perUserInstall = true
            }
        }
    }
}
