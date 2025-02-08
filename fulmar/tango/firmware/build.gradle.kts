plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {

    namespace = "com.fulmar.${project.name}"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }

kotlinOptions {
        jvmTarget = "19"
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.supermegazinc.logger)
    implementation(libs.supermegazinc.escentials)
    implementation(libs.converter.gson)
    implementation(project(":fulmar:system:api"))
    implementation(project(":supermegazinc:json"))
    implementation(libs.androidx.junit.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
}