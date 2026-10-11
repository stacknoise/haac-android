plugins {
    id("haac.android.library")
    id("haac.android.compose")
    id("haac.hilt")
}

dependencies {
    implementation(project(":core:error"))
    implementation(libs.kotlinx.coroutines.core)
}
