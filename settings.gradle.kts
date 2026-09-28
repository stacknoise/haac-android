pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "haac-android"

include(":app")
include(":core:common")
include(":core:error")
include(":core:security")
include(":core:network")
include(":core:database")
include(":feature:onboarding")
include(":feature:instance")
include(":feature:layout")
include(":feature:entities")
include(":feature:notifications")
include(":feature:settings")
