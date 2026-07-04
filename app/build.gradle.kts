plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.matheusghenriques.thedoor"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.matheusghenriques.thedoor"

        minSdk = 26
        targetSdk = 36

        versionCode = providers
            .environmentVariable("VERSION_CODE")
            .orElse("1")
            .get()
            .toInt()

        versionName = providers
            .environmentVariable("VERSION_NAME")
            .orElse("1.0.0")
            .get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystorePath = providers.environmentVariable("KEYSTORE_PATH").orNull
    val keystorePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
    val keyAlias = providers.environmentVariable("KEY_ALIAS").orNull
    val keyPassword = providers.environmentVariable("KEY_PASSWORD").orNull

    if (
        keystorePath != null &&
        keystorePassword != null &&
        keyAlias != null &&
        keyPassword != null
    ) {
        signingConfigs {
            create("release") {
                storeFile = File(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {

        release {

            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }

            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.argon2kt)

    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
