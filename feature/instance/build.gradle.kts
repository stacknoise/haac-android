plugins {
    id("haac.android.feature")
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:security"))
    testImplementation(libs.kotlinx.coroutines.test)
}
