import java.io.FileInputStream
import java.util.Properties

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
        maven {
            name = "LocalRepository"
            url = uri("/Repository")
        }
    }
}

rootProject.name = "ApiAbstractionTest"
include(":app")
include(":supermegazinc:ble_upgrade")
include(":fulmar:tango:session")
include(":fulmar:tango:layer1")
include(":fulmar:tango:trama")
include(":fulmar:tango:firmware")
include(":supermegazinc:security:diffie_hellman")
include(":supermegazinc:security:cryptography")
