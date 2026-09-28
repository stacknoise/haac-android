plugins {
    id("haac.android.application")
    id("haac.android.compose")
    id("haac.hilt")
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
}
