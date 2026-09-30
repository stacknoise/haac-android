plugins {
    id("haac.android.application")
    id("haac.android.compose")
    id("haac.hilt")
    alias(libs.plugins.aboutlibraries)
}

android {
    defaultConfig {
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:error"))
    implementation(project(":core:security"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:instance"))
    implementation(project(":feature:layout"))
    implementation(project(":feature:entities"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:settings"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.aboutlibraries.compose.m3)
    testImplementation(libs.kotlinx.coroutines.test)
}
