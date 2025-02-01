import java.io.FileInputStream
import java.util.Properties

include(":fulmar:system:application")


include(":fulmar:system:api")


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
include(":fulmar:tango:session")
include(":fulmar:tango:layer1")
include(":fulmar:tango:trama")
include(":fulmar:tango:firmware")
