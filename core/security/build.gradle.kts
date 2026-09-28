plugins {
    id("haac.android.library")
    id("haac.hilt")
}

dependencies {
    implementation(project(":core:error"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}