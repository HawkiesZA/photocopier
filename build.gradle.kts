import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "1.9.22"
    id("org.jetbrains.compose") version "1.7.0"
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

kotlin {
    jvm()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("com.drewnoakes:metadata-extractor:2.19.0")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "photocopier.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Photocopier"
            packageVersion = "1.0.0"
            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
                bundleID = "za.hawkiesza.photocopier"
                copyright = "Copyright © 2025 Gerrit Vermeulen"
                dockName = "Photocopier"
            }
        }
    }
}
