plugins {
    kotlin("multiplatform") version "2.4.20"
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "QuintalystShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("com.github.MetrolistGroup.innertubex:innertubex:v0.7.0")
            implementation("io.ktor:ktor-client-content-negotiation:3.6.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.6.0")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.6.0")
        }
    }
}

