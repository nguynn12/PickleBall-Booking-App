plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.pickleball"
    compileSdk = 36 // Sửa lại một chút cho đúng cú pháp chuẩn nếu release(36) báo lỗi nhé

    defaultConfig {
        applicationId = "com.example.pickleball"
        minSdk = 24
        targetSdk = 36
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

    // THÊM CODE PHẦN 4.1 Ở ĐÂY NÈ: BẬT VIEW BINDING
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // === CÀI THÊM "ĐỒ CHƠI" VÀO ĐÂY NHÉ ===

    // 1. Thư viện Glide để load ảnh sân Pickleball từ link mạng
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // 2. Thư viện MVVM (ViewModel & LiveData) để quản lý dữ liệu
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")

    // Khai báo Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    // Khai báo thư viện Firestore (không cần số phiên bản vì đã có BoM quản lý)
    implementation("com.google.firebase:firebase-firestore")
}