import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}


/**
 * Room/KSP args live here rather than inside `defaultConfig`.
 *
 * In the previous layout the `ksp { }` block sat nested in `android.defaultConfig`
 * and only resolved because Kotlin DSL kept the project-level KspExtension in the
 * implicit-receiver chain. It worked by accident and produced a project-wide
 * config rather than the per-variant one that was clearly intended.
 */
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}


/*
 * Release signing.
 *
 * Two sources, in priority order:
 *
 *   1. `keystore.properties` in the repo root — for local builds. Gitignored.
 *   2. Environment variables — for CI, where the workflow decodes the keystore
 *      from a repository secret at the top of the job and writes
 *      `keystore.properties` itself, so both paths read the same file.
 *
 * Neither source is required to be present, and that is deliberate rather than
 * sloppy. A `signingConfigs` block that throws when its material is absent takes
 * down every debug build on a fresh clone, which is a far worse failure than an
 * unsigned debug APK — nobody installs those. `assembleRelease` is the only
 * thing that needs the key, and the workflow verifies the signature before
 * uploading so an unsigned release fails loudly instead of quietly shipping.
 *
 * See docs/SIGNING.md.
 *
 * The `import java.util.Properties` at the top of this file is load-bearing, not
 * decoration. Written inline as `java.util.Properties()` it fails to compile:
 * at project scope `java` resolves to the Java plugin extension, so
 * `java.util` is read as a member access on an unrelated object and Kotlin
 * reports "Unresolved reference: util". Importing the type sidesteps the
 * collision entirely.
 */
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Resolved against the root project, not this module, so a relative
// `storeFile=release.jks` means the repo root whether or not Gradle was
// invoked with `-p app`.
val releaseKeystoreFile = rootProject.file(
    keystoreProps.getProperty("storeFile")
        ?: System.getenv("KEYSTORE_PATH")
        ?: "release.jks"
)

val hasSigningMaterial: Boolean =
    releaseKeystoreFile.exists() &&
        (keystoreProps.getProperty("storePassword") ?: System.getenv("KEYSTORE_PASSWORD")) != null &&
        (keystoreProps.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS")) != null &&
        (keystoreProps.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD")) != null


android {
    namespace = "com.dude555afk.xeno"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dude555afk.xeno"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = releaseKeystoreFile
            storePassword = keystoreProps.getProperty("storePassword")
                ?: System.getenv("KEYSTORE_PASSWORD")
            keyAlias = keystoreProps.getProperty("keyAlias")
                ?: System.getenv("KEY_ALIAS")
            keyPassword = keystoreProps.getProperty("keyPassword")
                ?: System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            // Only wire up the key when it is actually there. Attaching a
            // signingConfig whose keystore does not exist fails with an AGP
            // error that reads like a Gradle bug, so the check happens out here
            // where the consequence is an unsigned APK the verify step catches.
            if (hasSigningMaterial) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.webkit)
    implementation(libs.mozilla.geckoview)

    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.okhttp.logging)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation("junit:junit:4.13.2")
}
