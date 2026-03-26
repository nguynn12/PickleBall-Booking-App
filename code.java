buildFeatures {
    viewBinding = true
}//để sử dụng viewBinding trong project

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