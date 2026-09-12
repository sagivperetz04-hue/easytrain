pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "easytrain"

include(":app")

include(":core:common")
include(":core:model")
include(":core:database")
include(":core:network")
include(":core:data")
include(":core:sync")
include(":core:notifications")
include(":core:designsystem")
include(":core:ui")
include(":core:testing")

include(":feature:onboarding")
include(":feature:coach:hub")
include(":feature:coach:trainee")
include(":feature:plans")
include(":feature:workout")
include(":feature:sessions")
include(":feature:chat")
include(":feature:profile")
