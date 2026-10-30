plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ai.jarvish.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.jarvish.mobile"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
