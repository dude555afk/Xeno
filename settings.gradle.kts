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
        // Keep GeckoView on Mozilla's Maven repository, but resolve ordinary
        // Android/Kotlin dependencies from their canonical repositories. This
        // prevents transient Mozilla repository failures from breaking unrelated
        // libraries such as kotlinx-coroutines-android.
        exclusiveContent {
            forRepository {
                maven { url = uri("https://maven.mozilla.org/maven2/") }
            }
            filter {
                includeGroup("org.mozilla.geckoview")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "Xeno"
include(":app")
