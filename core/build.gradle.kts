plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.chemecador.tennistracker.core"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":shared"))

    api(libs.lifecycle.viewmodel)

    api(platform(libs.firebase.bom))
    api(libs.firebase.auth)
    api(libs.kotlinx.coroutines.play.services)

    api(platform(libs.koin.bom))
    api(libs.koin.core)
    api(libs.koin.android)
}
