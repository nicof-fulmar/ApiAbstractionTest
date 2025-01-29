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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.supermegazinc.logger)
    implementation(libs.supermegazinc.escentials)
    implementation(libs.converter.gson)
    implementation(libs.core.ktx)
    implementation(project(":fulmar:system:api"))
    implementation(libs.androidx.junit.ktx)
    testImplementation(libs.junit.jupiter)
    androidTestImplementation(libs.junit.junit)
    androidTestImplementation(libs.androidx.runner)
}