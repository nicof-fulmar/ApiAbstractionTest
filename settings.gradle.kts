pluginManagement {
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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ApiAbstractionTest"
include(":app")
include(":supermegazinc:escentials")
include(":supermegazinc:ble")
include(":supermegazinc:ble_upgrade")
include(":supermegazinc:diffie_hellman")
include(":fulmar:tango:session")
