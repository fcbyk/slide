plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.fcbyk.slide"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fcbyk.slide"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}
