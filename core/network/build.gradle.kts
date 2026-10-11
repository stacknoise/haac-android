plugins {
    id("haac.android.library")
    id("haac.hilt")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:error"))
    implementation(project(":core:security"))
    implementation(project(":core:database"))
    api(libs.okhttp)
    implementation(libs.okhttp.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.okhttp.tls)
    testImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(project(":core:error"))
    testFixturesImplementation(libs.kotlinx.serialization.json)
    testFixturesApi(libs.kotlinx.coroutines.test)
}