plugins {
    id("haac.android.library")
    id("haac.hilt")
}

dependencies {
    implementation(project(":core:error"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.biometric)
    // FragmentActivity is part of the BiometricPrompter API.
    api(libs.androidx.fragment)
    testImplementation(libs.kotlinx.coroutines.test)
}
