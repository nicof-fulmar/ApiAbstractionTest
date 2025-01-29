plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.apiabstractiontest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.apiabstractiontest"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        //release {
        //    isMinifyEnabled = true
        //    isShrinkResources = true
        //    proguardFiles(
        //        getDefaultProguardFile("proguard-android-optimize.txt"),
        //        "proguard-rules.pro"
        //    )
        //    signingConfig = signingConfigs.getByName("debug")
        //}
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.supermegazinc.escentials)

    implementation(libs.supermegazinc.bleupgrade)
    implementation(libs.supermegazinc.cryptography)

    implementation(project(":fulmar:tango:session"))
    implementation(project(":fulmar:tango:layer1"))
    implementation(project(":fulmar:tango:trama"))
    implementation(project(":fulmar:tango:firmware"))


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(project(":fulmar:system:api"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    

    //Permite acceder a los datos de las clases
    implementation(libs.kotlin.reflect)
    implementation(libs.converter.gson)

    implementation(libs.supermegazinc.escentials)
    implementation(libs.supermegazinc.logger)
    implementation(libs.supermegazinc.ble)
}