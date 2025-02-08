plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {

    namespace = "com.fulmar.${project.name}"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
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

    implementation(libs.supermegazinc.logger)
    implementation(libs.supermegazinc.escentials)

    implementation(libs.androidx.core.ktx)

    implementation(libs.kotlin.reflect)
    implementation(libs.converter.gson)

    implementation(libs.supermegazinc.bleupgrade)
    implementation(libs.supermegazinc.cryptography)
    implementation(project(":fulmar:tango:session"))
    implementation(project(":fulmar:tango:trama"))
    implementation(project(":fulmar:tango:firmware"))

}