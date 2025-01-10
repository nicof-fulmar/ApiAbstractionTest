import java.io.FileInputStream
import java.util.Properties

include(":fulmar:tango:trama")


include(":fulmar:tango:layer1")


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

val githubProperties = Properties()
githubProperties.load(FileInputStream("./github.properties"))

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/supermegazinc/Android-Libraries")
            credentials {
                username = githubProperties["gpr.usr"] as String?
                password = githubProperties["gpr.key"] as String?
            }
        }
        maven {
            name = "MyRepo"
            url = uri("Z:/")
        }
    }
}

rootProject.name = "ApiAbstractionTest"
include(":app")
include(":supermegazinc:ble_upgrade")
include(":fulmar:tango:session")
include(":supermegazinc:security:diffie_hellman")
include(":supermegazinc:security:cryptography")
