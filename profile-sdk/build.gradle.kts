import java.util.Properties
import kotlin.apply

group = "com.itm.profilesdk"
version = "1.0.1"

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    id("maven-publish")
}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "com.itm.profile_sdk"
        compileSdk = 36
        minSdk = 24

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        lint {
            // RoomDatabase.createOpenDelegate is @RestrictTo(LIBRARY_GROUP_PREFIX) — it is called
            // by Room's KSP-generated AppDatabase_Impl, which is outside the androidx group.
            // This is a known Room KMP issue; the generated code is correct and safe.
            disable += "RestrictedApi"
        }
    }

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
    var xcfName = "Profile_SDK"
    val syntheticPodsTbdDir = layout.buildDirectory
        .dir("cocoapods/synthetic/ios/build/EagerLinkingTBDs")
        .get()
        .asFile
        .absolutePath

    iosX64 {
        binaries.framework {
            baseName = xcfName
            linkerOpts("-F$syntheticPodsTbdDir/Debug-iphonesimulator")
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = xcfName
            linkerOpts("-F$syntheticPodsTbdDir/Debug-iphoneos")
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
            linkerOpts("-F$syntheticPodsTbdDir/Debug-iphonesimulator")
        }
    }

    cocoapods {
        version = "1.0"
        summary = "Profile-SDK Kotlin Multiplatform SDK"
        homepage = "https://theislam360.com"
        ios.deploymentTarget = "15.0"
        name = "Profile_SDK"
        framework {
            baseName = "Profile_SDK"
            isStatic = true
        }
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // Ktor
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)

                // Room
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)

                // Coroutines & Serialization
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)

                // Add KMP dependencies here
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                           }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.junit)
            }
        }

        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP’s default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
                implementation(libs.ktor.client.darwin)
            }
        }
    }



}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}


val props = Properties().apply {
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) {
        load(localProps.inputStream())
    }
}

val githubActor = props.getProperty("github.actor") ?: System.getenv("GITHUB_ACTOR")
val githubToken = props.getProperty("github.token") ?: System.getenv("GITHUB_TOKEN")

publishing {
    repositories {
        mavenLocal()
        if (!githubActor.isNullOrBlank() && !githubToken.isNullOrBlank()) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/islamic-technology-mission/itm-profile-sdk")
                credentials {
                    username = githubActor
                    password = githubToken
                }
            }
        }
    }
}
