plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.medicarenow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.medicarenow"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Basic Android dependencies
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation(libs.google.firebase.firestore)

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    
    // Firebase BOM - ensures all Firebase libraries use compatible versions
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    
    // Firebase dependencies (versions controlled by BOM)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    
    // BCrypt for password verification
    implementation("at.favre.lib:bcrypt:0.10.2")
    
    // Other utilities
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("androidx.cardview:cardview:1.0.0")
}
