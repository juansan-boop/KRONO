pluginManagement {
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

rootProject.name = "KRONO"

include(":app")

include(":core:core-common")
include(":core:core-domain")
include(":core:core-data")
include(":core:core-ui")

include(":feature-auth")
include(":feature-onboarding")
include(":feature-dashboard")
include(":feature-tasks")
include(":feature-calendar")
include(":feature-profile")
